import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type { Report, ReportCreateRequest, ReportType, ReportStatus } from '@/types/report'

export function getReports(params: {
  keyword?: string
  type?: ReportType
  status?: ReportStatus
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<PageData<Report>>>('/reports', { params })
}

export function getReport(id: number) {
  return request.get<ApiResponse<Report>>(`/reports/${id}`)
}

export function createReport(data: ReportCreateRequest) {
  return request.post<ApiResponse<Report>>('/reports', data)
}

export function updateReport(id: number, data: Partial<ReportCreateRequest>) {
  return request.put<ApiResponse<Report>>(`/reports/${id}`, data)
}

export function deleteReport(id: number) {
  return request.delete<ApiResponse<void>>(`/reports/${id}`)
}

export function copyReport(id: number) {
  return request.post<ApiResponse<Report>>(`/reports/${id}/copy`)
}

export function updateReportStatus(id: number, status: ReportStatus) {
  return request.put<ApiResponse<Report>>(`/reports/${id}/status`, { status })
}

export function generateReportFromScene(sceneId: number) {
  return request.post<ApiResponse<Report>>(`/reports/from-scene/${sceneId}`)
}
