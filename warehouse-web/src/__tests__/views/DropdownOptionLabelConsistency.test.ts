/**
 * P2-1 下拉框 label 字段错配修复验证
 *
 * Bug 复现:
 *   - 后端 VO 字段命名是 supplierName/customerName/productName/locationName/productCode
 *   - 前端 el-option :label 大量使用 s.name / c.name / p.name / loc.name / p.code
 *   - 渲染时下拉框全部为空白(原字段为 undefined)
 *
 * 影响范围(共 16 处):
 *   - 入库表单/查询/审核 — supplierList 用 s.name
 *   - 出库表单/查询/审核 — customerList 用 c.name
 *   - 借出/归还/查询    — productList 用 p.name
 *   - 盘点任务/录入/结果/库存报表 — productOptions 用 p.name / p.code
 *   - 入库/库存初始化  — locationList 用 loc.name
 *
 * 修复:
 *   - 把所有 :label 引用统一改为 VO 实际字段名
 *   - supplier: s.name → s.supplierName
 *   - customer: c.name → c.customerName
 *   - product : p.name → p.productName, p.code → p.productCode
 *   - location: loc.name → loc.locationName
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const VIEWS = resolve(__dirname, '../../views')

function readView(rel: string): string {
  return readFileSync(resolve(VIEWS, rel), 'utf-8')
}

describe('P2-1 入库模块下拉框 label 字段必须用 supplierName/productName/locationName', () => {
  it('InboundFormView supplierList 必须用 s.supplierName', () => {
    const src = readView('inbound/InboundFormView.vue')
    expect(src).toMatch(/el-option[\s\S]*?v-for="s in supplierList"[\s\S]*?:label="s\.supplierName"/)
  })

  it('InboundFormView productList 必须用 p.productName', () => {
    const src = readView('inbound/InboundFormView.vue')
    expect(src).toMatch(/el-option[\s\S]*?v-for="p in productList"[\s\S]*?:label="p\.productName"/)
  })

  it('InboundFormView locationList 必须用 loc.locationName', () => {
    const src = readView('inbound/InboundFormView.vue')
    expect(src).toMatch(/el-option[\s\S]*?v-for="loc in locationList"[\s\S]*?:label="loc\.locationName"/)
  })

  it('InboundQueryView supplierList 必须用 s.supplierName', () => {
    const src = readView('inbound/InboundQueryView.vue')
    expect(src).toContain('s.supplierName')
  })

  it('InboundAuditView supplierList 必须用 s.supplierName', () => {
    const src = readView('inbound/InboundAuditView.vue')
    expect(src).toContain('s.supplierName')
  })
})

describe('P2-1 出库模块下拉框 label 字段必须用 customerName/productName', () => {
  it('OutboundFormView customerList 必须用 c.customerName', () => {
    const src = readView('outbound/OutboundFormView.vue')
    expect(src).toMatch(/el-option[\s\S]*?v-for="c in customerList"[\s\S]*?:label="c\.customerName"/)
  })

  it('OutboundFormView productList 必须用 p.productName', () => {
    const src = readView('outbound/OutboundFormView.vue')
    expect(src).toMatch(/el-option[\s\S]*?v-for="p in productList"[\s\S]*?:label="p\.productName"/)
  })

  it('OutboundQueryView customerList 必须用 c.customerName', () => {
    const src = readView('outbound/OutboundQueryView.vue')
    expect(src).toContain('c.customerName')
  })

  it('OutboundAuditView customerList 必须用 c.customerName', () => {
    const src = readView('outbound/OutboundAuditView.vue')
    expect(src).toContain('c.customerName')
  })
})

describe('P2-1 借还模块下拉框 label 字段必须用 productName', () => {
  it('BorrowFormView productList 必须用 p.productName', () => {
    const src = readView('borrow/BorrowFormView.vue')
    expect(src).toContain('p.productName')
  })

  it('BorrowQueryView productList 必须用 p.productName', () => {
    const src = readView('borrow/BorrowQueryView.vue')
    expect(src).toContain('p.productName')
  })

  it('ReturnFormView productList 必须用 p.productName', () => {
    const src = readView('borrow/ReturnFormView.vue')
    expect(src).toContain('p.productName')
  })
})

describe('P2-1 盘点/报表模块下拉框 label 字段必须用 productName/productCode', () => {
  it('CheckTaskView productOptions 必须用 p.productName + p.productCode', () => {
    const src = readView('inventory/CheckTaskView.vue')
    expect(src).toContain('p.productName')
    expect(src).toContain('p.productCode')
  })

  it('CheckInputView productOptions 必须用 p.productName + p.productCode', () => {
    const src = readView('inventory/CheckInputView.vue')
    expect(src).toContain('p.productName')
    expect(src).toContain('p.productCode')
  })

  it('CheckResultView productOptions 必须用 p.productName + p.productCode', () => {
    const src = readView('inventory/CheckResultView.vue')
    expect(src).toContain('p.productName')
    expect(src).toContain('p.productCode')
  })

  it('StockReportView productOptions 必须用 p.productName + p.productCode', () => {
    const src = readView('report/StockReportView.vue')
    expect(src).toContain('p.productName')
    expect(src).toContain('p.productCode')
  })
})

describe('P2-1 反向断言:这些错误字段名一个都不应再出现', () => {
  const viewFiles = [
    'inbound/InboundFormView.vue',
    'inbound/InboundQueryView.vue',
    'inbound/InboundAuditView.vue',
    'outbound/OutboundFormView.vue',
    'outbound/OutboundQueryView.vue',
    'outbound/OutboundAuditView.vue',
    'borrow/BorrowFormView.vue',
    'borrow/BorrowQueryView.vue',
    'borrow/ReturnFormView.vue',
    'inventory/CheckTaskView.vue',
    'inventory/CheckInputView.vue',
    'inventory/CheckResultView.vue',
    'report/StockReportView.vue',
  ]

  it.each(viewFiles)('%s 不应再使用 :label="s.name" / c.name / p.name / loc.name / p.code 字段', (rel) => {
    const src = readView(rel)
    // 出现的几种错误模式(必须都为 0)
    const badPatterns = [
      /:label="s\.name"/, // supplier 错配
      /:label="c\.name"/, // customer 错配
      /:label="p\.name"/, // product 错配
      /:label="loc\.name"/, // location 错配
      /`\$\{p\.name\}/, // 模板字符串里 p.name
      /`\$\{loc\.name\}/, // 模板字符串里 loc.name
    ]
    for (const pat of badPatterns) {
      expect(src).not.toMatch(pat)
    }
  })
})
