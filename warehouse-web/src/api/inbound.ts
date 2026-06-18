import request from '@/utils/request'

export function getInboundList(params: any) {
  return request({ url: '/inbound-orders', method: 'get', params })
}

export function getInboundById(id: number | string) {
  return request({ url: `/inbound-orders/${id}`, method: 'get' })
}

export function createInbound(data: any) {
  return request({ url: '/inbound-orders', method: 'post', data })
}

export function confirmInbound(id: number | string, data: any) {
  return request({ url: `/inbound-orders/${id}/confirm`, method: 'put', data })
}
