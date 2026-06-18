import request from '@/utils/request'

export function getLogList(params: any) {
  return request({ url: '/logs', method: 'get', params })
}
