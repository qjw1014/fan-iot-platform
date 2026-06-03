import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/api'

const request = axios.create({
  baseURL: import.meta.env.DEV ? '' : import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (typeof body?.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      await router.replace('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
