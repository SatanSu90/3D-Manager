export type IndicatorType = 'ATOMIC' | 'DERIVED' | 'COMPOSITE'
export type IndicatorValueType = 'NUMBER' | 'STRING' | 'JSON' | 'TABLE'
export type IndicatorVisibility = 'PRIVATE' | 'DEPARTMENT_SHARED' | 'PUBLIC'

export interface Indicator {
  id: number
  name: string
  description: string
  type: IndicatorType
  valueType: IndicatorValueType
  value: string
  dataSourceId?: number
  taskId?: number
  ownerId: number
  ownerUsername?: string
  tags?: string
  visibility: IndicatorVisibility
  createdAt: string
  updatedAt: string
}

export interface IndicatorQuery {
  keyword?: string
  type?: string
  page?: number
  size?: number
}

export interface IndicatorCreateRequest {
  name: string
  description?: string
  type?: IndicatorType
  valueType?: IndicatorValueType
  value: string
  tags?: string
  visibility?: IndicatorVisibility
}

export type IndicatorUpdateRequest = Partial<IndicatorCreateRequest>
