import request from '@/utils/request'

export function getOutboundList(params: any) {
  return request({ url: '/outbound-orders', method: 'get', params })
}

export function getOutboundById(id: number | string) {
  return request({ url: `/outbound-orders/${id}`, method: 'get' })
}

export function createOutbound(data: any) {
  return request({ url: '/outbound-orders', method: 'post', data })
}

export function confirmOutbound(id: number | string, data: any) {
  return request({ url: `/outbound-orders/${id}/confirm`, method: 'put', data })
}
