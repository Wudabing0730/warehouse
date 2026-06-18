import request from '@/utils/request'

export function getUserList(params: any) {
  return request({ url: '/user', method: 'get', params })
}

export function createUser(data: any) {
  return request({ url: '/user', method: 'post', data })
}

export function updateUser(id: number | string, data: any) {
  return request({ url: `/user/${id}`, method: 'put', data })
}

export function deleteUser(id: number | string) {
  return request({ url: `/user/${id}`, method: 'delete' })
}

export function getUserById(id: number | string) {
  return request({ url: `/user/${id}`, method: 'get' })
}

export function updatePassword(id: number | string, data: any) {
  return request({ url: `/user/${id}/password`, method: 'put', data })
}
