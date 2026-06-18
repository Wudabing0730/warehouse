import request from '@/utils/request'

export function getSupplierList(params: any) {
  return request({ url: '/supplier', method: 'get', params })
}

export function createSupplier(data: any) {
  return request({ url: '/supplier', method: 'post', data })
}

export function updateSupplier(id: number | string, data: any) {
  return request({ url: `/supplier/${id}`, method: 'put', data })
}

export function deleteSupplier(id: number | string) {
  return request({ url: `/supplier/${id}`, method: 'delete' })
}
