import request from '@/utils/request'

export function getPermissionTree() {
  return request({ url: '/permissions', method: 'get' })
}
