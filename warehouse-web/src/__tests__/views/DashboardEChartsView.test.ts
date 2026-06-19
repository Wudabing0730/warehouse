/**
 * P3-2 仪表盘 ECharts 可视化修复验证
 *
 * Bug 复现:原 DashboardView 只有 4 个 stat card + alerts + recent ops,
 *         没有任何图表,数据展示不直观。
 *
 * 修复:用 ECharts 渲染 7 天出入库趋势折线图,
 *      DashboardSummaryVO 扩展 inboundTrend / outboundTrend / trendDates 3 个字段。
 *
 * 测试方法:读取 DashboardView.vue 源文件,断言图表相关元素已就位
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const DASH = readFileSync(
  resolve(__dirname, '../../views/dashboard/DashboardView.vue'),
  'utf-8'
)
const API = readFileSync(
  resolve(__dirname, '../../api/dashboard.ts'),
  'utf-8'
)

describe('P3-2 DashboardView 必须使用 ECharts 渲染图表', () => {
  it('必须 import echarts 库', () => {
    expect(DASH).toMatch(/import\s+(?:\*\s+as\s+)?echarts\s+from\s+['"]echarts['"]/)
  })

  it('必须含 v-chart / chart / ref="chart" 元素用于挂载 ECharts', () => {
    // 允许的几种图表挂载模式:v-chart 组件 / div ref="chartRef" + init
    const hasVChart = /v-chart/.test(DASH)
    const hasRefChart = /ref="chart[A-Za-z]*"/.test(DASH)
    const hasInit = /echarts\.init\(/.test(DASH)
    expect(hasVChart || hasRefChart || hasInit).toBe(true)
  })

  it('必须含 ECharts 折线图 series 配置(出入库趋势)', () => {
    // 折线图通常含 type: 'line', 或者 series 内含 'line'
    expect(DASH).toContain("type: 'line'")
  })

  it('必须含 ECharts option 配置(通过 buildChartOption 函数或 option 变量)', () => {
    // ECharts 配置可以放在 buildChartOption 函数返回 / option 变量 / 模板字符串中等
    const hasBuilder = /buildChartOption|buildOption|chartOption/.test(DASH)
    const hasInline = /option\s*[:=]/.test(DASH)
    expect(hasBuilder || hasInline).toBe(true)
  })

  it('渲染逻辑必须调用 setOption 提交配置', () => {
    expect(DASH).toContain('setOption')
  })
})

describe('P3-2 Dashboard API 必须含 7 天趋势数据', () => {
  it('DashboardSummary 接口必须含 inboundTrend / outboundTrend / trendDates', () => {
    expect(API).toContain('inboundTrend')
    expect(API).toContain('outboundTrend')
    expect(API).toContain('trendDates')
  })
})
