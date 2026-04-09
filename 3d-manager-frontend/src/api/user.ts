import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

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
