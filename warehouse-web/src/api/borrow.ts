import request from '@/utils/request'

export function getBorrowList(params: any) {
  return request({ url: '/borrow', method: 'get', params })
}

export function getBorrowById(id: number | string) {
  return request({ url: `/borrow/${id}`, method: 'get' })
}

export function createBorrow(data: any) {
  return request({ url: '/borrow', method: 'post', data })
}

export function returnBorrow(id: number | string, data: any) {
  return request({ url: `/borrow/${id}/return`, method: 'put', data })
}
