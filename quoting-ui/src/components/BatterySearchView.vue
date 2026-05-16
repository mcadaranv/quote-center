<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuoteStatsStore } from '../stores/quoteStats'

const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')
const router = useRouter()
const quoteStats = useQuoteStatsStore()

const batteries = ref([])
const selectedBattery = ref(null)
const isLoading = ref(false)
const isSearching = ref(false)
const isSubmittingQuote = ref(false)
const errorMessage = ref('')
const quoteMessage = ref('')

const selectedYear = ref('')
const selectedMake = ref('')
const selectedModel = ref('')
const quantity = ref(1)
const notes = ref('')

const isTrimModalOpen = ref(false)
const trimOptions = ref([])
const selectedTrim = ref('')
const isErrorModalOpen = ref(false)
const errorModalTitle = ref('Service Unavailable')
const errorModalMessage = ref('')

const isServiceDown = ref(false)
const isCheckingHealth = ref(false)

async function checkMetalsPricingHealth() {
  isCheckingHealth.value = true
  try {
    const response = await fetch(`${API_BASE}/quotes/metals-pricing/health`)
    const body = await response.json().catch(() => null)
    const status = body?.status ?? (response.ok ? 'UP' : 'DOWN')
    const wasDown = isServiceDown.value
    isServiceDown.value = status !== 'UP'
    if (wasDown && !isServiceDown.value && batteries.value.length === 0) {
      fetchBatteries()
    }
  } catch {
    isServiceDown.value = true
  } finally {
    isCheckingHealth.value = false
  }
}

const metalNameMap = {
  ni: 'Nickel',
  co: 'Cobalt',
  li: 'Lithium',
  cu: 'Copper',
  al: 'Aluminum',
}

async function fetchBatteries() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch(`${API_BASE}/car-battery-breakdowns`)

    if (!response.ok) {
      throw new Error(`Battery lookup failed (${response.status})`)
    }

    batteries.value = await response.json()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

const years = computed(() => {
  const yearSet = new Set()

  for (const battery of batteries.value) {
    if (battery.modelYearStart != null) yearSet.add(battery.modelYearStart)
    if (battery.modelYearEnd != null) yearSet.add(battery.modelYearEnd)
  }

  return [...yearSet].sort((a, b) => b - a)
})

const makes = computed(() => {
  const makeSet = new Set()

  for (const battery of batteries.value) {
    if (!matchesYear(battery)) continue
    makeSet.add(battery.make)
  }

  return [...makeSet].sort((a, b) => a.localeCompare(b))
})

const models = computed(() => {
  const modelSet = new Set()

  for (const battery of batteries.value) {
    if (!matchesYear(battery)) continue
    if (selectedMake.value && battery.make !== selectedMake.value) continue
    modelSet.add(battery.model)
  }

  return [...modelSet].sort((a, b) => a.localeCompare(b))
})

const criteriaSummary = computed(() => {
  const parts = []

  if (selectedYear.value) parts.push(`Year: ${selectedYear.value}`)
  if (selectedMake.value) parts.push(`Make: ${selectedMake.value}`)
  if (selectedModel.value) parts.push(`Model: ${selectedModel.value}`)

  const trimValue = selectedBattery.value?.trim || selectedTrim.value
  if (trimValue) parts.push(`Trim: ${trimValue}`)

  return parts.length > 0 ? parts.join(' | ') : 'No criteria selected'
})

const effectiveQuantity = computed(() => Math.max(1, Number(quantity.value) || 1))

const scaledCapacityText = computed(() => {
  if (!selectedBattery.value) return 'N/A'

  const baseCapacity = Number(selectedBattery.value.batteryCapacityKwh || 0)
  if (!baseCapacity) return 'N/A'

  const scaled = baseCapacity * effectiveQuantity.value
  return scaled.toLocaleString(undefined, { maximumFractionDigits: 2 })
})

