import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type {
  Role,
  RoleCreateRequest,
  SystemConfig,
  SystemConfigUpdateRequest,
  OperationLog,
  OperationLogPage,
} from '@/types/system'

// === 角色 ===
export function getRoles() {
  return request.get<ApiResponse<Role[]>>('/roles')
}

export function getRole(id: number) {
  return request.get<ApiResponse<Role>>(`/roles/${id}`)
}

export function getRoleUsers(id: number) {
  return request.get<ApiResponse<Array<{ id: number; username: string; role: string; email: string; phone: string; status: string }>>>(`/roles/${id}/users`)
}

export function createRole(data: RoleCreateRequest) {
  return request.post<ApiResponse<Role>>('/roles', data)
}

export function updateRole(id: number, data: RoleCreateRequest) {
  return request.put<ApiResponse<Role>>(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete<ApiResponse<void>>(`/roles/${id}`)
}

export function assignRole(roleId: number, userId: number) {
  return request.post<ApiResponse<void>>(`/roles/${roleId}/users/${userId}`)
}

export function removeRole(roleId: number, userId: number) {
  return request.delete<ApiResponse<void>>(`/roles/${roleId}/users/${userId}`)
}

// === 系统配置 ===
export function getSystemConfigs() {
  return request.get<ApiResponse<Record<string, SystemConfig[]>>>('/system/config')
}

export function getSystemConfig(key: string) {
  return request.get<ApiResponse<SystemConfig>>(`/system/config/${key}`)
}

export function updateSystemConfig(key: string, data: SystemConfigUpdateRequest) {
  return request.put<ApiResponse<SystemConfig>>(`/system/config/${key}`, data)
}

export function batchUpdateSystemConfig(data: SystemConfigUpdateRequest) {
  return request.put<ApiResponse<SystemConfig[]>>('/system/config/batch', data)
}

// === 操作日志 ===
export function getOperationLogs(params: {
  module?: string
  action?: string
  userId?: number
  status?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<OperationLogPage>>('/system/logs', { params })
}

export function cleanupLogs(days = 30) {
  return request.delete<ApiResponse<{ deleted: number; days: number }>>('/system/logs/cleanup', {
    params: { days },
  })
}
