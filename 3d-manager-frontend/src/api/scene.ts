import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Scene, SceneData } from '@/types/scene'

export function getScenes() {
  return request.get<ApiResponse<Scene[]>>('/scenes')
}

export function getScene(id: number) {
  return request.get<ApiResponse<Scene>>(`/scenes/${id}`)
}

export function saveScene(data: { name: string; sceneData: SceneData; thumbnailBase64?: string }) {
  return request.post<ApiResponse<Scene>>('/scenes', {
    name: data.name,
    sceneData: JSON.stringify(data.sceneData),
    thumbnailBase64: data.thumbnailBase64,
  })
}

export function updateScene(id: number, data: { name?: string; sceneData?: SceneData; thumbnailBase64?: string }) {
  return request.put<ApiResponse<Scene>>(`/scenes/${id}`, {
    name: data.name,
    sceneData: data.sceneData ? JSON.stringify(data.sceneData) : undefined,
    thumbnailBase64: data.thumbnailBase64,
  })
}

export function deleteScene(id: number) {
  return request.delete<ApiResponse<void>>(`/scenes/${id}`)
}

export function exportScene(id: number) {
  return request.get(`/scenes/${id}/export`, { responseType: 'blob' })
}
