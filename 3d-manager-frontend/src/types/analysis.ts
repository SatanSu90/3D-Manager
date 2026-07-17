export type AnalysisTaskStatus = 'DRAFT' | 'RUNNING' | 'SUCCESS' | 'FAILED'
export type AnalysisStepType = 'CLEAN' | 'FILTER' | 'STATS' | 'ADVANCED_STATS' | 'ML'
export type AnalysisStepStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export interface AnalysisTask {
  id: number
  name: string
  description: string
  status: AnalysisTaskStatus
  ownerId: number
  ownerUsername?: string
  config?: string
  resultSummary?: string
  errorMessage?: string
  executedAt?: string
  createdAt: string
  updatedAt: string
  steps?: AnalysisStep[]
  dataSources?: AnalysisDataSourceRef[]
}

export interface AnalysisStep {
  id: number
  taskId: number
  stepOrder: number
  stepType: AnalysisStepType
  stepName: string
  config: string // JSON string
  result?: string // JSON string
  status: AnalysisStepStatus
}

export interface AnalysisDataSourceRef {
  id: number
  taskId: number
  dataSourceId: number
  alias: string
  tableName?: string
  querySql?: string
  dataSourceName?: string
}

export interface AnalysisResult {
  columns: string[]
  rows: Record<string, unknown>[]
  summary?: Record<string, unknown>
  stepResults: { stepId: number; stepName: string; result: unknown }[]
}

// 步骤配置类型
export interface CleanConfig {
  removeNull: boolean
  nullStrategy: 'DROP_ROW' | 'FILL_DEFAULT'
  fillValues?: Record<string, unknown>
  dedup: boolean
  dedupFields?: string[]
  typeConversion?: Record<string, string>
  outlierRemoval?: { field: string; method: 'IQR' | 'Z_SCORE'; threshold: number }
}

export interface FilterCondition {
  field: string
  operator: string
  value: unknown
}

export interface FilterConfig {
  conditions: FilterCondition[]
  logic: 'AND' | 'OR'
  selectFields?: string[]
  orderBy?: { field: string; direction: 'ASC' | 'DESC' }
  limit?: number
}

export interface StatsConfig {
  fields: string[]
  methods: string[]
}

export interface AdvancedStatsConfig {
  type: 'CORRELATION' | 'GROUP_BY' | 'CROSS_TAB'
  fields?: string[]
  groupBy?: string[]
  aggregations?: { field: string; function: string }[]
  rowField?: string
  colField?: string
  valueField?: string
  function?: string
}

export interface MLConfig {
  algorithm: 'LINEAR_REGRESSION' | 'KMEANS' | 'DECISION_TREE'
  features?: string[]
  target?: string
  fields?: string[]
  k?: number
  maxIterations?: number
}

export interface AnalysisTaskCreateRequest {
  name: string
  description?: string
  config?: string
}

export interface AnalysisTaskQuery {
  keyword?: string
  page?: number
  size?: number
}

export interface AddDataSourceRequest {
  dataSourceId: number
  alias: string
  tableName?: string
  querySql?: string
}

export interface AddStepRequest {
  stepType: string
  stepName: string
  config: string
  stepOrder: number
}

export interface UpdateStepRequest {
  stepType?: string
  stepName?: string
  config?: string
  stepOrder?: number
}

export interface SaveAsIndicatorRequest {
  name: string
  description?: string
  type?: string
  valueType?: string
  tags?: string
}
