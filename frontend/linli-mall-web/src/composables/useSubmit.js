import { ref } from 'vue'

export function useSubmit(handler) {
  const submitting = ref(false)

  async function run(...args) {
    if (submitting.value) return undefined
    submitting.value = true
    try {
      return await handler(...args)
    } finally {
      submitting.value = false
    }
  }

  return { submitting, run }
}
