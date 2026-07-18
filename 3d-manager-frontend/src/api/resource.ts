import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type { Resource, ResourceType, ResourceBatchDeleteResult } from '@/types/report'

export function getResources(params: {
  keyword?: string
  type?: ResourceType
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<PageData<Resource>>>('/resources', { params })
}

export function uploadResource(data: {
  file: File
  type?: ResourceType
  tags?: string
}) {
  const formData = new FormData()
  formData.append('file', data.file)
  if (data.type) formData.append('type', data.type)
  if (data.tags) formData.append('tags', data.tags)
  return request.post<ApiResponse<Resource>>('/resources/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteResource(id: number) {
  return request.delete<ApiResponse<void>>(`/resources/${id}`)
}

export function batchDeleteResources(ids: number[]) {
  return request.delete<ApiResponse<ResourceBatchDeleteResult>>('/resources/batch', {
    params: { ids: ids.join(',') },
  })
}

export function downloadResource(id: number) {
  return request.get(`/resources/${id}/download`, { responseType: 'blob' })
}
