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

/**
 * 批量获取指标值：通过 Promise.all 逐个请求并扁平化为 Indicator 数组
 * @returns Indicator[] (已经包含最新的 value 字段)
 */
export async function getIndicatorsByIds(ids: number[]): Promise<Indicator[]> {
  if (!ids.length) return []
  const results = await Promise.all(ids.map((id) => getIndicator(id)))
  return results.map((r) => r.data.data)
}
