/**
 * P2-2 入库/出库审计字段错配修复验证
 *
 * Bug 复现:
 *   - 后端 VO 字段是 confirmOperatorName / confirmTime (语义:审核人/审核时间)
 *   - 前端 InboundQueryView / OutboundQueryView 表格 + 详情对话框使用
 *     row.auditorName / row.auditTime / currentRow.auditorName / currentRow.auditTime
 *   - 因字段名不匹配,所有"审核人"和"审核时间"列都显示 '--'
 *
 * 修复:
 *   - InboundOrderVO 加 @JsonProperty("auditorName") getAuditorName() 返回 confirmOperatorName
 *   - InboundOrderVO 加 @JsonProperty("auditTime") getAuditTime() 返回 confirmTime
 *   - OutboundOrderVO 同样处理
 *
 * 影响范围:4 个 view(进/出 × 查/审),共 8 处属性引用
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const VIEWS = resolve(__dirname, '../../views')
const VO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response'
)

function readView(rel: string): string {
  return readFileSync(resolve(VIEWS, rel), 'utf-8')
}

describe('P2-2 后端契约:InboundOrderVO 必须有 auditorName/auditTime 别名', () => {
  const src = readFileSync(resolve(VO, 'InboundOrderVO.java'), 'utf-8')

  it('必须有 @JsonProperty("auditorName") 返回 confirmOperatorName', () => {
    expect(src).toMatch(
      /@JsonProperty\("auditorName"\)[\s\S]*?public\s+String\s+getAuditorName\(\)[\s\S]*?return\s+confirmOperatorName/
    )
  })

  it('必须有 @JsonProperty("auditTime") 返回 confirmTime', () => {
    expect(src).toMatch(
      /@JsonProperty\("auditTime"\)[\s\S]*?public\s+(?:java\.time\.)?LocalDateTime\s+getAuditTime\(\)[\s\S]*?return\s+confirmTime/
    )
  })
})

describe('P2-2 后端契约:OutboundOrderVO 必须有 auditorName/auditTime 别名', () => {
  const src = readFileSync(resolve(VO, 'OutboundOrderVO.java'), 'utf-8')

  it('必须有 @JsonProperty("auditorName") 返回 confirmOperatorName', () => {
    expect(src).toMatch(
      /@JsonProperty\("auditorName"\)[\s\S]*?public\s+String\s+getAuditorName\(\)[\s\S]*?return\s+confirmOperatorName/
    )
  })

  it('必须有 @JsonProperty("auditTime") 返回 confirmTime', () => {
    expect(src).toMatch(
      /@JsonProperty\("auditTime"\)[\s\S]*?public\s+(?:java\.time\.)?LocalDateTime\s+getAuditTime\(\)[\s\S]*?return\s+confirmTime/
    )
  })
})

describe('P2-2 前端契约:InboundQueryView 仍然使用 auditorName/auditTime 字段(后端别名已注入)', () => {
  const src = readView('inbound/InboundQueryView.vue')

  it('表格"审核人"列读取 row.auditorName', () => {
    expect(src).toContain('row.auditorName')
  })

  it('表格"审核时间"列读取 row.auditTime', () => {
    expect(src).toContain('row.auditTime')
  })

  it('详情对话框"审核人"读取 currentRow.auditorName', () => {
    expect(src).toContain('currentRow.auditorName')
  })

  it('详情对话框"审核时间"读取 currentRow.auditTime', () => {
    expect(src).toContain('currentRow.auditTime')
  })
})

describe('P2-2 前端契约:OutboundQueryView 仍然使用 auditorName/auditTime 字段', () => {
  const src = readView('outbound/OutboundQueryView.vue')

  it('表格"审核人"列读取 row.auditorName', () => {
    expect(src).toContain('row.auditorName')
  })

  it('表格"审核时间"列读取 row.auditTime', () => {
    expect(src).toContain('row.auditTime')
  })

  it('详情对话框"审核人"读取 currentRow.auditorName', () => {
    expect(src).toContain('currentRow.auditorName')
  })

  it('详情对话框"审核时间"读取 currentRow.auditTime', () => {
    expect(src).toContain('currentRow.auditTime')
  })
})
