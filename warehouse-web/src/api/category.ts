import request from '@/utils/request'

export function getCategoryTree() {
  return request({ url: '/categories', method: 'get' })
}

export function createCategory(data: any) {
  return request({ url: '/categories', method: 'post', data })
}

export function updateCategory(data: any) {
  return request({ url: `/categories/${data.id}`, method: 'put', data })
}

export function deleteCategory(id: number | string) {
  return request({ url: `/categories/${id}`, method: 'delete' })
}
