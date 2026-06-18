/**
 * P0-8 修复验证:DashboardView 必须调用真实 API,不能继续使用硬编码假数据。
 *
 * Bug 复现:之前 stats 初始化为写死的 156/8420/12/8,
 *         recentOps 是 5 条写死的对象,alerts 永远为空。
 *
 * 修复:stats 初始化为 0,onMounted 时调用 getDashboardSummary() 拉取真实数据。
 *
 * 测试方法:读取 DashboardView.vue 源文件,断言关键修复模式都已就位。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const SOURCE = readFileSync(
  resolve(__dirname, '../../views/dashboard/DashboardView.vue'),
  'utf-8'
)

describe('P0-8 DashboardView must call real API, no more hardcoded fake data', () => {
  it('must import getDashboardSummary from dashboard API client', () => {
    expect(SOURCE).toMatch(/import.*getDashboardSummary.*from\s+['"]@\/api\/dashboard['"]/)
  })

  it('must NOT contain hardcoded fake numbers (156 / 8420 / 12 / 8 / 11 / 13.15)', () => {
    // 这些数字原本写死在 reactive 初始化里
    expect(SOURCE).not.toMatch(/productCount:\s*156/)
    expect(SOURCE).not.toMatch(/totalStock:\s*8420/)
    expect(SOURCE).not.toMatch(/todayInbound:\s*12/)
    expect(SOURCE).not.toMatch(/todayOutbound:\s*8/)
  })

  it('must NOT contain hardcoded recent operation samples', () => {
    expect(SOURCE).not.toContain("'入库 - 电子元器件 (批次: B20260618-001)'")
    expect(SOURCE).not.toContain("'出库 - 五金配件")
    expect(SOURCE).not.toContain("'盘点 - A仓货架A-01'")
  })

  it('stats must initialize to 0 (so UI shows 0 before API loads, not stale fake data)', () => {
    expect(SOURCE).toMatch(/productCount:\s*0/)
    expect(SOURCE).toMatch(/totalStock:\s*0/)
    expect(SOURCE).toMatch(/todayInbound:\s*0/)
    expect(SOURCE).toMatch(/todayOutbound:\s*0/)
  })

  it('must define fetchDashboardData() async function that calls getDashboardSummary', () => {
    expect(SOURCE).toMatch(/async\s+function\s+fetchDashboardData/)
    expect(SOURCE).toContain('await getDashboardSummary()')
  })

  it('must call fetchDashboardData() in onMounted', () => {
    expect(SOURCE).toMatch(/onMounted\(\(\)\s*=>\s*\{[\s\S]*fetchDashboardData\(\)/)
  })

  it('dashboard API client must call backend GET /api/v1/dashboard/summary', () => {
    const apiSource = readFileSync(
      resolve(__dirname, '../../api/dashboard.ts'),
      'utf-8'
    )
    expect(apiSource).toContain("/dashboard/summary")
    expect(apiSource).toContain("method: 'get'")
  })
})

describe('P0-8 DashboardController endpoint exists (backend contract)', () => {
  it('DashboardController exposes /api/v1/dashboard/summary', () => {
    const controller = readFileSync(
      resolve(
        __dirname,
        '../../../../warehouse-server/src/main/java/com/warehouse/controller/DashboardController.java'
      ),
      'utf-8'
    )
    expect(controller).toContain('"/api/v1/dashboard"')
    expect(controller).toContain('"/summary"')
    expect(controller).toContain('@GetMapping')
  })
})