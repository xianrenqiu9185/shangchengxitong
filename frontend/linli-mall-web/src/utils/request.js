import axios from 'axios'
import { showToast } from 'vant'
import router from '@/router'
import { clearToken, getToken } from '@/utils/auth'
import { uuid } from '@/utils/format'
import { mockAdapter } from '@/mocks'

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 12000,
  adapter: useMock ? mockAdapter : undefined,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  config.headers['X-Trace-Id'] = uuid()
  if (config.idempotent) config.headers['Idempotency-Key'] = uuid()
  return config
})

let redirecting = false

service.interceptors.response.use(
  (response) => {
    const payload = response.data || {}
    if (payload.code === 0) return payload.data

    if (payload.code === 10002) {
      clearToken()
      if (!redirecting) {
        redirecting = true
        const redirect = router.currentRoute.value.fullPath
        router.replace({ name: 'login', query: { redirect } }).finally(() => {
          redirecting = false
        })
      }
    } else if (payload.code === 10003) {
      showToast('暂无操作权限')
    } else {
      const trace = payload.traceId ? `（${payload.traceId}）` : ''
      showToast(`${payload.message || '请求失败'}${trace}`)
    }

    return Promise.reject(payload)
  },
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    } else {
      showToast(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default service
