/**
 * UX 改进验证:DashboardView 数据库为空时显示空数据提示
 *
 * 场景:用户截图显示 dashboard 4 个统计全为 0 + ECharts 图表空白。
 *      真实原因:用户的 MySQL 没跑 sql/03-demo-data.sql,数据库为空。
 *      修复:在 dashboard 顶部加 el-alert 空数据提示横幅,明确告诉用户
 *           "数据库暂无数据,请运行 sql/03-demo-data.sql 加载演示数据"。
 *
 * 测试方法:读取 DashboardView.vue 源文件,断言:
 *   1. 含 el-alert 或 el-empty 组件(用于空状态)
 *   2. 提示文案明确提到 sql/03-demo-data.sql 或"演示数据"或"暂无数据"
 *   3. 提示只在所有 stat 为 0 时显示(用 v-if 条件)
 *   4. 不影响已有 ECharts 渲染逻辑(只是新增,不删除)
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const DASH = readFileSync(
  resolve(__dirname, '../../views/dashboard/DashboardView.vue'),
  'utf-8'
)

describe('DashboardView 必须有空数据状态提示(数据库为空时)', () => {
  it('必须含 el-alert 或 el-empty 组件用于空状态展示', () => {
    // 允许两种实现方式:el-alert(顶部横幅) 或 el-empty(中央空状态)
    const hasElAlert = /<el-alert\b/.test(DASH)
    const hasElEmpty = /<el-empty\b/.test(DASH)
    expect(hasElAlert || hasElEmpty).toBe(true)
  })

  it('空数据提示文案必须明确提到"演示数据"或"sql/03-demo-data"', () => {
    // 文案应引导用户加载 sql/03-demo-data.sql
    const has03DemoHint = /03-demo-data/.test(DASH)
    const hasDemoHint = /演示数据/.test(DASH)
    const hasSeedHint = /暂无数据|没有数据|数据为空|加载数据/.test(DASH)
    expect(has03DemoHint || hasDemoHint || hasSeedHint).toBe(true)
  })

  it('空数据提示必须有 v-if 控制显示(只在数据为空时显示)', () => {
    // 找到包含 el-alert/el-empty 的 v-if 表达式
    // 模式: v-if="..." 或 v-if='...'
    const alertMatch = DASH.match(/<el-(alert|empty)\b[^>]*v-if=(["'])([^"']+)\2/)
    expect(alertMatch).not.toBeNull()
    // 条件必须能反映数据为空的状态
    if (alertMatch) {
      const cond = alertMatch[3]
      // 条件应包含相关字段,例如 isEmpty / hasNoData / allZero / productCount === 0 等
      const isEmptyCondition =
        /isEmpty|hasNoData|allZero|noData|empty|productCount.*0|productCount.*===\s*0/i.test(cond)
      expect(isEmptyCondition).toBe(true)
    }
  })

  it('提示不能影响已有 ECharts 折线图渲染(只是新增元素)', () => {
    // 修复必须保留 buildChartOption / setOption / chartRef 等关键元素
    expect(DASH).toMatch(/buildChartOption|chartOption/)
    expect(DASH).toContain('setOption')
    expect(DASH).toMatch(/ref="chart[A-Za-z]*"/)
  })

  it('空数据提示应使用 type="warning" 或 type="info" 区别于普通错误', () => {
    // el-alert 的 type 应为 warning(警告)或 info(信息),不是 error(那是 API 失败)
    const hasWarningType = /<el-alert[^>]*type=(["'])(warning|info)\1/.test(DASH)
    const hasEmptyDesc = /<el-empty[^>]*description=/.test(DASH)
    expect(hasWarningType || hasEmptyDesc).toBe(true)
  })
})

describe('DashboardView 必须保留已有的错误处理 catch 块(防御 regression)', () => {
  // 防止后续改动覆盖我们 commit 1f58686 的修复
  it('catch 块必须仍然调用 ElMessage.error', () => {
    expect(DASH).toMatch(/ElMessage\.error|ElNotification\.error/)
  })

  it('catch 块必须仍然有 console.error 排查线索', () => {
    expect(DASH).toMatch(/console\.error|console\.warn/)
  })
})