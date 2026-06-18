/**
 * P1-3 入库/出库明细 VO 的 locationName/locationCode 必须填充
 *
 * Bug 复现:
 *   convertDetailToVO 只查 product,没查 location,导致入库/出库详情行库位列只有 ID
 *
 * 修复:
 *   - InboundServiceImpl / OutboundServiceImpl 注入 WarehouseLocationMapper
 *   - convertDetailToVO 中按 detail.locationId 查 location_code/location_name 写入 VO
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const IMPL = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/service/impl'
)
const VO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response'
)

describe('P1-3 InboundOrderDetailVO 必须有 locationName 字段(契约)', () => {
  it('InboundOrderDetailVO 包含 locationCode + locationName', () => {
    const src = readFileSync(resolve(VO, 'InboundOrderDetailVO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+locationCode/)
    expect(src).toMatch(/private\s+String\s+locationName/)
  })

  it('InboundServiceImpl.convertDetailToVO 必须根据 locationId 查 location_code/location_name', () => {
    const src = readFileSync(resolve(IMPL, 'InboundServiceImpl.java'), 'utf-8')
    // 必须注入 WarehouseLocationMapper
    expect(src).toMatch(/import\s+com\.warehouse\.mapper\.WarehouseLocationMapper/)
    expect(src).toMatch(/@Resource|@Autowired[\s\S]*?WarehouseLocationMapper/)
    // convertDetailToVO 里必须有 setLocationCode / setLocationName
    expect(src).toContain('vo.setLocationCode(')
    expect(src).toContain('vo.setLocationName(')
  })
})

describe('P1-3 OutboundOrderDetailVO 必须有 locationName 字段(契约)', () => {
  it('OutboundOrderDetailVO 包含 locationCode + locationName', () => {
    const src = readFileSync(resolve(VO, 'OutboundOrderDetailVO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+locationCode/)
    expect(src).toMatch(/private\s+String\s+locationName/)
  })

  it('OutboundServiceImpl.convertDetailToVO 必须根据 locationId 查 location_code/location_name', () => {
    const src = readFileSync(resolve(IMPL, 'OutboundServiceImpl.java'), 'utf-8')
    expect(src).toMatch(/import\s+com\.warehouse\.mapper\.WarehouseLocationMapper/)
    expect(src).toMatch(/@Resource|@Autowired[\s\S]*?WarehouseLocationMapper/)
    expect(src).toContain('vo.setLocationCode(')
    expect(src).toContain('vo.setLocationName(')
  })
})

describe('P1-3 前端入库/出库详情表格展示 locationName', () => {
  it('InboundQueryView 详情对话框必须展示 locationName', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/inbound/InboundQueryView.vue'),
      'utf-8'
    )
    expect(src).toContain('locationName')
  })

  it('OutboundQueryView 详情对话框必须展示 locationName', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/outbound/OutboundQueryView.vue'),
      'utf-8'
    )
    expect(src).toContain('locationName')
  })
})