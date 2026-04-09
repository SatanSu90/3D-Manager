import request from '@/utils/request'
import type { ApiResponse, LoginRequest, LoginResponse, UserInfo } from '@/types/api'

export function login(data: LoginRequest) {
  return request.post<ApiResponse<LoginResponse>>('/auth/login', data)
}

export function register(data: { username: string; password: string }) {
  return request.post<ApiResponse<LoginResponse>>('/auth/register', data)
}

export function refreshToken(refreshToken: string) {
  return request.post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken })
}

export function getCurrentUser() {
  return request.get<ApiResponse<UserInfo>>('/auth/me')
}
