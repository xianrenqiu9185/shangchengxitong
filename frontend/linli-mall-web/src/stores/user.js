import { defineStore } from 'pinia'
import { login, logout, register } from '@/api/auth'
import { getMe } from '@/api/user'
import { clearToken, getToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    profile: null,
    permissions: [],
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.profile?.nickname || state.profile?.phone || '邻里会员',
  },
  actions: {
    applySession(data) {
      this.token = data.token
      this.profile = data.user
      this.permissions = data.permissions || []
      setToken(data.token)
    },
    async login(payload) {
      const data = await login(payload)
      this.applySession(data)
      return data
    },
    async register(payload) {
      const data = await register(payload)
      this.applySession(data)
      return data
    },
    async fetchProfile() {
      const data = await getMe()
      this.profile = data.user
      this.permissions = data.permissions || []
      return data
    },
    async logout() {
      try {
        await logout()
      } finally {
        this.reset()
      }
    },
    reset() {
      this.token = ''
      this.profile = null
      this.permissions = []
      clearToken()
    },
    hasPerm(perm) {
      return !perm || this.permissions.includes(perm)
    },
  },
})
