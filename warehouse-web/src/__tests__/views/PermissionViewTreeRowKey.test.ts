/**
 * PermissionView 树表 rowKey / tree-props 必须与后端 PermissionVO 字段一致。
 *
 * Bug 复现(2026-06-20):
 *   旧代码使用 row-key="id" + prop="name" / prop="code" + default-expand-all,
 *   而后端 PermissionVO 实际字段是 permissionId / permissionName / permissionCode,
 *   el-table 把所有行视为同一 rowKey=undefined。表现为:
 *     1. 点击任意一行的展开箭头 → 所有行一起展开(用户截图)
 *     2. 名称 / 权限编码 / 排序等列绑定到不存在的字段 → 全列空白
 *     3. header 上的"展开全部 / 折叠全部"按钮对隐藏的 el-tree 做 DOM hack,
 *        与表头本身的 el-table 无关,看起来按钮"无效"。
 *
 * 修复:
 *   1. row-key="permissionId"(匹配 PermissionVO.permissionId)
 *   2. :tree-props="{ children: 'children' }"(匹配 PermissionVO.children)
 *   3. prop 改为 permissionName / permissionCode
 *   4. 去掉 default-expand-all,改用 tableRef.toggleRowExpansion 单行控制
 *
 * 测试方法:静态读取 PermissionView.vue,正则锁定关键模式,
 *         保证未来重构不会再次退回错误配置。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const VIEW_PATH = resolve(__dirname, '../../views/system/PermissionView.vue')
const VO_PATH = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response/PermissionVO.java'
)
const source = readFileSync(VIEW_PATH, 'utf-8')

describe('PermissionView tree-table config must match PermissionVO', () => {
  it('uses row-key="permissionId" (must match backend PermissionVO.permissionId)', () => {
    expect(source).toContain('row-key="permissionId"')
    // 不能退回 "id",否则所有行 rowKey=undefined,点击展开会全部展开
    expect(source).not.toMatch(/row-key=["']id["']/)
  })

  it('declares tree-props with children (must match PermissionVO.children)', () => {
    expect(source).toMatch(/tree-props\s*=\s*["'{][^"'}]*children:\s*['"]children['"]/)
  })

  it('binds column props to permissionName / permissionCode (must match PermissionVO)', () => {
    expect(source).toMatch(/prop=["']permissionName["']/)
    expect(source).toMatch(/prop=["']permissionCode["']/)
    // 不能用旧的 name / code 字段名
    expect(source).not.toMatch(/prop=["']name["']/)
    expect(source).not.toMatch(/prop=["']code["']/)
  })

  it('does NOT use default-expand-all on el-table (would mask per-row toggle bug)', () => {
    expect(source).not.toMatch(/default-expand-all/)
  })

  it('header expand/collapse buttons operate on tableRef via toggleRowExpansion', () => {
    expect(source).toContain('tableRef')
    expect(source).toMatch(/toggleRowExpansion\([^)]*true\)/)
    expect(source).toMatch(/toggleRowExpansion\([^)]*false\)/)
  })

  it('PermissionVO backend contract reminder: permissionId is the source of truth', () => {
    const voSource = readFileSync(VO_PATH, 'utf-8')
    expect(voSource).toMatch(/private\s+Long\s+permissionId/)
    expect(voSource).toMatch(/private\s+List<PermissionVO>\s+children/)
  })
})