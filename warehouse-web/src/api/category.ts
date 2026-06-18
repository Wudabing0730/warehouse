import request from '@/utils/request'

export function getCategoryTree() {
  return request({ url: '/category/tree', method: 'get' })
}

export function createCategory(data: any) {
  return request({ url: '/category', method: 'post', data })
}

export function updateCategory(id: number | string, data: any) {
  return request({ url: `/category/${id}`, method: 'put', data })
}

export function deleteCategory(id: number | string) {
  return request({ url: `/category/${id}`, method: 'delete' })
}
