import { defineStore } from 'pinia'
import { addCartItem, clearCart, deleteCartItem, getCart, updateCartItem } from '@/api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    loading: false,
  }),
  getters: {
    count: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    selectedItems: (state) => state.items.filter((item) => item.checked),
    selectedCount() {
      return this.selectedItems.reduce((sum, item) => sum + item.quantity, 0)
    },
    total() {
      return this.selectedItems.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
    },
    allChecked: (state) => state.items.length > 0 && state.items.every((item) => item.checked),
  },
  actions: {
    async fetch() {
      this.loading = true
      try {
        const data = await getCart()
        this.items = data.items || []
      } finally {
        this.loading = false
      }
    },
    async add(productId, quantity = 1) {
      const data = await addCartItem({ productId, quantity })
      this.items = data.items || []
    },
    async update(id, payload) {
      const data = await updateCartItem(id, payload)
      this.items = data.items || []
    },
    async remove(id) {
      const data = await deleteCartItem(id)
      this.items = data.items || []
    },
    async clear() {
      const data = await clearCart()
      this.items = data.items || []
    },
    async toggleAll(checked) {
      await Promise.all(this.items.map((item) => this.update(item.id, { checked })))
    },
  },
})
