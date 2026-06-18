import request from '@/utils/request'

export function getStockList(params: any) {
  return request({ url: '/stock', method: 'get', params })
}

export function initStock(data: any) {
  return request({ url: '/stock/init', method: 'post', data })
}

export function getStockAlerts() {
  return request({ url: '/stock/alerts', method: 'get' })
}
