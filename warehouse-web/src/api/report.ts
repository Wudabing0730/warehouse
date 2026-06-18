import request from '@/utils/request'

export function getInboundReport(params: any) {
  return request({ url: '/report/inbound', method: 'get', params })
}

export function getOutboundReport(params: any) {
  return request({ url: '/report/outbound', method: 'get', params })
}

export function getStockReport(params: any) {
  return request({ url: '/report/stock', method: 'get', params })
}

export function getComprehensiveReport(params: any) {
  return request({ url: '/report/comprehensive', method: 'get', params })
}