const estimatedWeightText = computed(() => {
  if (!selectedBattery.value) return 'N/A'

  const kg = Number(selectedBattery.value.packWeightKg || 0)
  if (!kg) return 'N/A'

  const lbs = kg * effectiveQuantity.value * 2.20462
  return `${Math.round(lbs).toLocaleString()} lbs`
})

function scaledMetalAmount(value) {
  const numericValue = Number(value || 0)
  return Number((numericValue * effectiveQuantity.value).toFixed(4))
}

function toMetalName(fieldName) {
  const shortCode = fieldName.replace(/WeightKg$/, '')
  return metalNameMap[shortCode] || shortCode.charAt(0).toUpperCase() + shortCode.slice(1)
}

const metalCompositionLines = computed(() => {
  if (!selectedBattery.value) return []

  return Object.entries(selectedBattery.value)
    .filter(([key, value]) => key.endsWith('WeightKg') && key !== 'packWeightKg' && value != null)
    .map(([key, value]) => {
      const amount = scaledMetalAmount(value)
      return {
        metal: toMetalName(key),
        amount,
        amountText: `${amount.toLocaleString(undefined, { maximumFractionDigits: 2 })} kg`,
      }
    })
})

watch(selectedYear, () => {
  if (selectedMake.value && !makes.value.includes(selectedMake.value)) {
    selectedMake.value = ''
  }

  if (selectedModel.value && !models.value.includes(selectedModel.value)) {
    selectedModel.value = ''
  }
})

watch(selectedMake, () => {
  if (selectedModel.value && !models.value.includes(selectedModel.value)) {
    selectedModel.value = ''
  }
})

function matchesYear(battery) {
  if (!selectedYear.value) return true

  const year = Number(selectedYear.value)
  const start = Number(battery.modelYearStart || year)
  const end = Number(battery.modelYearEnd || start)
  return year >= start && year <= end
}

async function searchBatteryTypes() {
  errorMessage.value = ''
  quoteMessage.value = ''

  if (!selectedYear.value || !selectedMake.value || !selectedModel.value) {
    errorMessage.value = 'Select year, make, and model to search battery types.'
    selectedBattery.value = null
    return
  }

  isSearching.value = true
  try {
    const results = await fetchBatterySearchResults({
      year: selectedYear.value,
      make: selectedMake.value,
      model: selectedModel.value,
    })

    if (results.length === 0) {
      selectedBattery.value = null
      errorMessage.value = 'No battery type found for the selected year, make, and model.'
      return
    }

    if (results.length === 1) {
      selectedBattery.value = results[0]
      return
    }

    const uniqueTrims = [...new Set(results.map((item) => (item.trim || 'Unknown').trim()))]

    if (uniqueTrims.length <= 1) {
      selectedBattery.value = results[0]
      return
    }

    trimOptions.value = uniqueTrims
    selectedTrim.value = uniqueTrims[0]
    isTrimModalOpen.value = true
  } catch (error) {
    selectedBattery.value = null
    errorMessage.value = error.message
  } finally {
    isSearching.value = false
  }
}

async function fetchBatterySearchResults({ make, model, year, trim }) {
  const query = new URLSearchParams({ make, model })

  if (year) query.set('year', year)
  if (trim) query.set('trim', trim)

  const response = await fetch(`${API_BASE}/car-battery-breakdowns?${query.toString()}`)

  if (!response.ok) {
    throw new Error(`Battery search failed (${response.status})`)
  }

  const body = await response.json()
  return Array.isArray(body) ? body : []
}

function closeTrimModal() {
  isTrimModalOpen.value = false
  trimOptions.value = []
  selectedTrim.value = ''
}

