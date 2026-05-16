<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import QuoteSummaryView from './QuoteSummaryView.vue'

const router = useRouter()
const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')

const quotes = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const selectedQuoteId = ref(null)
const sortBy = ref('quoteId')
const sortDirection = ref('desc')

const sortedQuotes = computed(() => {
  const direction = sortDirection.value === 'asc' ? 1 : -1

  return [...quotes.value].sort((a, b) => {
    const aValue = sortableValue(a, sortBy.value)
    const bValue = sortableValue(b, sortBy.value)

    if (aValue == null && bValue == null) return 0
    if (aValue == null) return 1
    if (bValue == null) return -1

    if (aValue < bValue) return -1 * direction
    if (aValue > bValue) return 1 * direction
    return 0
  })
})

async function fetchQuotes() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch(`${API_BASE}/quotes`)
    if (!response.ok) {
      throw new Error(`Failed to load quotes (${response.status})`)
    }
    quotes.value = await response.json()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

function selectRow(quoteId) {
  selectedQuoteId.value = selectedQuoteId.value === quoteId ? null : quoteId
}

function formatQuoteId(id) {
  return `RW-${String(id).padStart(5, '0')}`
}

function batterySummaryText(quote) {
  if (!quote.battery) return 'N/A'
  const qty = quote.batteryQty ?? 0
  const make = quote.battery.make || ''
  const model = quote.battery.model || ''
  return `${qty}x ${make} ${model}`.trim()
}

function totalValueFor(quote) {
  const details = quote.details || []
  if (details.length === 0) return null

  return details.reduce((sum, detail) => {
    const value = Number(detail.valueInDollars)
    if (!Number.isNaN(value) && value > 0) return sum + value

    const amount = Number(detail.amount)
    const price = Number(detail.pricePerAmount)
    if (!Number.isNaN(amount) && !Number.isNaN(price)) return sum + amount * price

    return sum
  }, 0)
}

function formatMoney(value) {
  if (value === null) return '—'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function sortableValue(quote, column) {
  switch (column) {
    case 'quoteId':
      return Number(quote.quoteId || 0)
    case 'submittedDate':
      return quote.submittedDate || ''
    case 'battery':
      return batterySummaryText(quote).toLowerCase()
    case 'quoteStatus':
      return String(quote.quoteStatus || 'PENDING').toLowerCase()
    case 'totalValue': {
      const value = totalValueFor(quote)
      return value == null ? null : Number(value)
    }
    default:
      return ''
  }
}

function toggleSort(column) {
  if (sortBy.value === column) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
    return
  }

  sortBy.value = column
  sortDirection.value = column === 'quoteId' ? 'desc' : 'asc'
}

