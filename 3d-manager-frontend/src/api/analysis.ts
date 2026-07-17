import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type {
  AnalysisTask,
  AnalysisDataSourceRef,
  AnalysisStep,
  AnalysisResult,
  AnalysisTaskCreateRequest,
  AnalysisTaskQuery,
  AddDataSourceRequest,
  AddStepRequest,
  UpdateStepRequest,
  SaveAsIndicatorRequest,
} from '@/types/analysis'

// 分析任务
export function createAnalysisTask(data: AnalysisTaskCreateRequest) {
  return request.post<ApiResponse<AnalysisTask>>('/analysis/tasks', data)
}

export function getAnalysisTasks(params: AnalysisTaskQuery) {
  return request.get<ApiResponse<PageData<AnalysisTask>>>('/analysis/tasks', { params })
}

export function getAnalysisTask(id: number) {
  return request.get<ApiResponse<AnalysisTask>>(`/analysis/tasks/${id}`)
}

export function deleteAnalysisTask(id: number) {
  return request.delete<ApiResponse<void>>(`/analysis/tasks/${id}`)
}

export function updateAnalysisTask(id: number, data: Partial<AnalysisTaskCreateRequest>) {
  return request.put<ApiResponse<AnalysisTask>>(`/analysis/tasks/${id}`, data)
}

// 数据源关联
export function addTaskDataSource(taskId: number, data: AddDataSourceRequest) {
  return request.post<ApiResponse<AnalysisDataSourceRef>>(`/analysis/tasks/${taskId}/datasources`, data)
}

export function removeTaskDataSource(taskId: number, dsRefId: number) {
  return request.delete<ApiResponse<void>>(`/analysis/tasks/${taskId}/datasources/${dsRefId}`)
}

// 步骤管理
export function addAnalysisStep(taskId: number, data: AddStepRequest) {
  return request.post<ApiResponse<AnalysisStep>>(`/analysis/tasks/${taskId}/steps`, data)
}

export function updateAnalysisStep(stepId: number, data: UpdateStepRequest) {
  return request.put<ApiResponse<AnalysisStep>>(`/analysis/steps/${stepId}`, data)
}

export function deleteAnalysisStep(stepId: number) {
  return request.delete<ApiResponse<void>>(`/analysis/steps/${stepId}`)
}

// 执行与结果
export function executeAnalysisTask(taskId: number) {
  return request.post<ApiResponse<AnalysisTask>>(`/analysis/tasks/${taskId}/execute`)
}

export function getAnalysisResults(taskId: number) {
  return request.get<ApiResponse<AnalysisResult>>(`/analysis/tasks/${taskId}/results`)
}

export function saveAsIndicator(taskId: number, data: SaveAsIndicatorRequest) {
  return request.post<ApiResponse<void>>(`/analysis/tasks/${taskId}/save-indicator`, data)
}
