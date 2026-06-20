/**
 * P3-3 出库管理"提交出库单"功能异常 — 前端契约测试
 *
 * 复现 Bug:
 *   - OutboundFormView.vue 的 payload.details 不含 locationId,
 *     导致后端 t_outbound_order_detail.location_id 落库 NULL,
 *     confirm 阶段只能用 product.defaultLocationId 兜底,业务"指定库位"语义错误。
 *   - 修复后:前端表单必须含"目标库位"列、DetailRow 必须有 locationId、
 *     handleSubmit payload 必须带 locationId、validateDetails 必须校验 locationId 必填。
 *
 * vitest 的 expect().toContain/.toMatch 仅接受 1 个参数(TypeScript 类型限制),
 * 失败原因统一放到 assertion 后的独立 expect 调用,或合并到消息字符串里。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const VIEW = resolve(__dirname, '../../views/outbound/OutboundFormView.vue')

describe('P3-3 OutboundFormView 必须提供"目标库位"列(契约)', () => {
  it('模板必须包含 label="目标库位" 的 el-table-column', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/label\s*=\s*["']目标库位["']/)
  })

  it('模板必须包含 v-model="row.locationId" 的库位下拉', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toContain('row.locationId')
  })
})

describe('P3-3 OutboundFormView DetailRow 必须含 locationId 字段', () => {
  it('DetailRow interface 必须声明 locationId', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/interface\s+DetailRow[\s\S]*?locationId\s*:\s*number\s*\|\s*null/)
  })

  it('createEmptyDetail 必须初始化 locationId: null', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/function\s+createEmptyDetail[\s\S]*?locationId\s*:\s*null/)
  })
})

describe('P3-3 OutboundFormView 必须加载 locationList', () => {
  it('必须 import getLocationList', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/import\s*\{[^}]*getLocationList[^}]*\}\s*from\s*['"]@\/api\/location['"]/)
  })

  it('必须定义 locationList ref', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/const\s+locationList\s*=\s*ref/)
  })

  it('onMounted 必须 await loadLocations()', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toContain('loadLocations')
  })
})

describe('P3-3 OutboundFormView handleSubmit payload 必须含 locationId', () => {
  it('payload.details.map 中必须有 locationId: d.locationId', () => {
    const src = readFileSync(VIEW, 'utf-8')
    // 在 details.map 块内必须出现 locationId 字段(防止 Bug 复发)
    const mapBlockMatch = src.match(/details\s*:\s*formData\.details\.map\([\s\S]*?\)\s*,?\s*\}/)
    expect(mapBlockMatch).not.toBeNull()
    expect(mapBlockMatch?.[0]).toContain('locationId: d.locationId')
  })
})

describe('P3-3 OutboundFormView validateDetails 必须校验 locationId 必填', () => {
  it('validateDetails 中必须有"目标库位"提示', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/请选择目标库位/)
  })
})

describe('P3-3 前端 catch 必须透出后端错误信息(全 3 个表单页统一)', () => {
  it('OutboundFormView.vue catch 必须用 ElMessage.error 透出', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/catch\s*\(e:\s*any\)[\s\S]*?ElMessage\.error/)
  })

  it('InboundFormView.vue catch 必须用 ElMessage.error 透出', () => {
    const view = resolve(__dirname, '../../views/inbound/InboundFormView.vue')
    const src = readFileSync(view, 'utf-8')
    expect(src).toMatch(/catch\s*\(e:\s*any\)[\s\S]*?ElMessage\.error/)
  })

  it('LocationView.vue catch 必须用 ElMessage.error 透出', () => {
    const view = resolve(__dirname, '../../views/base/LocationView.vue')
    const src = readFileSync(view, 'utf-8')
    expect(src).toMatch(/catch\s*\(e:\s*any\)[\s\S]*?ElMessage\.error/)
  })
})
