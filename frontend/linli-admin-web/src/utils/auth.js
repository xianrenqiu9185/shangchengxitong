const TOKEN_KEY = 'linli_admin_token'
const ROLE_KEY = 'linli_admin_role'

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const getRole = () => localStorage.getItem(ROLE_KEY) || 'operator'
export const setRole = (role) => localStorage.setItem(ROLE_KEY, role)

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
}
