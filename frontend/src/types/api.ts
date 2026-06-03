export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface UserInfo {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  status: string
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  user: UserInfo
}
