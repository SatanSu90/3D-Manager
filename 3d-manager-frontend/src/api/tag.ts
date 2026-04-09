import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Tag } from '@/types/model'

export function getTags() {
  return request.get<ApiResponse<Tag[]>>('/tags')
}

export function createTag(data: { name: string }) {
  return request.post<ApiResponse<Tag>>('/tags', data)
}

export function updateTag(id: number, data: { name: string }) {
  return request.put<ApiResponse<Tag>>(`/tags/${id}`, data)
}

export function deleteTag(id: number) {
  return request.delete<ApiResponse<void>>(`/tags/${id}`)
}
