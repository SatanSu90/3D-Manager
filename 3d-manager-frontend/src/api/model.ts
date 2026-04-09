import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type { Model, ModelQuery } from '@/types/model'

export function getModels(params: ModelQuery) {
  return request.get<ApiResponse<PageData<Model>>>('/models', { params })
}

export function getModel(id: number) {
  return request.get<ApiResponse<Model>>(`/models/${id}`)
}

export function uploadModel(formData: FormData) {
  return request.post<ApiResponse<Model>>('/models/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0, // 大文件不上限
  })
}

export function deleteModel(id: number) {
  return request.delete<ApiResponse<void>>(`/models/${id}`)
}

export function downloadModel(id: number) {
  return request.get(`/models/${id}/download`, { responseType: 'blob' })
}

export function getModelUrl(id: number) {
  return request.get<ApiResponse<{ downloadUrl: string }>>(`/models/${id}/download`)
}

export function updateModel(id: number, data: { name?: string; description?: string; categoryId?: number | null; tagIds?: number[] }) {
  return request.put<ApiResponse<Model>>(`/models/${id}`, data)
}
