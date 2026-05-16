<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  isSubmitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['cancel', 'submit'])

const API_BASE = (import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`).replace(/\/$/, '')

const reasonOptions = ref([])
const isLoadingReasons = ref(false)
const reasonsError = ref('')

const selectedReasonCode = ref('')
const comments = ref('')
const otherValue = ref('')
const localError = ref('')

const selectedReason = computed(() => reasonOptions.value.find((option) => option.code === selectedReasonCode.value) || null)
const isOtherSelected = computed(() => selectedReason.value?.label.toLowerCase() === 'other')

watch(selectedReasonCode, () => {
  localError.value = ''

  if (!isOtherSelected.value) {
    otherValue.value = ''
  }
})

async function loadReasons() {
  isLoadingReasons.value = true
  reasonsError.value = ''

  try {
    const response = await fetch(`${API_BASE}/quote-rejection-reasons?active=true`)
    if (!response.ok) {
      throw new Error(`Unable to load rejection reasons (${response.status})`)
    }

    const body = await response.json()
    reasonOptions.value = (Array.isArray(body) ? body : []).map((reason) => ({
      code: String(reason.reasonId),
      label: reason.reasonName,
    }))
  } catch (error) {
    reasonsError.value = error.message
  } finally {
    isLoadingReasons.value = false
  }
}

function submitRejection() {
  if (!selectedReason.value) {
    localError.value = 'Select one reason before submitting the rejection.'
    return
  }

  if (isOtherSelected.value && !otherValue.value.trim()) {
    localError.value = 'Provide the other reason when selecting Other.'
    return
  }

  emit('submit', {
    rejectCode: selectedReason.value.code,
    reasonLabel: selectedReason.value.label,
    comments: comments.value.trim(),
    otherValue: isOtherSelected.value ? otherValue.value.trim() : '',
  })
}

onMounted(loadReasons)
</script>

<template>
  <section class="reject-card" role="dialog" aria-modal="true" aria-label="Reject Quote">
    <header class="reject-header">Reject Quote</header>

    <div class="reject-body">
      <p class="intro">Please provide a reason for rejecting this quote:</p>

      <p v-if="isLoadingReasons" class="form-info">Loading reasons...</p>
      <p v-else-if="reasonsError" class="form-error">{{ reasonsError }}</p>

      <div v-else class="reason-list" role="radiogroup" aria-label="Rejection reasons">
        <label v-for="option in reasonOptions" :key="option.code" class="reason-option">
          <input v-model="selectedReasonCode" type="radio" name="rejectionReason" :value="option.code" :disabled="isSubmitting" />
          <span>{{ option.label }}</span>
        </label>
      </div>

    <div v-if="isOtherSelected" class="comments-row">
        <label for="rejectionOtherValue">Other Reason:</label>
        <input id="rejectionOtherValue" v-model="otherValue" type="text" maxlength="500" :disabled="isSubmitting" />
      </div>

      <div class="comments-row">
        <label for="rejectionComments">Additional Comments:</label>
        <input id="rejectionComments" v-model="comments" type="text" maxlength="2000" :disabled="isSubmitting" />
      </div>

      <p v-if="localError" class="form-error">{{ localError }}</p>

      <div class="actions-row">
        <button type="button" class="secondary" :disabled="isSubmitting" @click="emit('cancel')">Cancel</button>
        <button type="button" :disabled="isSubmitting" @click="submitRejection">
          {{ isSubmitting ? 'Submitting...' : 'Submit Rejection' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.reject-card {
  width: min(680px, 100%);
  border: 1px solid #8f9aa3;
  background: #ffffff;
  box-shadow: 0 12px 24px rgb(0 0 0 / 15%);
}

.reject-header {
  border-bottom: 1px solid #8f9aa3;
  padding: 0.9rem 1rem;
  font-size: 1.35rem;
  font-weight: 700;
}

.reject-body {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
}

.intro {
  margin: 0;
}

.reason-list {
  display: grid;
  gap: 0.55rem;
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.reason-option input {
  width: auto;
  height: auto;
}

.comments-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.65rem;
  align-items: center;
}

.comments-row input {
  height: 38px;
  border: 1px solid #9ba6ad;
  padding: 0 0.6rem;
  font: inherit;
}

.actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
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

.form-error {
  margin: 0;
  color: #a1192e;
}

.form-info {
  margin: 0;
  color: #52616b;
}

@media (max-width: 760px) {
  .comments-row {
    grid-template-columns: 1fr;
  }

  .actions-row {
    flex-direction: column;
  }

  .actions-row button {
    width: 100%;
  }
}
</style>
