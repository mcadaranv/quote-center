<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RejectQuoteForm from './RejectQuoteForm.vue'

const props = defineProps({
  quoteId: { type: Number, default: null },
  isEmbedded: { type: Boolean, default: false },
})

const emit = defineEmits(['quote-updated'])

const route = useRoute()
const router = useRouter()
const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')

const quote = ref(null)
const isLoading = ref(false)
const isUpdatingStatus = ref(false)
const isSubmittingRejection = ref(false)
const isRejectModalOpen = ref(false)
const rejectionReasons = ref([])
const quoteRejections = ref([])
const errorMessage = ref('')
const statusMessage = ref('')

const resolvedQuoteId = computed(() => props.quoteId || Number(route.params.quoteId))
const selectedYear = computed(() => route.query.year || '')
const isBusyRejecting = computed(() => isSubmittingRejection.value || isUpdatingStatus.value)

const displayQuoteId = computed(() => {
  if (!quote.value?.quoteId) return 'N/A'
  return `RW-${String(quote.value.quoteId).padStart(5, '0')}`
})

const formattedDate = computed(() => {
  if (!quote.value?.submittedDate) return 'N/A'
  return quote.value.submittedDate
})

const batterySummary = computed(() => {
  if (!quote.value?.battery) return 'N/A'

  const qty = quote.value.batteryQty ?? 0
  const make = quote.value.battery.make || ''
  const model = quote.value.battery.model || ''
  const year = selectedYear.value || quote.value.battery.modelYearStart || 'N/A'

  return `${qty}x ${make} ${model} (${year})`.trim()
})

const detailRows = computed(() => quote.value?.details || [])

const totalValue = computed(() =>
  detailRows.value.reduce((sum, detail) => {
    const value = Number(detail.valueInDollars)
    if (!Number.isNaN(value) && value > 0) return sum + value

    const amount = Number(detail.amount)
    const price = Number(detail.pricePerAmount)
    if (!Number.isNaN(amount) && !Number.isNaN(price)) return sum + amount * price

    return sum
  }, 0),
)

const unsupportedMetalMessage = computed(() => {
  const notes = quote.value?.notes
  if (typeof notes !== 'string') return ''
  if (!notes.toLowerCase().includes('unsupported metal')) return ''
  const pipeIndex = notes.indexOf('|')
  return pipeIndex !== -1 ? notes.slice(pipeIndex + 1).trim() : notes
})

const displayNotes = computed(() => {
  const notes = quote.value?.notes
  if (typeof notes !== 'string') return ''

  if (!unsupportedMetalMessage.value) return notes

  const pipeIndex = notes.indexOf('|')
  return pipeIndex !== -1 ? notes.slice(0, pipeIndex).trim() : notes
})

const displayAllNotes = computed(() => {
  const notes = quote.value?.notes
  if (typeof notes !== 'string') return ''
  return notes
})

function formatMoney(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(Number(value) || 0)
}

function formatDecimal(value, fractionDigits = 2) {
  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) return '-'
  return numericValue.toLocaleString(undefined, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  })
}

