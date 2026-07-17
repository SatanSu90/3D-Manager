import request from '@/utils/request'
import type { ApiResponse, PageData } from '@/types/api'
import type {
  Indicator,
  IndicatorQuery,
  IndicatorCreateRequest,
  IndicatorUpdateRequest,
} from '@/types/indicator'

export function getIndicators(params: IndicatorQuery) {
  return request.get<ApiResponse<PageData<Indicator>>>('/indicators', { params })
}

export function getIndicator(id: number) {
  return request.get<ApiResponse<Indicator>>(`/indicators/${id}`)
}

export function createIndicator(data: IndicatorCreateRequest) {
  return request.post<ApiResponse<Indicator>>('/indicators', data)
}

export function updateIndicator(id: number, data: IndicatorUpdateRequest) {
  return request.put<ApiResponse<Indicator>>(`/indicators/${id}`, data)
}

export function deleteIndicator(id: number) {
  return request.delete<ApiResponse<void>>(`/indicators/${id}`)
}
