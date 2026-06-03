import request from '@/api/request'
import type { ApiResponse, LoginResponse } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export async function loginApi(params: LoginParams) {
  if (
    import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true' &&
    params.username === 'admin' &&
    params.password === 'please_change_admin_password'
  ) {
    return {
      token: 'dev-demo-token',
      tokenType: 'Bearer',
      user: {
        id: 1,
        username: 'admin',
        realName: '系统管理员',
        status: 'enabled'
      }
    }
  }

  const response = await request.post<ApiResponse<LoginResponse>>('/api/auth/login', params)
  return response.data.data
}
