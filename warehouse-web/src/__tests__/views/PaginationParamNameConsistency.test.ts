/**
 * P1-1 全局分页参数名一致性测试
 *
 * Bug 复现:
 *   后端所有 Controller.list 接 `Integer page, Integer size`(默认 1/10)
 *   前端 25 个 Vue 文件大量使用 pageNum/pageSize 发请求
 *   => 切换 "每页 50 条" 时,后端始终返回 10 条(默认 size=10 生效)
 *   => 翻到第 5 页时,后端按 pageNum=5 / size=10 返回第 41-50 条,与前端期望不符
 *
 * 修复: 前端所有列表 fetchList 改为 params: { page: ..., size: ... }
 *       pagination 状态字段也改名为 page/size(全栈统一)
 *       已存在的 warehouse-server 后端契约不变
 *
 * 测试方法: 读取所有列表视图,断言 fetchList 的 params 用 'page' / 'size',
 *          且 el-pagination v-model 也用 page/size。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { resolve, join } from 'path'

// 收集所有需要分页的视图(包含 pagination 关键字的 *.vue)
const VIEWS_DIR = resolve(__dirname, '../../views')

function collectVueFiles(dir: string): string[] {
  const out: string[] = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    const st = statSync(full)
    if (st.isDirectory()) out.push(...collectVueFiles(full))
    else if (entry.endsWith('.vue')) out.push(full)
  }
  return out
}

const allViews = collectVueFiles(VIEWS_DIR)
const listViews = allViews.filter((f) => {
  const s = readFileSync(f, 'utf-8')
  return (s.includes('pagination') || s.includes('fetchList')) &&
    (s.includes('el-pagination') || s.includes('el-table'))
})

describe('P1-1 前端分页参数必须使用 page/size 与后端契约对齐', () => {
  it('至少要扫描到列表视图(防止空目录)', () => {
    expect(listViews.length).toBeGreaterThan(5)
  })

  for (const file of listViews) {
    const rel = file.replace(VIEWS_DIR, '').replace(/\\/g, '/')

    it(`${rel}: params 对象中不允许出现 pageNum/pageSize/pageNo`, () => {
      const src = readFileSync(file, 'utf-8')
      // 找 params 对象(包括 params: Record<...> = { ... })
      const m = src.match(/params\s*(?::\s*[^=]+)?\s*=\s*\{([\s\S]*?)\}/)
      if (!m) return // 视图无 params,跳过
      const body = m[1]
      expect(body, `${rel} 不应再使用 pageNum/pageSize/pageNo,后端契约是 page/size`)
        .not.toMatch(/pageNum|pageSize|pageNo/)
    })
  }

  it('SupplierView 必须使用 page/size', () => {
    const src = readFileSync(resolve(VIEWS_DIR, 'base/SupplierView.vue'), 'utf-8')
    expect(src).toMatch(/page:\s*pagination\.page/)
    expect(src).toMatch(/size:\s*pagination\.size/)
    expect(src).not.toMatch(/pageNum:\s*pagination\.pageNum/)
    expect(src).not.toMatch(/pageSize:\s*pagination\.pageSize/)
  })

  it('CustomerView 必须使用 page/size', () => {
    const src = readFileSync(resolve(VIEWS_DIR, 'base/CustomerView.vue'), 'utf-8')
    expect(src).toMatch(/page:\s*pagination\.page/)
    expect(src).toMatch(/size:\s*pagination\.size/)
    expect(src).not.toMatch(/pageNum:\s*pagination\.pageNum/)
    expect(src).not.toMatch(/pageSize:\s*pagination\.pageSize/)
  })

  it('UserView 必须使用 page/size', () => {
    const src = readFileSync(resolve(VIEWS_DIR, 'system/UserView.vue'), 'utf-8')
    expect(src).toMatch(/page:\s*pagination\.page/)
    expect(src).toMatch(/size:\s*pagination\.size/)
    expect(src).not.toMatch(/pageNum:\s*pagination\.pageNum/)
    expect(src).not.toMatch(/pageSize:\s*pagination\.pageSize/)
  })

  it('LocationView 必须使用 page/size', () => {
    const src = readFileSync(resolve(VIEWS_DIR, 'base/LocationView.vue'), 'utf-8')
    expect(src).toMatch(/page:\s*pagination\.page/)
    expect(src).toMatch(/size:\s*pagination\.size/)
    expect(src).not.toMatch(/pageNum:\s*pagination\.pageNum/)
    expect(src).not.toMatch(/pageSize:\s*pagination\.pageSize/)
  })

  it('el-pagination v-model 必须绑定 page/size 字段(以 SupplierView 为代表)', () => {
    const src = readFileSync(resolve(VIEWS_DIR, 'base/SupplierView.vue'), 'utf-8')
    expect(src).toMatch(/v-model:current-page="pagination\.page"/)
    expect(src).toMatch(/v-model:page-size="pagination\.size"/)
  })
})

describe('P1-1 后端 Controller 契约统一为 page/size(回归保护)', () => {
  it('所有 Controller.list 必须接 page/size', () => {
    const controllers = [
      'SupplierController.java', 'CustomerController.java',
      'UserController.java', 'RoleController.java',
      'BorrowController.java', 'InboundController.java', 'OutboundController.java',
      'ProductController.java', 'WarehouseLocationController.java',
      'InventoryCheckController.java', 'StockController.java',
      // OperationLogController 在 P1-7 中单独处理
    ]
    const ctrlDir = resolve(
      __dirname,
      '../../../../warehouse-server/src/main/java/com/warehouse/controller'
    )
    for (const ctrl of controllers) {
      const src = readFileSync(resolve(ctrlDir, ctrl), 'utf-8')
      expect(src, `${ctrl} 仍然接 page/size`).toMatch(/Integer\s+page/)
      expect(src, `${ctrl} 仍然接 page/size`).toMatch(/Integer\s+size/)
    }
  })
})