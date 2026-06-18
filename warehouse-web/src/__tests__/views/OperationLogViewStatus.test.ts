/**
 * P0-6 修复验证:OperationLogView 的状态筛选下拉必须使用整型 1/0 绑定,
 * 不能用 'success' / 'fail' 字符串(后端 OperationLogQueryDTO.status 是 Integer)。
 *
 * Bug 复现:之前 status 选项值是字符串 'success' / 'fail',后端 DTO 用 Integer,
 * Jackson 反序列化失败或匹配 0 条记录,选择"成功"/"失败"过滤永远查不到。
 *
 * 修复:选项值用 :value="1" / :value="0" 整型绑定;表格展示也用 === 1 判断。
 */
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const SOURCE = readFileSync(
  resolve(__dirname, '../../views/system/OperationLogView.vue'),
  'utf-8'
)

describe('P0-6 OperationLogView status filter must use integer 1/0 (matches backend Integer)', () => {
  it('search form status options must use :value="1" and :value="0"', () => {
    expect(SOURCE).toMatch(/<el-option\s+label="成功"\s+:value="1"/)
    expect(SOURCE).toMatch(/<el-option\s+label="失败"\s+:value="0"/)
  })

  it('must NOT use legacy string status values "success" or "fail" for options', () => {
    // 排除整型 :value,只查字符串 value
    expect(SOURCE).not.toMatch(/<el-option[^>]*value="success"/)
    expect(SOURCE).not.toMatch(/<el-option[^>]*value="fail"/)
  })

  it('table cell display must compare status === 1 (integer, not string)', () => {
    expect(SOURCE).toContain("row.status === 1 ? '成功' : '失败'")
    expect(SOURCE).not.toContain("row.status === 'success'")
  })

  it('searchForm.status must be typed as number | null (matches backend)', () => {
    expect(SOURCE).toMatch(/status:\s*null\s+as\s+number\s*\|\s*null/)
    expect(SOURCE).not.toMatch(/status:\s*''\s+as\s+string/)
  })

  it('LogEntry interface status must be number', () => {
    expect(SOURCE).toMatch(/status:\s*number/)
  })

  it('OperationLogQueryDTO backend contract reminder (must be Integer)', () => {
    const dto = readFileSync(
      resolve(
        __dirname,
        '../../../../warehouse-server/src/main/java/com/warehouse/dto/request/OperationLogQueryDTO.java'
      ),
      'utf-8'
    )
    expect(dto).toMatch(/private\s+Integer\s+status/)
  })
})