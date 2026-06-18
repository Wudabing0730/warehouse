import request from '@/utils/request'

export function getRoleList(params: any) {
  return request({ url: '/role', method: 'get', params })
}

export function createRole(data: any) {
  return request({ url: '/role', method: 'post', data })
}

export function updateRole(id: number | string, data: any) {
  return request({ url: `/role/${id}`, method: 'put', data })
}

export function deleteRole(id: number | string) {
  return request({ url: `/role/${id}`, method: 'delete' })
}

export function getRoleById(id: number | string) {
  return request({ url: `/role/${id}`, method: 'get' })
}

export function assignPermissions(roleId: number | string, permissionIds: number[] | string[]) {
  return request({ url: `/role/${roleId}/permissions`, method: 'put', data: { permissionIds } })
}
