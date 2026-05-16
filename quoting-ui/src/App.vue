<script setup>
import { onMounted, ref } from 'vue'

const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')
const appVersion = ref('')

onMounted(async () => {
  try {
    const res = await fetch(`${API_BASE}/info`)
    if (res.ok) {
      const data = await res.json()
      appVersion.value = data.version
    }
  } catch {
    // version remains empty if unavailable
  }
})
</script>

<template>
  <RouterView />
  <footer v-if="appVersion" class="app-footer">
    <span class="app-footer__version">v{{ appVersion }}</span>
  </footer>
</template>

<style scoped>
.app-footer {
  position: fixed;
  bottom: 0;
  right: 0;
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
  color: #6c757d;
  background: transparent;
  pointer-events: none;
}
</style>
