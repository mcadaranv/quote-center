import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')

export const useQuoteStatsStore = defineStore('quoteStats', () => {
  const quotes = ref([])
  const loaded = ref(false)

  const total = computed(() => quotes.value.length)
  const accepted = computed(() => quotes.value.filter(q => String(q.quoteStatus).toUpperCase() === 'ACCEPTED').length)
  const rejected = computed(() => quotes.value.filter(q => String(q.quoteStatus).toUpperCase() === 'REJECTED').length)

  async function fetchStats() {
    try {
      const res = await fetch(`${API_BASE}/quotes`)
      if (res.ok) {
        quotes.value = await res.json()
        loaded.value = true
      }
    } catch {
      // stats remain unchanged on failure
    }
  }

  return { total, accepted, rejected, loaded, fetchStats }
})
