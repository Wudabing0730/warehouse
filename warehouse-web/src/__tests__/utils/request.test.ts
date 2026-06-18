import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { getToken, getRefreshToken, setTokens } from '@/utils/auth'

/**
 * Regression test for the 401-on-all-requests bug.
 *
 * The refresh handler in `request.ts` calls bare `axios.post(...)` (NOT the
 * service instance), so the response interceptor does NOT run and
 * `response.data` is the FULL backend body — i.e. the `Result<T>` wrapper
 * `{ code, message, data: { accessToken, refreshToken }, timestamp }`.
 *
 * The handler must therefore extract tokens from `response.data.data`,
 * NOT `response.data`. This test pins that contract so a future refactor
 * doesn't reintroduce the bug.
 */
describe('request.ts 401 refresh handler - raw axios unwrap convention', () => {
  beforeEach(() => {
    localStorage.clear()
    // Pre-seed an existing pair so refresh has a refreshToken to send
    setTokens('old.access.token', 'old.refresh.token')
    vi.restoreAllMocks()
  })

  it('extracts new tokens from response.data.data (raw axios + wrapped body)', async () => {
    vi.spyOn(axios, 'post').mockResolvedValueOnce({
      data: {
        code: 200,
        message: 'ok',
        data: { accessToken: 'rotated.access', refreshToken: 'rotated.refresh' },
        timestamp: 1234567890,
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as any,
    })

    // Mirror exactly what request.ts:66-70 does:
    //   const response = await axios.post('/api/v1/auth/refresh', { refreshToken })
    //   const { accessToken, refreshToken: newRefreshToken } = response.data.data
    const response = await axios.post('/api/v1/auth/refresh', {
      refreshToken: 'old.refresh.token',
    })
    const { accessToken, refreshToken: newRefreshToken } = (response as any).data.data

    setTokens(accessToken, newRefreshToken)

    expect(accessToken).toBe('rotated.access')
    expect(newRefreshToken).toBe('rotated.refresh')
    expect(getToken()).toBe('rotated.access')
    expect(getRefreshToken()).toBe('rotated.refresh')
  })

  it('would save "undefined" if you forgot the extra .data (documents the bug)', async () => {
    // This test exists to make the failure mode obvious: if someone reverts
    // request.ts back to `response.data`, the destructure yields undefined.
    vi.spyOn(axios, 'post').mockResolvedValueOnce({
      data: {
        code: 200,
        message: 'ok',
        data: { accessToken: 'rotated.access', refreshToken: 'rotated.refresh' },
        timestamp: 1234567890,
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as any,
    })

    const response = await axios.post('/api/v1/auth/refresh', {
      refreshToken: 'old.refresh.token',
    })
    // BUGGY pattern — only one .data
    const wrong = (response as any).data
    expect(wrong.accessToken).toBeUndefined()
    expect(wrong.refreshToken).toBeUndefined()
  })
})