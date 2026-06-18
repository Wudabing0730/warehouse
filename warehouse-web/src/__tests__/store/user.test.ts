import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the request module so we can control what service.post returns
// without actually making HTTP calls.
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

// Import AFTER the mock is registered so the store picks up the mock.
import service from '@/utils/request'
import { useUserStore } from '@/store/user'
import { getToken, getRefreshToken } from '@/utils/auth'

describe('user store - login / refresh token extraction (regression for 401 bug)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.mocked(service.post).mockReset()
  })

  it('login() unwraps Result<T>.data and saves real JWT to localStorage (was saving undefined)', async () => {
    // service.post returns the wrapped Result<T> body
    // because the response interceptor only does `return response.data`
    // (i.e. it returns the full body, NOT body.data).
    vi.mocked(service.post).mockResolvedValueOnce({
      code: 200,
      message: '操作成功',
      data: {
        accessToken: 'jwt.access.token',
        refreshToken: 'jwt.refresh.token',
        userInfo: {
          userId: 1,
          username: 'admin',
          realName: '系统管理员',
          roles: ['系统管理员'],
          permissions: ['dashboard:view'],
        },
      },
      timestamp: Date.now(),
    })

    const store = useUserStore()
    await store.login('admin', 'admin123')

    // Critical assertion: localStorage MUST contain the real JWT.
    // Before the fix these were `undefined` (or the string "undefined"),
    // which caused every protected request to come back as 401.
    expect(getToken()).toBe('jwt.access.token')
    expect(getRefreshToken()).toBe('jwt.refresh.token')
    expect(store.accessToken).toBe('jwt.access.token')
    expect(store.refreshToken).toBe('jwt.refresh.token')

    // userInfo fields should also be populated correctly
    expect(store.username).toBe('admin')
    expect(store.realName).toBe('系统管理员')
    expect(store.roles).toEqual(['系统管理员'])
  })

  it('refreshAccessToken() unwraps .data from the wrapped refresh response', async () => {
    // Pre-seed localStorage so refreshAccessToken has a refresh token to use
    localStorage.setItem('refresh_token', 'old.refresh.token')

    vi.mocked(service.post).mockResolvedValueOnce({
      code: 200,
      message: 'ok',
      data: { accessToken: 'new.access.token', refreshToken: 'new.refresh.token' },
      timestamp: Date.now(),
    })

    const store = useUserStore()
    await store.refreshAccessToken()

    expect(getToken()).toBe('new.access.token')
    expect(getRefreshToken()).toBe('new.refresh.token')
    expect(store.accessToken).toBe('new.access.token')
  })
})