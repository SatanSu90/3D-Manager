import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Department, DepartmentCreateRequest, DepartmentUser } from '@/types/department'

export function getDepartmentTree() {
  return request.get<ApiResponse<Department[]>>('/departments/tree')
}

export function getDepartments() {
  return request.get<ApiResponse<Department[]>>('/departments')
}

export function getDepartment(id: number) {
  return request.get<ApiResponse<Department>>(`/departments/${id}`)
}

export function createDepartment(data: DepartmentCreateRequest) {
  return request.post<ApiResponse<Department>>('/departments', data)
}

export function updateDepartment(id: number, data: Partial<DepartmentCreateRequest>) {
  return request.put<ApiResponse<Department>>(`/departments/${id}`, data)
}

export function deleteDepartment(id: number) {
  return request.delete<ApiResponse<void>>(`/departments/${id}`)
}

export function getDepartmentUsers(id: number) {
  return request.get<ApiResponse<DepartmentUser[]>>(`/departments/${id}/users`)
}

export function assignUsers(id: number, userIds: number[]) {
  return request.post<ApiResponse<void>>(`/departments/${id}/users`, { userIds })
}

export function removeUser(id: number, userId: number) {
  return request.delete<ApiResponse<void>>(`/departments/${id}/users/${userId}`)
}
