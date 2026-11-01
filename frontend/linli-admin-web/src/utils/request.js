import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearSession, getToken } from '@/utils/auth'
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

service.interceptors.response.use(
  (response) => {
    const payload = response.data || {}
    if (payload.code === 0) return payload.data
    if (payload.code === 10002) {
      clearSession()
      router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (payload.code === 10003) {
      ElMessage.error('当前账号无此操作权限')
    } else {
      ElMessage.error(`${payload.message || '请求失败'}${payload.traceId ? `（${payload.traceId}）` : ''}`)
    }
    return Promise.reject(payload)
  },
  (error) => {
    if (error.response?.status === 401) {
      clearSession()
      router.replace({ name: 'login' })
    } else {
      ElMessage.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default service