async function fetchQuote() {
  if (!resolvedQuoteId.value) {
    errorMessage.value = 'Invalid quote id.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  statusMessage.value = ''

  try {
    const response = await fetch(`${API_BASE}/quotes/${resolvedQuoteId.value}`)
    if (!response.ok) {
      throw new Error(`Unable to load quote (${response.status})`)
    }

    quote.value = await response.json()

    if (props.isEmbedded) {
      await fetchQuoteRejections()
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

async function fetchQuoteRejections() {
  quoteRejections.value = []
  if (!resolvedQuoteId.value) return

  try {
    const response = await fetch(`${API_BASE}/quote-rejections?quoteId=${resolvedQuoteId.value}`)
    if (response.ok) {
      const body = await response.json()
      quoteRejections.value = Array.isArray(body) ? body : []
    }
  } catch {
    // Rejection info is supplementary; silently ignore fetch failures.
  }
}

watch(() => props.quoteId, (newId) => {
  if (newId) {
    quote.value = null
    quoteRejections.value = []
    fetchQuote()
  }
})

async function loadRejectionReasons() {
  const response = await fetch(`${API_BASE}/quote-rejection-reasons?active=true`)

  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `Unable to load rejection reasons (${response.status})`)
  }

  const body = await response.json()
  rejectionReasons.value = Array.isArray(body) ? body : []
}

function openRejectModal() {
  errorMessage.value = ''
  statusMessage.value = ''
  isRejectModalOpen.value = true

  if (rejectionReasons.value.length === 0) {
    loadRejectionReasons().catch(() => {
      // Fall back to reason code text if lookup fails.
      rejectionReasons.value = []
    })
  }
}

function closeRejectModal() {
  if (isBusyRejecting.value) return
  isRejectModalOpen.value = false
}

async function updateQuoteStatus(nextStatus, successMessage = '') {
  if (!quote.value?.quoteId || !quote.value?.battery?.id) {
    throw new Error('Quote data is incomplete for status update.')
  }

  isUpdatingStatus.value = true

  try {
    const payload = {
      submittedDate: quote.value.submittedDate,
      quoteStatus: nextStatus,
      battery: { id: quote.value.battery.id },
      batteryQty: quote.value.batteryQty,
      notes: quote.value.notes,
    }

    const response = await fetch(`${API_BASE}/quotes/${quote.value.quoteId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      const body = await response.text()
      throw new Error(body || `Status update failed (${response.status})`)
    }

    quote.value = await response.json()
    statusMessage.value = successMessage || `Quote marked as ${nextStatus}.`

    if (props.isEmbedded) {
      emit('quote-updated', quote.value)
    } else {
      setTimeout(() => router.push({ name: 'quotes' }), 800)
    }
  } finally {
    isUpdatingStatus.value = false
  }
}

async function submitRejection({ rejectCode, reasonLabel, comments, otherValue }) {
  if (!quote.value?.quoteId) {
    errorMessage.value = 'Quote data is incomplete for rejection.'
    return
  }

  isSubmittingRejection.value = true
  errorMessage.value = ''
  statusMessage.value = ''

  try {
    if (rejectionReasons.value.length === 0) {
      await loadRejectionReasons()
    }

    const matchedReason = rejectionReasons.value.find(
      (reason) => String(reason.reasonName || '').trim().toLowerCase() === reasonLabel.toLowerCase(),
    )

    const payload = {
      quote: { quoteId: quote.value.quoteId },
      reason: matchedReason?.reasonId ? { reasonId: matchedReason.reasonId } : null,
      otherValue: otherValue || (!matchedReason ? `${rejectCode}: ${reasonLabel}` : ''),
      comments,
    }

    const createResponse = await fetch(`${API_BASE}/quote-rejections`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    if (!createResponse.ok) {
      const body = await createResponse.text()
      throw new Error(body || `Rejection submit failed (${createResponse.status})`)
    }

    await updateQuoteStatus('REJECTED', 'Quote rejected and reason recorded.')
    isRejectModalOpen.value = false
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmittingRejection.value = false
  }
}

async function handleAcceptQuote() {
  errorMessage.value = ''
  statusMessage.value = ''

  try {
    await updateQuoteStatus('ACCEPTED')
  } catch (error) {
    errorMessage.value = error.message
  }
}

onMounted(fetchQuote)
</script>

<template>
  <main class="summary-page">
    <section class="summary-card">
      <header class="summary-header">Quote Summary</header>

      <div class="summary-body">
        <p v-if="isLoading" class="state-message">Loading quote summary...</p>
        <p v-else-if="errorMessage" class="state-message error">{{ errorMessage }}</p>

        <template v-if="quote">
          <div class="top-row">
            <div>Quote ID: <strong>{{ displayQuoteId }}</strong></div>
            <div>Date: <strong>{{ formattedDate }}</strong></div>
          </div>

          <div class="battery-row">
            Battery: <strong>{{ batterySummary }}</strong>
          </div>

          <div class="section-label">Metal Value Breakdown:</div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Metal</th>
                  <th>Amount (kg)</th>
                  <th>Price ($/kg)</th>
                  <th>Value ($)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="detail in detailRows" :key="detail.id || detail.metal">
                  <td>{{ detail.metal }}</td>
                  <td>{{ formatDecimal(detail.amount, 2) }}</td>
                  <td>{{ formatDecimal(detail.pricePerAmount, 2) }}</td>
                  <td>{{ formatDecimal(detail.valueInDollars, 2) }}</td>
                </tr>
                <tr v-if="detailRows.length === 0">
                  <td colspan="4">No detail rows available.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="total-row">
            Total Value: <strong>{{ formatMoney(totalValue) }}</strong>
          </div>

          <p v-if="unsupportedMetalMessage && !isEmbedded" class="unsupported-metal-message">{{ unsupportedMetalMessage }}</p>

          <div v-if="displayNotes && !isEmbedded" class="info-section">
            <div class="section-label">Notes:</div>
            <p class="info-text">{{ displayNotes }}</p>
          </div>

          <div v-if="displayNotes && isEmbedded" class="info-section">
            <div class="section-label">Notes:</div>
            <p class="info-text">{{ displayAllNotes }}</p>
          </div>

          <template v-if="isEmbedded">
            <div v-if="quoteRejections.length > 0" class="info-section">
              <div class="section-label">Rejection Details:</div>
              <div v-for="rejection in quoteRejections" :key="rejection.id" class="rejection-block">
                <div v-if="rejection.reason?.reasonName" class="rejection-row">
                  <span class="rejection-label">Reason:</span>
                  <span>{{ rejection.reason.reasonName }}</span>
                </div>
                <div v-if="rejection.otherValue" class="rejection-row">
                  <span class="rejection-label">Other Reason:</span>
                  <span>{{ rejection.otherValue }}</span>
                </div>
                <div v-if="rejection.comments" class="rejection-row">
                  <span class="rejection-label">Additional Comments:</span>
                  <span>{{ rejection.comments }}</span>
                </div>
              </div>
            </div>
          </template>

          <div v-if="!isEmbedded" class="actions-row">
            <button type="button" @click="handleAcceptQuote" :disabled="isUpdatingStatus || isSubmittingRejection">
              Accept Quote
            </button>
            <button type="button" class="secondary" @click="openRejectModal" :disabled="isUpdatingStatus || isSubmittingRejection">
              Reject Quote
            </button>
          </div>

          <p v-if="statusMessage" class="state-message success">{{ statusMessage }}</p>
        </template>
      </div>
    </section>

    <section v-if="!isEmbedded && isRejectModalOpen" class="reject-backdrop" @click.self="closeRejectModal">
      <RejectQuoteForm :is-submitting="isBusyRejecting" @cancel="closeRejectModal" @submit="submitRejection" />
    </section>
  </main>
</template>

<style scoped>
.summary-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.reject-backdrop {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgb(0 0 0 / 35%);
}

.summary-card {
  width: min(980px, 100%);
  border: 1px solid #8f9aa3;
  background: #ffffff;
  box-shadow: 0 10px 20px rgb(0 0 0 / 8%);
}

.summary-header {
  border-bottom: 1px solid #8f9aa3;
  text-align: center;
  font-size: 1.9rem;
  font-weight: 700;
  padding: 1rem;
}

.summary-body {
  padding: 1.1rem;
  display: grid;
  gap: 1rem;
}

.top-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.battery-row,
.section-label,
.total-row {
  font-size: 1rem;
}

.section-label {
  font-weight: 600;
}

.table-wrap {
  border: 1px solid #9ba6ad;
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid #d5dce0;
  padding: 0.6rem;
  text-align: left;
}

.actions-row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
}

button {
  cursor: pointer;
  background: #133e67;
  color: #ffffff;
  border: 1px solid #133e67;
  padding: 0.55rem 0.9rem;
  font: inherit;
}

button.secondary {
  background: #ffffff;
  color: #133e67;
}

.link-btn {
  border: none;
  background: transparent;
  color: #133e67;
  padding: 0;
}

.state-message {
  margin: 0;
  color: #133e67;
}

.state-message.error {
  color: #a1192e;
}

.state-message.success {
  color: #196132;
}

.unsupported-metal-message {
  margin: 0;
  color: #a1192e;
  font-weight: 600;
}

.info-section {
  display: grid;
  gap: 0.35rem;
}

.info-text {
  margin: 0;
}

.rejection-block {
  display: grid;
  gap: 0.3rem;
  padding: 0.5rem 0.75rem;
  border-left: 3px solid #c64f5f;
  background: #fdf2f4;
}

.rejection-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.95rem;
}

.rejection-label {
  font-weight: 600;
  min-width: 80px;
}

@media (max-width: 760px) {
  .actions-row {
    flex-direction: column;
  }

  .actions-row button {
    width: 100%;
  }
}
</style>
