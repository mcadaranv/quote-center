import { createRouter, createWebHistory } from 'vue-router'
import BatterySearchView from '../components/BatterySearchView.vue'
import QuoteSummaryView from '../components/QuoteSummaryView.vue'
import QuotesView from '../components/QuotesView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'battery-search',
      component: BatterySearchView,
    },
    {
      path: '/quotes',
      name: 'quotes',
      component: QuotesView,
    },
    {
      path: '/quote-summary/:quoteId',
      name: 'quote-summary',
      component: QuoteSummaryView,
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

export default router
