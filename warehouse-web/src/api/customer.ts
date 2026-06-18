import request from '@/utils/request'

export function getCustomerList(params: any) {
  return request({ url: '/customers', method: 'get', params })
}

export function createCustomer(data: any) {
  return request({ url: '/customers', method: 'post', data })
}

export function updateCustomer(data: any) {
  return request({ url: `/customers/${data.id}`, method: 'put', data })
}

export function deleteCustomer(id: number | string) {
  return request({ url: `/customers/${id}`, method: 'delete' })
}
