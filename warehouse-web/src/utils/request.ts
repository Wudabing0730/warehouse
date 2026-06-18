import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { getToken, getRefreshToken, setTokens, removeTokens } from '@/utils/auth'

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// Refresh token queue to prevent multiple concurrent refreshes
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function subscribeTokenRefresh(cb: (token: string) => void): void {
  refreshSubscribers.push(cb)
}

function onTokenRefreshed(token: string): void {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

// Request interceptor: attach Bearer token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// Response interceptor: unwrap data, handle 401 with refresh
service.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = getRefreshToken()

      if (!refreshToken) {
        removeTokens()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(service(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await axios.post('/api/v1/auth/refresh', {
          refreshToken,
        })
        const { accessToken, refreshToken: newRefreshToken } = response.data
        setTokens(accessToken, newRefreshToken)
        onTokenRefreshed(accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return service(originalRequest)
      } catch (refreshError) {
        removeTokens()
        refreshSubscribers = []
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

export default service
