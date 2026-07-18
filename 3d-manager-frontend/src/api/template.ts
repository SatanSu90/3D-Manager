import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type { Template, TemplateCreateRequest, TemplateApplyResult, TemplateCategory } from '@/types/report'

export function getTemplates(params: {
  keyword?: string
  category?: TemplateCategory
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<PageData<Template>>>('/templates', { params })
}

export function getTemplate(id: number) {
  return request.get<ApiResponse<Template>>(`/templates/${id}`)
}

export function createTemplate(data: TemplateCreateRequest) {
  return request.post<ApiResponse<Template>>('/templates', data)
}

export function updateTemplate(id: number, data: Partial<TemplateCreateRequest>) {
  return request.put<ApiResponse<Template>>(`/templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete<ApiResponse<void>>(`/templates/${id}`)
}

export function applyTemplate(id: number) {
  return request.post<ApiResponse<TemplateApplyResult>>(`/templates/${id}/apply`)
}
