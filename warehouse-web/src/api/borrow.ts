import request from '@/utils/request'

export function getBorrowList(params: any) {
  return request({ url: '/borrow-records', method: 'get', params })
}

export function getBorrowById(id: number | string) {
  return request({ url: `/borrow-records/${id}`, method: 'get' })
}

export function createBorrow(data: any) {
  return request({ url: '/borrow-records', method: 'post', data })
}

export function returnBorrow(id: number | string, data: any) {
  return request({ url: `/borrow-records/${id}/return`, method: 'put', data })
}
