import request from '@/utils/request'

export function getLocationList(params: any) {
  return request({ url: '/location', method: 'get', params })
}

export function createLocation(data: any) {
  return request({ url: '/location', method: 'post', data })
}

export function updateLocation(id: number | string, data: any) {
  return request({ url: `/location/${id}`, method: 'put', data })
}

export function deleteLocation(id: number | string) {
  return request({ url: `/location/${id}`, method: 'delete' })
}
