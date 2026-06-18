import request from '@/utils/request'

export function getUserList(params: any) {
  return request({ url: '/users', method: 'get', params })
}

export function createUser(data: any) {
  return request({ url: '/users', method: 'post', data })
}

export function updateUser(data: any) {
  return request({ url: `/users/${data.id}`, method: 'put', data })
}

export function deleteUser(id: number | string) {
  return request({ url: `/users/${id}`, method: 'delete' })
}

export function getUserById(id: number | string) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function updatePassword(id: number | string, data: any) {
  return request({ url: `/users/${id}/password`, method: 'put', data })
}
