import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type {
  DataSource,
  DataSourceCreateRequest,
  DataSourceTestRequest,
  ConnectionTestResult,
  TableInfo,
  ColumnInfo,
  DataPreviewResult,
} from '@/types/datasource'

export function getDataSources(params: {
  keyword?: string
  type?: string
  page?: number
  size?: number
}) {
  return request.get<ApiResponse<PageData<DataSource>>>('/datasources', { params })
}

export function getDataSource(id: number) {
  return request.get<ApiResponse<DataSource>>(`/datasources/${id}`)
}

export function createDataSource(data: DataSourceCreateRequest) {
  return request.post<ApiResponse<DataSource>>('/datasources', data)
}

export function updateDataSource(id: number, data: Partial<DataSourceCreateRequest>) {
  return request.put<ApiResponse<DataSource>>(`/datasources/${id}`, data)
}

export function deleteDataSource(id: number) {
  return request.delete<ApiResponse<void>>(`/datasources/${id}`)
}

export function testConnectionDirect(data: DataSourceTestRequest) {
  return request.post<ApiResponse<ConnectionTestResult>>('/datasources/test', data)
}

export function testConnection(id: number) {
  return request.post<ApiResponse<ConnectionTestResult>>(`/datasources/${id}/test`)
}

export function getTables(id: number) {
  return request.get<ApiResponse<TableInfo[]>>(`/datasources/${id}/tables`)
}

export function getColumns(id: number, tableName: string) {
  return request.get<ApiResponse<ColumnInfo[]>>(`/datasources/${id}/tables/${tableName}/columns`)
}

export function previewData(id: number, tableName: string, limit = 100) {
  return request.get<ApiResponse<DataPreviewResult>>(
    `/datasources/${id}/tables/${tableName}/preview`,
    { params: { limit } }
  )
}

export function executeQuery(id: number, sql: string) {
  return request.post<ApiResponse<DataPreviewResult>>(`/datasources/${id}/query`, { sql })
}
