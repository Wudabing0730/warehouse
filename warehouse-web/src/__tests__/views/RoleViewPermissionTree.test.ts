/**
 * P0-1 修复验证:RoleView.vue 中 el-tree 的 node-key / props.label 必须与
 * 后端 PermissionVO 字段一致。
 *
 * Bug 复现:之前 `node-key="id"` + `:props="{ label: 'name' }"` 与后端
 * PermissionVO 的 permissionId / permissionName 不匹配。
 * 表现:
 *   1. Element Plus 渲染时 label 字段是 undefined → 树节点文字为空
 *   2. setCheckedKeys() 用后端返回的 permissionId 数组匹配 node-key="id"
 *      → 全部不命中,checkedKeys 静默为空
 *   3. 用户点击"保存" → getCheckedKeys() 返回 [] → 后端把
 *      t_role_permission 里该 role 的所有关联清空
 *
 * 修复:`node-key="permissionId"` + `:props="{ label: 'permissionName' }"`
 *
 * 测试方法:读取 RoleView.vue 源文件,正则断言关键模式,
 *         确保不会再被无意回退到错误的字段名。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const ROLE_VIEW_PATH = resolve(__dirname, '../../views/system/RoleView.vue')
const source = readFileSync(ROLE_VIEW_PATH, 'utf-8')

describe('P0-1 RoleView permission tree el-tree config must match PermissionVO', () => {
  it('uses node-key="permissionId" (must match backend PermissionVO.permissionId)', () => {
    expect(source).toContain('node-key="permissionId"')
    // 不能用旧的 "id",否则与后端字段错配
    expect(source).not.toMatch(/node-key=["']id["']/)
  })

  it('uses permissionName as el-tree props.label (must match PermissionVO.permissionName)', () => {
    // 宽松匹配:允许 props 顺序/引号风格有差异,但 label 字段必须是 permissionName
    expect(source).toMatch(/props\s*=\s*["'{][^"'}]*label:\s*['"]permissionName['"]/)
    // 不能用旧的 "name"
    expect(source).not.toMatch(/props\s*=\s*["'{][^"'}]*label:\s*['"]name['"][^"'}]*[,}]/)
  })

  it('uses "children" as el-tree children prop (must match PermissionVO.children)', () => {
    expect(source).toMatch(/children:\s*['"]children['"]/)
  })

  it('PermissionVO contract reminder: backend field name is the source of truth', () => {
    // 这个 case 提醒维护者:任何修改 PermissionVO 字段后必须同步修改此处
    const backendVoFieldRegex = /private\s+Long\s+permissionId/
    expect(source.length).toBeGreaterThan(0) // placeholder so vitest 不抱怨 no-op
    expect(backendVoFieldRegex.test(
      readFileSync(
        resolve(__dirname, '../../../../warehouse-server/src/main/java/com/warehouse/dto/response/PermissionVO.java'),
        'utf-8'
      )
    )).toBe(true)
  })
})