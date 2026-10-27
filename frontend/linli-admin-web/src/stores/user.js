import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/admin'
import { getMe } from '@/api/admin'
import { clearSession, getRole, getToken, setRole, setToken } from '@/utils/auth'

export const useUserStore = defineStore('admin-user', {
  state: () => ({
    token: getToken(),
    role: getRole(),
    profile: null,
    permissions: [],
  }),
  getters: {
    isAdmin: (state) => state.role === 'admin',
    displayName: (state) => state.profile?.nickname || '运营人员',
  },
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      this.profile = data.user
      this.role = data.user.role
      this.permissions = data.permissions || []
      setToken(data.token)
      setRole(this.role)
      return data
    },
    async fetchProfile() {
      const data = await getMe()
      this.profile = data.user
      this.role = data.user.role
      this.permissions = data.permissions || []
      setRole(this.role)
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.reset()
      }
    },
    reset() {
      this.token = ''
      this.profile = null
      this.permissions = []
      clearSession()
    },
    hasPerm(perm) {
      return !perm || this.permissions.includes(perm)
    },
  },
})
