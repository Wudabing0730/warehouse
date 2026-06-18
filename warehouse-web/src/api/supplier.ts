import request from '@/utils/request'

export function getSupplierList(params: any) {
  return request({ url: '/suppliers', method: 'get', params })
}

export function createSupplier(data: any) {
  return request({ url: '/suppliers', method: 'post', data })
}

export function updateSupplier(data: any) {
  return request({ url: `/suppliers/${data.id}`, method: 'put', data })
}

export function deleteSupplier(id: number | string) {
  return request({ url: `/suppliers/${id}`, method: 'delete' })
}
