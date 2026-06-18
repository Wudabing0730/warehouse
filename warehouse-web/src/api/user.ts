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

/**
 * 管理员重置用户密码(P1-5 修复)
 * 不需要 oldPassword,只接 newPassword
 */
export function resetPasswordAdmin(id: number | string, newPassword: string) {
  return request({ url: `/users/${id}/password/reset`, method: 'put', data: { newPassword } })
}
