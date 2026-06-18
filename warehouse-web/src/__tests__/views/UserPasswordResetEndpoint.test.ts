/**
 * P1-5 用户密码重置走错接口修复验证
 *
 * Bug 复现:
 *   - 前端 handleResetPwdSubmit 调用 updateUser({ id, password }),走的是 PUT /users/{id}
 *     这会把整个用户对象 PUT 一遍,绕过专用密码端点
 *   - 后端专用 PUT /users/{id}/password 需要 oldPassword,管理员重置场景无法提供
 *
 * 修复:
 *   - 后端新增 PUT /users/{id}/password/reset,只需 newPassword
 *   - 新增 PasswordResetDTO / PasswordChangeDTO 区分两种语义
 *   - UserService.resetPassword(userId, newPassword) 无需 oldPassword
 *   - 前端新增 resetPasswordAdmin(id, newPassword) 走专用端点
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
const DTO = resolve(
  __dirname,
  '../../../../warehouse-server/src/main/java/com/warehouse/dto/request'
)
const API = resolve(__dirname, '../../api/user.ts')
const VIEW = resolve(__dirname, '../../views/system/UserView.vue')

describe('P1-5 后端契约:必须有专用密码重置端点', () => {
  it('PasswordResetDTO 必须包含 newPassword 字段(无 oldPassword 字段)', () => {
    const src = readFileSync(resolve(DTO, 'PasswordResetDTO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+newPassword/)
    // 不应有 oldPassword 字段定义
    expect(src).not.toMatch(/private\s+String\s+oldPassword/)
  })

  it('PasswordChangeDTO 必须包含 oldPassword + newPassword', () => {
    const src = readFileSync(resolve(DTO, 'PasswordChangeDTO.java'), 'utf-8')
    expect(src).toMatch(/private\s+String\s+oldPassword/)
    expect(src).toMatch(/private\s+String\s+newPassword/)
  })

  it('UserController 必须暴露 PUT /users/{id}/password/reset 端点', () => {
    const src = readFileSync(resolve(CTRL, 'UserController.java'), 'utf-8')
    expect(src).toMatch(/@PutMapping\("\/\{id\}\/password\/reset"\)/)
    expect(src).toMatch(/@RequirePermission\("system:user:resetPassword"\)/)
    expect(src).toContain('userService.resetPassword(')
  })

  it('UserService 接口必须包含 resetPassword 方法', () => {
    const src = readFileSync(resolve(SVC, 'UserService.java'), 'utf-8')
    expect(src).toMatch(/void\s+resetPassword\(\s*Long\s+userId,\s*String\s+newPassword\s*\)/)
  })

  it('UserServiceImpl.resetPassword 不验证 oldPassword(管理员场景)', () => {
    const src = readFileSync(
      resolve(SVC, 'impl/UserServiceImpl.java'),
      'utf-8'
    )
    // 必须有 resetPassword 方法,且不含 passwordEncoder.matches(oldPassword, ...)
    const m = src.match(/public void resetPassword\([\s\S]*?^    \}/m)
    expect(m, 'UserServiceImpl 必须实现 resetPassword 方法').toBeTruthy()
    expect(m![0]).not.toContain('passwordEncoder.matches(oldPassword')
  })
})

describe('P1-5 前端契约:必须使用专用 resetPasswordAdmin API', () => {
  it('user.ts 必须导出 resetPasswordAdmin', () => {
    const src = readFileSync(API, 'utf-8')
    expect(src).toMatch(/export\s+function\s+resetPasswordAdmin/)
    expect(src).toMatch(/\/users\/\$\{id\}\/password\/reset/)
  })

  it('UserView handleResetPwdSubmit 必须调用 resetPasswordAdmin 而不是 updateUser', () => {
    const src = readFileSync(VIEW, 'utf-8')
    expect(src).toContain('resetPasswordAdmin')
    // 关键:handleResetPwdSubmit 函数体里不能调 updateUser
    const m = src.match(/async function handleResetPwdSubmit\([\s\S]*?^}/m)
    expect(m).toBeTruthy()
    expect(m![0]).not.toContain('await updateUser({ id: resetPwdEditingId')
    expect(m![0]).toContain('await resetPasswordAdmin(')
  })
})