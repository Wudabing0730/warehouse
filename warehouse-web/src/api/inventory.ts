import request from '@/utils/request'

export function getCheckList(params: any) {
  return request({ url: '/inventory-checks', method: 'get', params })
}

export function getCheckById(id: number | string) {
  return request({ url: `/inventory-checks/${id}`, method: 'get' })
}

export function createCheck(data: any) {
  return request({ url: '/inventory-checks', method: 'post', data })
}

export function confirmCheck(id: number | string, data: any) {
  return request({ url: `/inventory-checks/${id}/confirm`, method: 'put', data })
}
