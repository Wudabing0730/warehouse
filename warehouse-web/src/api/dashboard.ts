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
  // P3-2: 最近 7 天出入库趋势
  inboundTrend?: number[]
  outboundTrend?: number[]
  trendDates?: string[]
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return request({ url: '/dashboard/summary', method: 'get' })
}