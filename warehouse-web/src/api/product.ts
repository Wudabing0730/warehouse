import request from '@/utils/request'

export function getProductList(params: any) {
  return request({ url: '/product', method: 'get', params })
}

export function createProduct(data: any) {
  return request({ url: '/product', method: 'post', data })
}

export function updateProduct(id: number | string, data: any) {
  return request({ url: `/product/${id}`, method: 'put', data })
}

export function deleteProduct(id: number | string) {
  return request({ url: `/product/${id}`, method: 'delete' })
}

export function getProductById(id: number | string) {
  return request({ url: `/product/${id}`, method: 'get' })
}
