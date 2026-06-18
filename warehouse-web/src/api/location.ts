import request from '@/utils/request'

export function getLocationList(params: any) {
  return request({ url: '/locations', method: 'get', params })
}

export function createLocation(data: any) {
  return request({ url: '/locations', method: 'post', data })
}

export function updateLocation(data: any) {
  return request({ url: `/locations/${data.id}`, method: 'put', data })
}

export function deleteLocation(id: number | string) {
  return request({ url: `/locations/${id}`, method: 'delete' })
}
