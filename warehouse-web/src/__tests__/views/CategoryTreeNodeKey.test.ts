/**
 * P1-6 商品分类树 node-key 修复验证
 *
 * Bug 历史:
 *   - 原先 node-key="id",但后端 CategoryVO 字段是 categoryId
 *   - P0-3 修复时给 CategoryVO 加了 @JsonProperty("id") getId() 别名
 *   - 因此 el-tree node-key="id" 现在能匹配到 categoryId 的别名
 *
 * 测试目的:
 *   - 锁定双轨契约:CategoryVO 必须同时输出 categoryId 原字段 + id 别名
 *   - 前端 CategoryView 必须用 node-key="id" + el-tree-select value="id"
 *   - 任何一边破坏(P0-3 回归 / 前端误改)都会被这条测试捕获
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const VO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response/CategoryVO.java'
)
const VIEW = resolve(__dirname, '../../views/base/CategoryView.vue')

describe('P1-6 后端 CategoryVO 契约', () => {
  it('CategoryVO 必须有 categoryId 原字段 + getId() 别名', () => {
    const src = readFileSync(VO, 'utf-8')
    expect(src).toMatch(/private\s+Long\s+categoryId/)
    expect(src).toMatch(/@JsonProperty\("id"\)/)
    expect(src).toMatch(/public\s+Long\s+getId\(\)/)
  })
})

describe('P1-6 前端 CategoryView 契约', () => {
  it('CategoryView 的 el-tree 必须使用 node-key="id" 配合别名', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toMatch(/node-key="id"/)
  })

  it('CategoryView 的 el-tree-select 必须使用 value="id"', () => {
    const src = readFileSync(VIEW, 'utf-8')
    // el-tree-select 的 props 中 value 必须为 'id'(对应 CategoryVO 的 getId())
    expect(src).toMatch(/value:\s*'id'/)
  })

  it('CategoryView 必须能从 data.id 拿到节点 ID(categoryId 别名)', () => {
    const src = readFileSync(VIEW, 'utf-8')
    // form.id = data.id,form.parentId = data.parentId(原生字段),data.children
    expect(src).toMatch(/form\.id\s*=\s*data\.id/)
    expect(src).toMatch(/data\.parentId/)
    expect(src).toMatch(/data\.categoryName/)
  })
})