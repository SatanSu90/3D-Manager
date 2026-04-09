import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/api'
import { login as loginApi, refreshToken as refreshTokenApi, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshTokenValue = ref(localStorage.getItem('refreshToken') || '')
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    const data = res.data.data
    accessToken.value = data.accessToken
    refreshTokenValue.value = data.refreshToken
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    await fetchUser()
  }

  async function fetchUser() {
    try {
      const res = await getCurrentUser()
      user.value = res.data.data
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  async function refreshAccessToken(): Promise<string> {
    const res = await refreshTokenApi(refreshTokenValue.value)
    const data = res.data.data
    accessToken.value = data.accessToken
    refreshTokenValue.value = data.refreshToken
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    return data.accessToken
  }

  function logout() {
    accessToken.value = ''
    refreshTokenValue.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshTokenValue,
    user,
    isLoggedIn,
    isAdmin,
    login,
    fetchUser,
    refreshAccessToken,
    logout,
  }
})
