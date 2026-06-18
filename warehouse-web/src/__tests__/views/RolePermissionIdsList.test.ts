/**
 * P1-4 角色列表 VO permissionIds 修复验证
 *
 * Bug 复现:
 *   - RoleVO 有 permissionIds 字段,getById 路径下 toRoleVO 会填充
 *   - 但 list 路径 page() 也会调用 toRoleVO(role),permissionIds 其实是填充的
 *   - 真正问题:前端表格没有"权限数"列,管理员看不出每个角色有没有成功分配权限
 *
 * 修复:
 *   - 前端 RoleView 表格新增"权限数"列,展示 row.permissionIds.length
 *   - 后端 RoleServiceImpl.page 用批量预取避免 N+1 查询
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const IMPL = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/service/impl'
)

describe('P1-4 RoleVO 必须包含 permissionIds 字段', () => {
  it('RoleVO 包含 permissionIds 字段', () => {
    const src = readFileSync(
      resolve(__dirname, '../../../../warehouse-server/src/main/java/com/warehouse/dto/response/RoleVO.java'),
      'utf-8'
    )
    expect(src).toMatch(/private\s+List<Long>\s+permissionIds/)
  })
})

describe('P1-4 RoleServiceImpl.page 必须用批量预取填充 permissionIds', () => {
  it('RoleServiceImpl.page 必须包含 batch prefetch 逻辑避免 N+1', () => {
    const src = readFileSync(resolve(IMPL, 'RoleServiceImpl.java'), 'utf-8')
    // 必须用 in 查询批量取,而不是逐个 role 查
    expect(src).toMatch(/in\(RolePermission::getRoleId,\s*roleIds\)/)
    // 必须按 roleId 分组聚合
    expect(src).toMatch(/Collectors\.groupingBy\(\s*RolePermission::getRoleId/)
  })
})

describe('P1-4 前端 RoleView 必须展示权限数列', () => {
  it('RoleView 表格必须有"权限数"列展示 permissionIds.length', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/system/RoleView.vue'),
      'utf-8'
    )
    expect(src).toContain('权限数')
    expect(src).toMatch(/permissionIds\?\.length/)
  })
})