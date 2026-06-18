import request from '@/utils/request'

export function getProductList(params: any) {
  return request({ url: '/products', method: 'get', params })
}

export function createProduct(data: any) {
  return request({ url: '/products', method: 'post', data })
}

export function updateProduct(data: any) {
  return request({ url: `/products/${data.id}`, method: 'put', data })
}

export function deleteProduct(id: number | string) {
  return request({ url: `/products/${id}`, method: 'delete' })
}

export function getProductById(id: number | string) {
  return request({ url: `/products/${id}`, method: 'get' })
}
