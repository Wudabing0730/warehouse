import request from '@/utils/request'

export function getCheckList(params: any) {
  return request({ url: '/inventory/check', method: 'get', params })
}

export function getCheckById(id: number | string) {
  return request({ url: `/inventory/check/${id}`, method: 'get' })
}

export function createCheck(data: any) {
  return request({ url: '/inventory/check', method: 'post', data })
}

export function confirmCheck(id: number | string, data: any) {
  return request({ url: `/inventory/check/${id}/confirm`, method: 'put', data })
}
