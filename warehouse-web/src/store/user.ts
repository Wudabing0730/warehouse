import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, getRefreshToken, setTokens, removeTokens as removeAuthTokens } from '@/utils/auth'
import service from '@/utils/request'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // State
  const userId = ref<string>('')
  const username = ref<string>('')
  const realName = ref<string>('')
  const accessToken = ref<string>('')
  const refreshToken = ref<string>('')
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])

  // Getters
  const isLoggedIn = computed(() => !!accessToken.value)

  // Persist to localStorage for tokens
  function persistTokens(): void {
    setTokens(accessToken.value, refreshToken.value)
  }

  function restoreTokens(): void {
    const at = getToken()
    const rt = getRefreshToken()
    if (at) accessToken.value = at
    if (rt) refreshToken.value = rt
  }

  // Actions
  async function login(usernameParam: string, password: string): Promise<void> {
    const response: any = await service.post('/auth/login', {
      username: usernameParam,
      password,
    })
    const { accessToken: at, refreshToken: rt, userInfo } = response
    accessToken.value = at
    refreshToken.value = rt
    persistTokens()

    if (userInfo) {
      userId.value = userInfo.userId ?? userInfo.id ?? ''
      username.value = userInfo.username ?? ''
      realName.value = userInfo.realName ?? ''
      permissions.value = userInfo.permissions ?? []
      roles.value = userInfo.roles ?? []
    }
  }

  async function logout(): Promise<void> {
    try {
      await service.post('/auth/logout')
    } catch {
      // Ignore logout API errors
    }
    clearState()
    router.push('/login')
  }

  async function refreshAccessToken(): Promise<void> {
    const rt = getRefreshToken()
    if (!rt) {
      clearState()
      router.push('/login')
      return
    }
    try {
      const response: any = await service.post('/auth/refresh', {
        refreshToken: rt,
      })
      accessToken.value = response.accessToken
      refreshToken.value = response.refreshToken
      persistTokens()
    } catch {
      clearState()
      router.push('/login')
    }
  }

  async function fetchUserInfo(): Promise<void> {
    // Placeholder: user info comes from login response.
    // Future enhancement: call GET /auth/userinfo to refresh user data.
  }

  function clearState(): void {
    userId.value = ''
    username.value = ''
    realName.value = ''
    accessToken.value = ''
    refreshToken.value = ''
    permissions.value = []
    roles.value = []
    removeAuthTokens()
  }

  return {
    // State
    userId,
    username,
    realName,
    accessToken,
    refreshToken,
    permissions,
    roles,
    // Getters
    isLoggedIn,
    // Actions
    login,
    logout,
    refreshAccessToken,
    fetchUserInfo,
    restoreTokens,
  }
})
