import request from '@/utils/request'

export function getPermissionTree() {
  return request({ url: '/permission/tree', method: 'get' })
}