async function confirmTrimSelection() {
  if (!selectedTrim.value) {
    errorMessage.value = 'Select a trim to continue.'
    return
  }

  isSearching.value = true
  errorMessage.value = ''

  try {
    const results = await fetchBatterySearchResults({
      year: selectedYear.value,
      make: selectedMake.value,
      model: selectedModel.value,
      trim: selectedTrim.value,
    })

    if (results.length === 0) {
      selectedBattery.value = null
      errorMessage.value = 'No battery details found for the selected trim.'
      closeTrimModal()
      return
    }

    selectedBattery.value = results[0]
    closeTrimModal()
  } catch (error) {
    selectedBattery.value = null
    errorMessage.value = error.message
  } finally {
    isSearching.value = false
  }
}

async function calculateQuote() {
  if (!selectedBattery.value) {
    quoteMessage.value = 'Choose a battery type to calculate a quote.'
    return
  }

  if (metalCompositionLines.value.length === 0) {
    errorMessage.value = 'No metal composition data is available for this battery.'
    quoteMessage.value = ''
    return
  }

  errorMessage.value = ''
  quoteMessage.value = ''
  isSubmittingQuote.value = true

  try {
    const payload = {
      submittedDate: new Date().toISOString().slice(0, 10),
      quoteStatus: 'PENDING',
      battery: { id: selectedBattery.value.id },
      batteryQty: effectiveQuantity.value,
      notes: notes.value?.trim() || '',
      details: metalCompositionLines.value.map((line) => ({
        metal: line.metal.toLowerCase(),
        amount: line.amount,        
      })),
    }

    const response = await fetch(`${API_BASE}/quotes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      await throwQuoteApiError(response)
    }

    const createdQuote = await response.json()
    await quoteStats.fetchStats()
    await router.push({
      name: 'quote-summary',
      params: { quoteId: createdQuote.quoteId },
      query: { year: selectedYear.value || '' },
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmittingQuote.value = false
  }
}

async function throwQuoteApiError(response) {
  const bodyText = await response.text()

  let parsedBody = null
  try {
    parsedBody = bodyText ? JSON.parse(bodyText) : null
  } catch {
    parsedBody = null
  }

  const messageFromBody =
    (parsedBody && typeof parsedBody.message === 'string' && parsedBody.message.trim()) ||
    bodyText ||
    `Quote create failed (${response.status})`

  const status = (parsedBody && parsedBody.status) || response.status

  if (Number(status) === 503) {
    showErrorModal({
      title: 'Pricing Service Temporarily Unavailable',
      message: messageFromBody,
    })
  }

  throw new Error(messageFromBody)
}

function showErrorModal({ title, message }) {
  errorModalTitle.value = title || 'Error'
  errorModalMessage.value = message || 'An unexpected error occurred.'
  isErrorModalOpen.value = true
}

function closeErrorModal() {
  isErrorModalOpen.value = false
}

function clearForm() {
  selectedYear.value = ''
  selectedMake.value = ''
  selectedModel.value = ''
  selectedBattery.value = null
  closeTrimModal()
  quantity.value = 1
  notes.value = ''
  quoteMessage.value = ''
  errorMessage.value = ''
}

onMounted(async () => {
  quoteStats.fetchStats()
  await checkMetalsPricingHealth()
  if (!isServiceDown.value) {
    fetchBatteries()
  }
})
</script>

<template>
  <main class="search-page">
    <section class="quote-card">
      <header class="quote-header">
        <span>Get a Battery Quote</span>
        <div v-if="quoteStats.loaded" class="quote-stats">
          <span class="stat stat--total">Total: {{ quoteStats.total }}</span>
          <span class="stat stat--accepted">Accepted: {{ quoteStats.accepted }}</span>
          <span class="stat stat--rejected">Rejected: {{ quoteStats.rejected }}</span>
        </div>
      </header>

      <div class="quote-body">
        <div class="field-row">
          <label>Battery Type:</label>
          <select v-model="selectedYear">
            <option value="">Year</option>
            <option v-for="year in years" :key="year" :value="String(year)">{{ year }}</option>
          </select>

          <select v-model="selectedMake">
            <option value="">Make</option>
            <option v-for="make in makes" :key="make" :value="make">{{ make }}</option>
          </select>

          <select v-model="selectedModel">
            <option value="">Model</option>
            <option v-for="model in models" :key="model" :value="model">{{ model }}</option>
          </select>

          <button type="button" class="search-button" @click="searchBatteryTypes" :disabled="isSearching || isLoading">
            {{ isSearching ? 'Searching...' : 'Search Battery Types' }}
          </button>
        </div>

        <p v-if="isLoading" class="state-message">Loading battery data...</p>
        <p v-else-if="errorMessage" class="state-message error">{{ errorMessage }}</p>

        <div class="details-header">
          <div class="details-label">Battery Details:</div>
          <div v-if="selectedBattery" class="criteria-text">{{ criteriaSummary }}</div>
        </div>
        <section class="details-box" v-if="selectedBattery">
          <div class="details-head">
            <span>Capacity: {{ scaledCapacityText }} kWh</span>
            <span>Estimated Weight: {{ estimatedWeightText }}</span>
          </div>

          <div class="composition-title">Trim: {{ selectedBattery.trim || 'N/A' }}</div>

          <div class="composition-title">Metal Composition:</div>
          <div class="composition-grid">
            <span v-for="line in metalCompositionLines" :key="line.metal">- {{ line.metal }}: {{ line.amountText }}</span>
          </div>
        </section>
        <section class="details-box muted" v-else>
          Select filters above to view battery details.
        </section>

        <div class="single-field-row">
          <label for="quantity">Quantity:</label>
          <input id="quantity" v-model.number="quantity" type="number" min="1" class="quantity-input" />
        </div>

        <div class="single-field-row notes-row">
          <label for="notes">Additional Notes:</label>
          <input id="notes" v-model="notes" type="text" maxlength="250" />
        </div>

        <div class="actions-row">
          <button type="button" @click="calculateQuote" :disabled="isSubmittingQuote">
            {{ isSubmittingQuote ? 'Calculating...' : 'Calculate Quote' }}
          </button>
          <button type="button" class="secondary" @click="clearForm">Clear</button>
        </div>

        <p v-if="quoteMessage" class="state-message success">{{ quoteMessage }}</p>
      </div>
    </section>

    <section v-if="isTrimModalOpen" class="modal-backdrop" @click.self="closeTrimModal">
      <div class="trim-modal" role="dialog" aria-modal="true" aria-label="Select Trim">
        <h3>Select Trim</h3>
        <p>Multiple trims were found. Choose one to continue the search.</p>

        <div class="trim-list">
          <label v-for="trim in trimOptions" :key="trim" class="trim-option">
            <input v-model="selectedTrim" type="radio" name="trim" :value="trim" />
            <span>{{ trim }}</span>
          </label>
        </div>

        <div class="modal-actions">
          <button type="button" @click="confirmTrimSelection" :disabled="isSearching">Use Trim</button>
          <button type="button" class="secondary" @click="closeTrimModal">Cancel</button>
        </div>
      </div>
    </section>

    <section v-if="isServiceDown" class="modal-backdrop">
      <div class="service-down-modal" role="alertdialog" aria-modal="true" aria-label="Service Unavailable">
        <h3>Metal Pricing System Unavailable</h3>
        <p>The metal pricing service is currently not available. Please try again.</p>
        <div class="modal-actions">
          <button type="button" :disabled="isCheckingHealth" @click="checkMetalsPricingHealth">
            {{ isCheckingHealth ? 'Checking...' : 'Try Again' }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="isErrorModalOpen" class="modal-backdrop" @click.self="closeErrorModal">
      <div class="error-modal" role="alertdialog" aria-modal="true" aria-label="Quote Error">
        <h3>{{ errorModalTitle }}</h3>
        <p>{{ errorModalMessage }}</p>

        <div class="modal-actions">
          <button type="button" @click="closeErrorModal">Okay</button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.search-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.quote-card {
  width: min(980px, 100%);
  border: 1px solid #8f9aa3;
  background: #ffffff;
  box-shadow: 0 10px 20px rgb(0 0 0 / 8%);
}

.quote-header {
  border-bottom: 1px solid #8f9aa3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 1.9rem;
  font-weight: 700;
  padding: 1rem;
}

.quote-stats {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.stat {
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 12px;
  white-space: nowrap;
}

.stat--total    { background: #e9ecef; color: #495057; }
.stat--accepted { background: #d1e7dd; color: #0f5132; }
.stat--rejected { background: #f8d7da; color: #842029; }

.quote-body {
  padding: 1.1rem;
  display: grid;
  gap: 1rem;
}

.field-row {
  display: grid;
  grid-template-columns: auto 110px 150px 170px 220px;
  gap: 0.6rem;
  align-items: center;
}

label {
  font-weight: 600;
}

select,
input,
button {
  height: 38px;
  border: 1px solid #9ba6ad;
  padding: 0 0.6rem;
  font: inherit;
}

.search-button {
  background: #133e67;
  color: #ffffff;
  border-color: #133e67;
}

.details-label {
  font-weight: 600;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.6rem;
}

.criteria-text {
  font-size: 0.92rem;
  color: #465660;
  text-align: right;
}

.details-box {
  border: 1px solid #9ba6ad;
  padding: 0.9rem;
  display: grid;
  gap: 0.7rem;
}

.details-box.muted {
  color: #52616b;
}

.details-head {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.7rem;
}

.composition-title {
  font-weight: 600;
}

.composition-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.45rem 1rem;
}

.single-field-row {
  display: grid;
  grid-template-columns: 140px minmax(160px, 1fr);
  gap: 0.6rem;
  align-items: center;
}

.quantity-input {
  max-width: 90px;
}

.notes-row input {
  width: 100%;
}

.actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

button {
  cursor: pointer;
  background: #133e67;
  color: #ffffff;
  border-color: #133e67;
  padding: 0 0.95rem;
}

button.secondary {
  background: #ffffff;
  color: #133e67;
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

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 35%);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.trim-modal {
  width: min(420px, 100%);
  background: #ffffff;
  border: 1px solid #9ba6ad;
  padding: 1rem;
  box-shadow: 0 10px 24px rgb(0 0 0 / 18%);
  display: grid;
  gap: 0.8rem;
}

.error-modal {
  width: min(460px, 100%);
  background: #ffffff;
  border: 1px solid #c64f5f;
  padding: 1rem;
  box-shadow: 0 12px 26px rgb(0 0 0 / 20%);
  display: grid;
  gap: 0.8rem;
}

.trim-modal h3,
.trim-modal p,
.error-modal h3,
.error-modal p {
  margin: 0;
}

.error-modal h3 {
  color: #8f1126;
}

.trim-list {
  display: grid;
  gap: 0.45rem;
}

.trim-option {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.trim-option input {
  width: auto;
  height: auto;
}

.service-down-modal {
  width: min(460px, 100%);
  background: #ffffff;
  border: 1px solid #c64f5f;
  padding: 1rem;
  box-shadow: 0 12px 26px rgb(0 0 0 / 20%);
  display: grid;
  gap: 0.8rem;
}

.service-down-modal h3 {
  margin: 0;
  color: #8f1126;
}

.service-down-modal p {
  margin: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

@media (max-width: 900px) {
  .quote-header {
    font-size: 1.5rem;
  }

  .field-row {
    grid-template-columns: 1fr;
  }

  .details-head,
  .composition-grid,
  .single-field-row {
    grid-template-columns: 1fr;
  }

  .details-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .criteria-text {
    text-align: left;
  }

  .actions-row {
    justify-content: stretch;
  }

  .actions-row button {
    flex: 1;
  }
}
</style>
