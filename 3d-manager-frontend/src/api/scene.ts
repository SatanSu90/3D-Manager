import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type { Scene, SceneData } from '@/types/scene'

export function getScenes(params?: {
  keyword?: string
  categoryId?: number
  status?: string
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<PageData<Scene>>>('/scenes', { params })
}

export function getScene(id: number) {
  return request.get<ApiResponse<Scene>>(`/scenes/${id}`)
}

export function previewScene(id: number, password = '') {
  return request.post<ApiResponse<Scene>>(`/scenes/${id}/preview`, { password })
}

export function saveScene(data: {
  name: string
  sceneData: SceneData
  thumbnailBase64?: string
  description?: string
  categoryId?: number
  visibility?: string
  status?: string
  resolution?: string
  previewPassword?: string
}) {
  return request.post<ApiResponse<Scene>>('/scenes', {
    name: data.name,
    sceneData: JSON.stringify(data.sceneData),
    thumbnailBase64: data.thumbnailBase64,
    description: data.description,
    categoryId: data.categoryId,
    visibility: data.visibility,
    status: data.status,
    resolution: data.resolution,
    previewPassword: data.previewPassword,
  })
}

export function updateScene(id: number, data: {
  name?: string
  sceneData?: SceneData
  thumbnailBase64?: string
  description?: string
  categoryId?: number
  visibility?: string
  status?: string
  resolution?: string
  previewPassword?: string
}) {
  return request.put<ApiResponse<Scene>>(`/scenes/${id}`, {
    name: data.name,
    sceneData: data.sceneData ? JSON.stringify(data.sceneData) : undefined,
    thumbnailBase64: data.thumbnailBase64,
    description: data.description,
    categoryId: data.categoryId,
    visibility: data.visibility,
    status: data.status,
    resolution: data.resolution,
    previewPassword: data.previewPassword,
  })
}

export function deleteScene(id: number) {
  return request.delete<ApiResponse<void>>(`/scenes/${id}`)
}

export function copyScene(id: number) {
  return request.post<ApiResponse<Scene>>(`/scenes/${id}/copy`)
}

export function updateSceneStatus(id: number, status: string) {
  return request.put<ApiResponse<void>>(`/scenes/${id}/status`, { status })
}

export function exportScene(id: number) {
  return request.get(`/scenes/${id}/export`, { responseType: 'blob' })
}
