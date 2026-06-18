import request from '@/utils/request'

export function getInboundReport(params: any) {
  return request({ url: '/reports/inbound', method: 'get', params })
}

export function getOutboundReport(params: any) {
  return request({ url: '/reports/outbound', method: 'get', params })
}

export function getStockReport(params: any) {
  return request({ url: '/reports/stock', method: 'get', params })
}

export function getComprehensiveReport(params: any) {
  return request({ url: '/reports/comprehensive', method: 'get', params })
}
