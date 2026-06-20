/**
 * Cross-module rowKey / field 契约 sweep 测试
 *
 * 历史 bug(2026-06-20):
 *   1. OperationLogView 表格 row-key="id" + prop="id",但后端 OperationLogVO 字段
 *      是 logId(无 @JsonProperty("id") 别名) → 所有行 rowKey=undefined,
 *      点任意一行展开 → 所有行一起展开;ID 列空白。
 *   2. StockInitView 表格 prop="id",但 StockVO.stockId 无 id 别名 → ID 列空白。
 *   3. StockInitView / ProductView / InboundFormView / OutboundFormView 多处
 *      库位下拉 :value="loc.id",但 LocationVO.locationId 无 id 别名 →
 *      库位选择写入 undefined,提交校验失败。
 *   4. 客户/供应商/产品/用户/入库/出库/盘点/借还 这些 VO 都有 @JsonProperty("id")
 *      getId() 别名,前端用 .id 是 OK 的,不能回退到 xxxId 破坏这些视图。
 *
 * 本测试双管齐下:
 *   A. 已知"必须改"的视图,锁定 row-key/prop 字段名
 *   B. 已知"必须保留别名"的 VO,锁定 @JsonProperty("id") getId()
 *   C. 全局 sweep:任何 row-key/node-key 必须是已知后端字段
 *
 * 任何一边破坏(后端删别名 / 前端改字段名)都会被捕获。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { resolve, join } from 'path'

const VIEWS_DIR = resolve(__dirname, '../../views')
const VO_DIR = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/response'
)

function readSrc(rel: string): string {
  return readFileSync(resolve(__dirname, rel), 'utf-8')
}

function listVue(dir: string, out: string[] = []): string[] {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) listVue(p, out)
    else if (p.endsWith('.vue')) out.push(p)
  }
  return out
}

// ───────────────────────────────────────────────────────────────────
// A. 必须改的视图:row-key/prop 字段名锁定
// ───────────────────────────────────────────────────────────────────

describe('A. OperationLogView 后端字段是 logId(无 id 别名)', () => {
  const src = readSrc('../../views/system/OperationLogView.vue')

  it('row-key 必须用 logId,不能用 id', () => {
    expect(src).toMatch(/row-key=["']logId["']/)
    expect(src).not.toMatch(/row-key=["']id["']/)
  })

  it('ID 列 prop 必须用 logId', () => {
    expect(src).toMatch(/prop=["']logId["']/)
  })

  it('TypeScript 接口 logId 字段名锁定', () => {
    expect(src).toMatch(/logId:\s*number/)
  })
})

describe('A. StockInitView 后端字段是 stockId / locationId(均无 id 别名)', () => {
  const src = readSrc('../../views/base/StockInitView.vue')

  it('表格 ID 列 prop 必须用 stockId,不能用 id', () => {
    expect(src).toMatch(/prop=["']stockId["']/)
    expect(src).not.toMatch(/<el-table-column\s+prop=["']id["']/)
  })

  it('库位下拉 :key 和 :value 都必须用 loc.locationId', () => {
    expect(src).not.toMatch(/loc\.id\b/)
    expect(src).toMatch(/:key=["']loc\.locationId["']/)
    expect(src).toMatch(/:value=["']loc\.locationId["']/)
  })
})

describe('A. ProductView 库位下拉也用 loc.locationId', () => {
  const src = readSrc('../../views/base/ProductView.vue')

  it('不允许出现 loc.id', () => {
    expect(src).not.toMatch(/loc\.id\b/)
  })

  it('库位下拉 :value 用 loc.locationId', () => {
    expect(src).toMatch(/:value=["']loc\.locationId["']/)
  })
})

describe('A. InboundFormView 库位下拉也用 loc.locationId', () => {
  const src = readSrc('../../views/inbound/InboundFormView.vue')

  it('不允许出现 loc.id', () => {
    expect(src).not.toMatch(/loc\.id\b/)
  })

  it('库位下拉 :value 用 loc.locationId', () => {
    expect(src).toMatch(/:value=["']loc\.locationId["']/)
  })
})

describe('A. OutboundFormView 库位下拉也用 loc.locationId', () => {
  const src = readSrc('../../views/outbound/OutboundFormView.vue')

  it('不允许出现 loc.id', () => {
    expect(src).not.toMatch(/loc\.id\b/)
  })

  it('库位下拉 :value 用 loc.locationId', () => {
    expect(src).toMatch(/:value=["']loc\.locationId["']/)
  })
})

// ───────────────────────────────────────────────────────────────────
// B. 必须保留 id 别名的 VO(否则 .id 在前端失效)
// ───────────────────────────────────────────────────────────────────

const ALIASED_VOS = [
  'CategoryVO',
  'CustomerVO',
  'SupplierVO',
  'ProductVO',
  'UserVO',
  'RoleVO',
  'InboundOrderVO',
  'OutboundOrderVO',
  'InventoryCheckVO',
  'BorrowRecordVO',
] as const

describe.each(ALIASED_VOS)('B. %s 必须保留 @JsonProperty("id") getId() 别名', (voName) => {
  const src = readFileSync(resolve(VO_DIR, `${voName}.java`), 'utf-8')

  it(`@JsonProperty("id") 存在`, () => {
    expect(src).toMatch(/@JsonProperty\(\s*["']id["']\s*\)/)
  })

  it('getId() 存在', () => {
    expect(src).toMatch(/public\s+Long\s+getId\s*\(\s*\)/)
  })
})

const NON_ALIASED_VOS = [
  { name: 'PermissionVO', primaryId: 'permissionId' },
  { name: 'OperationLogVO', primaryId: 'logId' },
  { name: 'StockVO', primaryId: 'stockId' },
  { name: 'LocationVO', primaryId: 'locationId' },
] as const

describe.each(NON_ALIASED_VOS)(
  'B. %s 不应误加 id 别名(主键是 $primaryId)',
  ({ name, primaryId }) => {
    const src = readFileSync(resolve(VO_DIR, `${name}.java`), 'utf-8')

    it(`字段名是 ${primaryId}`, () => {
      expect(src).toMatch(new RegExp(`private\\s+Long\\s+${primaryId}`))
    })

    it('没有 @JsonProperty("id") 别名(避免与 xxxId 双向对齐冲突)', () => {
      expect(src).not.toMatch(/@JsonProperty\(\s*["']id["']\s*\)/)
    })
  }
)

// ───────────────────────────────────────────────────────────────────
// C. 全局 sweep:row-key/node-key 必须是已知后端字段
// ───────────────────────────────────────────────────────────────────

const KNOWN_ROW_KEYS = new Set([
  'id', // 仅当对应 VO 在 ALIASED_VOS 中有别名
  'permissionId',
  'logId',
  'stockId',
  'locationId',
  'productId',
  'customerId',
  'supplierId',
  'userId',
  'roleId',
  'orderId',
  'recordId',
  'checkId',
  'categoryId',
])

describe('C. 全 sweep — 所有 .vue 中 row-key 必须是已知后端字段', () => {
  const files = listVue(VIEWS_DIR)
  const SKIP_FILES = new Set<string>([
    resolve(VIEWS_DIR, 'base/CategoryView.vue'),
    resolve(VIEWS_DIR, 'system/PermissionView.vue'),
  ])

  const filesWithRowKey = files.filter((f) => !SKIP_FILES.has(f))

  it.each(filesWithRowKey)('%s 有合法 row-key', (file) => {
    const src = readFileSync(file, 'utf-8')
    const m = src.match(/row-key=["']([a-zA-Z_][a-zA-Z0-9_]*)["']/)
    if (!m) return // 没有 row-key,跳过
    expect(
      KNOWN_ROW_KEYS.has(m[1]),
      `${file}: row-key="${m[1]}" 不在已知后端字段白名单中。若新增 VO 请同步更新 KNOWN_ROW_KEYS。`
    ).toBe(true)
  })
})

describe('C. 全 sweep — 所有 .vue 中 node-key 必须是已知后端字段', () => {
  const files = listVue(VIEWS_DIR)
  const SKIP_FILES = new Set<string>([
    resolve(VIEWS_DIR, 'base/CategoryView.vue'),
    resolve(VIEWS_DIR, 'system/PermissionView.vue'),
    resolve(VIEWS_DIR, 'system/RoleView.vue'),
  ])

  const filesWithNodeKey = files.filter((f) => !SKIP_FILES.has(f))

  it.each(filesWithNodeKey)('%s 有合法 node-key', (file) => {
    const src = readFileSync(file, 'utf-8')
    const m = src.match(/node-key=["']([a-zA-Z_][a-zA-Z0-9_]*)["']/)
    if (!m) return
    expect(
      KNOWN_ROW_KEYS.has(m[1]),
      `${file}: node-key="${m[1]}" 不在已知后端字段白名单中。`
    ).toBe(true)
  })
})