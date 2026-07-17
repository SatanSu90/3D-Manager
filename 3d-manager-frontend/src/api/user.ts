import request from '@/utils/request'
import type { ApiResponse, UserInfo } from '@/types/api'

export interface UserItem {
  id: number
  username: string
  role: string
  createdAt: string
}

export function getUsers() {
  return request.get<ApiResponse<UserItem[]>>('/users')
}

export function updateUserRole(id: number, role: string) {
  return request.put<ApiResponse<void>>(`/users/${id}/role`, { role })
}

export function deleteUser(id: number) {
  return request.delete<ApiResponse<void>>(`/users/${id}`)
}

export function getCurrentUserProfile() {
  return request.get<ApiResponse<UserInfo>>('/users/me')
}

export function updateProfile(data: {
  avatar?: string
  email?: string
  phone?: string
  oldPassword?: string
  newPassword?: string
}) {
  return request.put<ApiResponse<UserInfo>>('/users/me', data)
}

export function updateUserStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return request.put<ApiResponse<void>>(`/users/${id}/status`, { status })
}