function sortIndicator(column) {
  if (sortBy.value !== column) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

function ariaSort(column) {
  if (sortBy.value !== column) return 'none'
  return sortDirection.value === 'asc' ? 'ascending' : 'descending'
}

function statusClass(status) {
  switch (String(status).toUpperCase()) {
    case 'ACCEPTED': return 'status-accepted'
    case 'REJECTED': return 'status-rejected'
    default:         return 'status-pending'
  }
}

async function handleQuoteUpdated() {
  await fetchQuotes()
}

onMounted(fetchQuotes)
</script>

<template>
  <main class="quotes-page">
    <section class="quotes-card">
      <header class="quotes-header">
        <span>All Quotes</span>
        <button type="button" class="new-quote-btn" @click="router.push('/')">+ New Quote</button>
      </header>

      <div class="quotes-body">
        <p v-if="isLoading" class="state-message">Loading quotes...</p>
        <p v-else-if="errorMessage" class="state-message error">{{ errorMessage }}</p>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th :aria-sort="ariaSort('quoteId')">
                  <button type="button" class="sort-button" @click="toggleSort('quoteId')">
                    Quote ID <span class="sort-indicator">{{ sortIndicator('quoteId') }}</span>
                  </button>
                </th>
                <th :aria-sort="ariaSort('submittedDate')">
                  <button type="button" class="sort-button" @click="toggleSort('submittedDate')">
                    Date <span class="sort-indicator">{{ sortIndicator('submittedDate') }}</span>
                  </button>
                </th>
                <th :aria-sort="ariaSort('battery')">
                  <button type="button" class="sort-button" @click="toggleSort('battery')">
                    Battery <span class="sort-indicator">{{ sortIndicator('battery') }}</span>
                  </button>
                </th>
                <th :aria-sort="ariaSort('quoteStatus')">
                  <button type="button" class="sort-button" @click="toggleSort('quoteStatus')">
                    Status <span class="sort-indicator">{{ sortIndicator('quoteStatus') }}</span>
                  </button>
                </th>
                <th :aria-sort="ariaSort('totalValue')">
                  <button type="button" class="sort-button" @click="toggleSort('totalValue')">
                    Total Value <span class="sort-indicator">{{ sortIndicator('totalValue') }}</span>
                  </button>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="quote in sortedQuotes"
                :key="quote.quoteId"
                class="quote-row"
                :class="{ selected: selectedQuoteId === quote.quoteId }"
                @click="selectRow(quote.quoteId)"
              >
                <td>{{ formatQuoteId(quote.quoteId) }}</td>
                <td>{{ quote.submittedDate || '—' }}</td>
                <td>{{ batterySummaryText(quote) }}</td>
                <td>
                  <span class="status-badge" :class="statusClass(quote.quoteStatus)">
                    {{ quote.quoteStatus || 'PENDING' }}
                  </span>
                </td>
                <td>{{ formatMoney(totalValueFor(quote)) }}</td>
              </tr>
              <tr v-if="!isLoading && sortedQuotes.length === 0">
                <td colspan="5" class="empty-row">No quotes found.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <QuoteSummaryView
      v-if="selectedQuoteId"
      :quote-id="selectedQuoteId"
      :is-embedded="true"
      @quote-updated="handleQuoteUpdated"
    />
  </main>
</template>

<style scoped>
.quotes-page {
  min-height: 100vh;
  display: grid;
  align-content: start;
  gap: 1.5rem;
  padding: 1.5rem 1rem;
}

.quotes-card {
  width: min(1080px, 100%);
  margin: 0 auto;
  border: 1px solid #8f9aa3;
  background: #ffffff;
  box-shadow: 0 10px 20px rgb(0 0 0 / 8%);
}

.quotes-header {
  border-bottom: 1px solid #8f9aa3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  font-size: 1.55rem;
  font-weight: 700;
}

.new-quote-btn {
  cursor: pointer;
  background: #133e67;
  color: #ffffff;
  border: 1px solid #133e67;
  padding: 0.45rem 0.85rem;
  font: inherit;
  font-size: 0.9rem;
}

.quotes-body {
  padding: 1rem;
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
  padding: 0.65rem 0.75rem;
  text-align: left;
  white-space: nowrap;
}

th {
  background: #f4f6f8;
  font-weight: 600;
}

.sort-button {
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  font: inherit;
  font-weight: inherit;
  color: inherit;
  cursor: pointer;
}

.sort-indicator {
  display: inline-block;
  width: 1rem;
  text-align: center;
}

.quote-row {
  cursor: pointer;
}

.quote-row:hover {
  background: #eef2f6;
}

.quote-row.selected {
  background: #dde7f0;
}

.empty-row {
  text-align: center;
  color: #52616b;
}

.status-badge {
  display: inline-block;
  padding: 0.2rem 0.55rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: 3px;
}

.status-pending {
  background: #e8f0fe;
  color: #1a56a0;
}

.status-accepted {
  background: #e6f4ea;
  color: #196132;
}

.status-rejected {
  background: #fce8ea;
  color: #8f1126;
}

.state-message {
  margin: 0 0 0.75rem;
  color: #133e67;
}

.state-message.error {
  color: #a1192e;
}

@media (max-width: 700px) {
  .quotes-header {
    font-size: 1.2rem;
  }
}
</style>
