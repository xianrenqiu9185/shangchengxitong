import { useUserStore } from '@/stores/user'

export default {
  mounted(el, binding) {
    const user = useUserStore()
    if (!user.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}
