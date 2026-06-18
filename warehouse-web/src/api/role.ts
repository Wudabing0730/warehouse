import request from '@/utils/request'

export function getRoleList(params: any) {
  return request({ url: '/roles', method: 'get', params })
}

export function createRole(data: any) {
  return request({ url: '/roles', method: 'post', data })
}

export function updateRole(data: any) {
  return request({ url: `/roles/${data.id}`, method: 'put', data })
}

export function deleteRole(id: number | string) {
  return request({ url: `/roles/${id}`, method: 'delete' })
}

export function getRoleById(id: number | string) {
  return request({ url: `/roles/${id}`, method: 'get' })
}

export function assignPermissions(data: { roleId: number | string; permissionIds: (number | string)[] }) {
  return request({ url: `/roles/${data.roleId}/permissions`, method: 'put', data: data.permissionIds })
}

export async function getRolePermissions(roleId: number | string) {
  const res: any = await request({ url: `/roles/${roleId}`, method: 'get' })
  return { data: res.data?.permissionIds ?? [] }
}
