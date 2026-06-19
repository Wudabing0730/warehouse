/**
 * Bug 复现验证:DashboardView fetchDashboardData 必须有显式错误反馈
 *
 * Bug 复现:
 *   用户截图显示 dashboard 所有 stat 卡片为 0 + ECharts 图表完全空白(只有 legend 和一条横线)。
 *   代码 DashboardView.vue 第 198-200 行:
 *     } catch {
 *       // 错误由全局 axios 拦截器统一处理,这里保持零值
 *     }
 *   是**完全空的 catch**,无 console.error、无 ElMessage、无任何用户反馈。
 *
 * 后果:无论 API 失败原因是 401(token 过期)、500(后端 NPE)、Redis 不可用、网络异常,
 *      前端表现完全一致(全 0 + 图表空白),用户/开发者完全无法判断失败原因。
 *
 * 修复:DashboardView.vue 的 catch 块必须:
 *   1. 调用 ElMessage.error(或 ElNotification.error)给用户错误提示
 *   2. 调用 console.error(或 console.warn)留给开发者排查
 *   3. 必须 import ElMessage 从 'element-plus'
 *
 * 测试方法:读取 DashboardView.vue 源文件,正则断言 catch 块包含上述要素。
 *         当前文件会失败(因为是空 catch),证明 bug 真实存在。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const DASH = readFileSync(
  resolve(__dirname, '../../views/dashboard/DashboardView.vue'),
  'utf-8'
)

describe('DashboardView 必须 import ElMessage(用于错误反馈)', () => {
  it('必须从 element-plus 导入 ElMessage 或 ElNotification', () => {
    // 允许 ElMessage / ElNotification 两种形式
    const hasElMessageImport = /import\s+\{[^}]*\bElMessage\b[^}]*\}\s+from\s+['"]element-plus['"]/.test(DASH)
    const hasElNotificationImport = /import\s+\{[^}]*\bElNotification\b[^}]*\}\s+from\s+['"]element-plus['"]/.test(DASH)
    expect(hasElMessageImport || hasElNotificationImport).toBe(true)
  })
})

describe('DashboardView fetchDashboardData 的 catch 块必须有显式错误处理', () => {
  /**
   * 提取 catch 块内容
   * 匹配 fetchDashboardData 函数内的 } catch { ... } 区段
   */
  function extractCatchBlock(): string {
    // 匹配 fetchDashboardData 函数体里的 catch 块(支持 catch (e) {} 或 catch {} 两种)
    const re = /async\s+function\s+fetchDashboardData[\s\S]*?}\s*catch\s*(?:\([^)]*\))?\s*\{([\s\S]*?)\n\s*\}\s*\n/
    const m = DASH.match(re)
    return m ? m[1] : ''
  }

  it('fetchDashboardData 函数内必须存在 catch 块(防止被静默吞错)', () => {
    const block = extractCatchBlock()
    expect(block.length).toBeGreaterThan(0)
  })

  it('catch 块必须包含 console.error 或 console.warn(开发者排错)', () => {
    const block = extractCatchBlock()
    expect(block).toMatch(/console\.(error|warn)/)
  })

  it('catch 块必须调用 ElMessage.error 或 ElNotification.error(用户反馈)', () => {
    const block = extractCatchBlock()
    expect(block).toMatch(/ElMessage\.error|ElNotification\.error/)
  })

  it('catch 块不能是空的(不能是 } catch {} 什么都不做)', () => {
    const block = extractCatchBlock().trim()
    expect(block).not.toBe('')
    // 也不能只是注释
    const codeOnly = block.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '').trim()
    expect(codeOnly.length).toBeGreaterThan(0)
  })
})

describe('DashboardView fetchDashboardData 调用 API 时不能继续静默', () => {
  it('源文件必须不能再保留"保持零值"这类注释(暗示静默吞错)', () => {
    // 这是 bug 的"症状":代码注释里直接承认"保持零值",等同于承认吞错
    // 修复后,正确的注释应该是"提示用户 + console.error"
    expect(DASH).not.toMatch(/保持零值|保持\s*零\s*值/)
  })
})