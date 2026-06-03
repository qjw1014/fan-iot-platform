import { defineStore } from 'pinia'

import { loginApi, type LoginParams } from '@/api/auth'
import type { UserInfo } from '@/types/api'

const TOKEN_KEY = 'fan_iot_token'
const USER_KEY = 'fan_iot_user'

function loadUser() {
  const value = localStorage.getItem(USER_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as UserInfo
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: loadUser() as UserInfo | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(params: LoginParams) {
      const result = await loginApi(params)
      this.token = result.token
      this.user = result.user
      localStorage.setItem(TOKEN_KEY, result.token)
      localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
