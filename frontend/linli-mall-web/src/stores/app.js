import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    deliveryFee: 300,
    freeShippingThreshold: 3990,
    paymentTimeoutMinutes: 30,
  }),
})
