import request from '@/utils/request'

export interface RecentOperation {
  time: string
  description: string
  user: string
}

export interface DashboardSummary {
  productCount: number
  totalStock: number
  todayInbound: number
  todayOutbound: number
  alerts: string[]
  recentOps: RecentOperation[]
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return request({ url: '/dashboard/summary', method: 'get' })
}