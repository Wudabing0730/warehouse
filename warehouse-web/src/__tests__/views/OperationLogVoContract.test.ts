/**
 * P1-7 操作日志 Controller 返回实体不是 VO 修复验证
 *
 * Bug 复现:
 *   Controller 返 IPage<OperationLog> entity
 *   前端表格读 id/description/executeTime/ip/createTime
 *   后端 entity 字段 logId/operation/executionTime/ipAddress/operateTime
 *   => 列表全空白
 *
 * 修复:
 *   - OperationLogVO 新增 description/executeTime/ip/createTime 别名 + LocalDateTime 格式化
 *   - OperationLogController 返 IPage<OperationLogVO>
 *   - OperationLogService 新增 pageVO 做 Entity -> VO 转换
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const CTRL = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/controller/OperationLogController.java'
)
const SVC = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/service/OperationLogService.java'
)
const VO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response/OperationLogVO.java'
)

describe('P1-7 后端契约:OperationLogController 必须返回 VO', () => {
  it('OperationLogController.list 返回类型必须是 IPage<OperationLogVO>', () => {
    const src = readFileSync(CTRL, 'utf-8')
    expect(src).toMatch(/public\s+Result<IPage<OperationLogVO>>\s+list/)
    expect(src).not.toMatch(/public\s+Result<IPage<OperationLog>>\s+list/)
  })

  it('OperationLogService 接口必须包含 pageVO 方法', () => {
    const src = readFileSync(SVC, 'utf-8')
    expect(src).toMatch(/IPage<OperationLogVO>\s+pageVO\(/)
  })

  it('OperationLogVO 必须通过 @JsonProperty 给字段加前端别名', () => {
    const src = readFileSync(VO, 'utf-8')
    // description -> operation
    expect(src).toMatch(/@JsonProperty\("description"\)/)
    // executeTime -> executionTime
    expect(src).toMatch(/@JsonProperty\("executeTime"\)/)
    // ip -> ipAddress
    expect(src).toMatch(/@JsonProperty\("ip"\)/)
    // createTime -> operateTime
    expect(src).toMatch(/@JsonProperty\("createTime"\)/)
    // LocalDateTime 必须带 @JsonFormat
    expect(src).toMatch(/@JsonFormat\(pattern\s*=\s*"yyyy-MM-dd HH:mm:ss"/)
  })

  it('OperationLogVO 必须有 getDescription/getExecuteTime/getIp/getCreateTime 别名 getter', () => {
    const src = readFileSync(VO, 'utf-8')
    expect(src).toMatch(/public\s+String\s+getDescription\(/)
    expect(src).toMatch(/public\s+Integer\s+getExecuteTime\(/)
    expect(src).toMatch(/public\s+String\s+getIp\(/)
    expect(src).toMatch(/public\s+LocalDateTime\s+getCreateTime\(/)
  })
})

describe('P1-7 前端契约:OperationLogView 表格列必须能匹配后端字段', () => {
  it('OperationLogView 表格列 prop 与 VO 别名一致', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/system/OperationLogView.vue'),
      'utf-8'
    )
    expect(src).toMatch(/prop="description"/)
    expect(src).toMatch(/prop="executeTime"/)
    expect(src).toMatch(/prop="ip"/)
    expect(src).toMatch(/prop="createTime"/)
  })
})