import request from '@/utils/request'

export function getStockList(params: any) {
  return request({ url: '/stocks', method: 'get', params })
}

export function initStock(data: any) {
  return request({ url: '/stocks/init', method: 'post', data })
}

export function getStockAlerts() {
  return request({ url: '/stocks/alerts', method: 'get' })
}
