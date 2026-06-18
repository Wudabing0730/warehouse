/**
 * P1-2 供应商/客户/库位列表搜索框修复验证
 *
 * Bug 复现:
 *   后端 Supplier/Customer/Location 的 Controller.list 只接 page/size,前端发 supplierCode/customerCode/locationCode
 *   搜索条件被后端忽略,返回所有记录
 *
 * 修复:
 *   - 新增 SupplierQueryDTO / CustomerQueryDTO / LocationQueryDTO
 *   - 3 个 Controller.list 接对应 QueryDTO,3 个 ServiceImpl 用 QueryWrapper.like 拼接
 *   - 前端 fetchList 把 searchForm 字段塞进 params,Spring MVC 自动绑定到 DTO
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const CTRL = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/controller'
)
const SVC = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/service'
)
const IMPL = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/service/impl'
)
const DTO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/request'
)

describe('P1-2 后端契约:Supplier/Customer/Location 必须支持搜索条件', () => {
  it('SupplierQueryDTO 必须包含 supplierCode/supplierName/status 字段', () => {
    const src = readFileSync(resolve(DTO, 'SupplierQueryDTO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+supplierCode/)
    expect(src).toMatch(/private\s+String\s+supplierName/)
    expect(src).toMatch(/private\s+Integer\s+status/)
  })

  it('CustomerQueryDTO 必须包含 customerCode/customerName/status 字段', () => {
    const src = readFileSync(resolve(DTO, 'CustomerQueryDTO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+customerCode/)
    expect(src).toMatch(/private\s+String\s+customerName/)
    expect(src).toMatch(/private\s+Integer\s+status/)
  })

  it('LocationQueryDTO 必须包含 locationCode/locationName/zone/status 字段', () => {
    const src = readFileSync(resolve(DTO, 'LocationQueryDTO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+locationCode/)
    expect(src).toMatch(/private\s+String\s+locationName/)
    expect(src).toMatch(/private\s+String\s+zone/)
    expect(src).toMatch(/private\s+Integer\s+status/)
  })

  it('SupplierController.list 必须接受 SupplierQueryDTO 参数', () => {
    const src = readFileSync(resolve(CTRL, 'SupplierController.java'), 'utf-8')
    expect(src).toMatch(/public\s+Result<IPage<SupplierVO>>\s+list\(/)
    expect(src).toContain('SupplierQueryDTO query')
    expect(src).toContain('supplierService.page(new Page<>(page, size), query)')
  })

  it('CustomerController.list 必须接受 CustomerQueryDTO 参数', () => {
    const src = readFileSync(resolve(CTRL, 'CustomerController.java'), 'utf-8')
    expect(src).toMatch(/public\s+Result<IPage<CustomerVO>>\s+list\(/)
    expect(src).toContain('CustomerQueryDTO query')
    expect(src).toContain('customerService.page(new Page<>(page, size), query)')
  })

  it('WarehouseLocationController.list 必须接受 LocationQueryDTO 参数', () => {
    const src = readFileSync(resolve(CTRL, 'WarehouseLocationController.java'), 'utf-8')
    expect(src).toMatch(/public\s+Result<IPage<LocationVO>>\s+list\(/)
    expect(src).toContain('LocationQueryDTO query')
    expect(src).toContain('warehouseLocationService.page(new Page<>(page, size), query)')
  })

  it('SupplierServiceImpl.page 必须用 like 拼接 supplierCode/supplierName', () => {
    const src = readFileSync(resolve(IMPL, 'SupplierServiceImpl.java'), 'utf-8')
    expect(src).toContain('wrapper.like("supplier_code"')
    expect(src).toContain('wrapper.like("supplier_name"')
  })

  it('CustomerServiceImpl.page 必须用 like 拼接 customerCode/customerName', () => {
    const src = readFileSync(resolve(IMPL, 'CustomerServiceImpl.java'), 'utf-8')
    expect(src).toContain('wrapper.like("customer_code"')
    expect(src).toContain('wrapper.like("customer_name"')
  })

  it('WarehouseLocationServiceImpl.page 必须用 like 拼接 locationCode/locationName', () => {
    const src = readFileSync(resolve(IMPL, 'WarehouseLocationServiceImpl.java'), 'utf-8')
    expect(src).toContain('wrapper.like("location_code"')
    expect(src).toContain('wrapper.like("location_name"')
  })
})

describe('P1-2 前端契约:SupplierView/CustomerView/LocationView 必须发送搜索字段', () => {
  it('SupplierView fetchList 必须把 supplierCode/supplierName 加进 params', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/base/SupplierView.vue'),
      'utf-8'
    )
    expect(src).toMatch(/params\.supplierCode\s*=\s*searchForm\.supplierCode/)
    expect(src).toMatch(/params\.supplierName\s*=\s*searchForm\.supplierName/)
  })

  it('CustomerView fetchList 必须把 customerCode/customerName 加进 params', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/base/CustomerView.vue'),
      'utf-8'
    )
    expect(src).toMatch(/params\.customerCode\s*=\s*searchForm\.customerCode/)
    expect(src).toMatch(/params\.customerName\s*=\s*searchForm\.customerName/)
  })

  it('LocationView fetchList 必须把 locationCode/locationName 加进 params', () => {
    const src = readFileSync(
      resolve(__dirname, '../../views/base/LocationView.vue'),
      'utf-8'
    )
    expect(src).toMatch(/params\.locationCode\s*=\s*searchForm\.locationCode/)
    expect(src).toMatch(/params\.locationName\s*=\s*searchForm\.locationName/)
  })
})