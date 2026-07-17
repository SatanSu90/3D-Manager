export type DataSourceType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'SQLSERVER' | 'DM'
export type DataSourceStatus = 'ACTIVE' | 'ERROR' | 'DISABLED'
export type DataSourceVisibility = 'PRIVATE' | 'DEPARTMENT_SHARED' | 'PUBLIC'

export interface DataSource {
  id: number
  name: string
  type: DataSourceType
  host: string
  port: number
  databaseName: string
  username: string
  visibility: DataSourceVisibility
  status: DataSourceStatus
  sslEnabled: boolean
  poolConfig: string | null
  lastTestTime: string | null
  lastTestResult: boolean | null
  ownerId: number
  ownerName: string
  createdAt: string
  updatedAt: string
}

export interface DataSourceCreateRequest {
  name: string
  type: DataSourceType
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  poolConfig?: string
  sslEnabled?: boolean
  visibility?: DataSourceVisibility
}

export interface DataSourceTestRequest {
  type: DataSourceType
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  sslEnabled?: boolean
}

export interface ConnectionTestResult {
  success: boolean
  message: string
  databaseProductName?: string
  databaseProductVersion?: string
}

export interface TableInfo {
  name: string
  type: string
  remarks: string | null
}

export interface ColumnInfo {
  name: string
  type: string
  size: number
  nullable: boolean
  remarks: string | null
  defaultValue: string | null
}

export interface DataPreviewResult {
  columns: string[]
  rows: Record<string, unknown>[]
  total: number
}
