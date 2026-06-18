import request from '@/utils/request'

export function getInboundList(params: any) {
  return request({ url: '/inbound', method: 'get', params })
}

export function getInboundById(id: number | string) {
  return request({ url: `/inbound/${id}`, method: 'get' })
}

export function createInbound(data: any) {
  return request({ url: '/inbound', method: 'post', data })
}

export function confirmInbound(id: number | string, data: any) {
  return request({ url: `/inbound/${id}/confirm`, method: 'put', data })
}
