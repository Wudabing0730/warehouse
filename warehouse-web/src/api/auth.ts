import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface RefreshParams {
  refreshToken: string
}

export function login(data: LoginParams) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function refreshToken(data: RefreshParams) {
  return request({ url: '/auth/refresh', method: 'post', data })
}

export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}
