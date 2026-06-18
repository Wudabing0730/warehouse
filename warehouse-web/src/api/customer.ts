import request from '@/utils/request'

export function getCustomerList(params: any) {
  return request({ url: '/customer', method: 'get', params })
}

export function createCustomer(data: any) {
  return request({ url: '/customer', method: 'post', data })
}

export function updateCustomer(id: number | string, data: any) {
  return request({ url: `/customer/${id}`, method: 'put', data })
}

export function deleteCustomer(id: number | string) {
  return request({ url: `/customer/${id}`, method: 'delete' })
}
