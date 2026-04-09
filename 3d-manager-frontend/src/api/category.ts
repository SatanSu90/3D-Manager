import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Category } from '@/types/model'

export function getCategories() {
  return request.get<ApiResponse<Category[]>>('/categories')
}

export function createCategory(data: { name: string; parentId: number | null; sortOrder: number }) {
  return request.post<ApiResponse<Category>>('/categories', data)
}

export function updateCategory(id: number, data: { name?: string; parentId?: number | null; sortOrder?: number }) {
  return request.put<ApiResponse<Category>>(`/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete<ApiResponse<void>>(`/categories/${id}`)
}

export interface CategoryTreeNode {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  modelCount: number
}

export function getCategoryTree() {
  return request.get<ApiResponse<CategoryTreeNode[]>>('/categories/tree')
}
