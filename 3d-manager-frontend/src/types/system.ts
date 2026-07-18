export interface Role {
  id: number
  name: string
  code: string
  description: string
  permissions: string[]
  isSystem: boolean
  userCount?: number
  createdAt: string
}

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'
  description: string
  category: string
  isEditable: boolean
  createdAt?: string
  updatedAt?: string
}

export interface OperationLog {
  id: number
  userId: number | null
  username: string | null
  module: string
  action: string
  targetType: string | null
  targetId: string | null
  description: string | null
  requestUrl: string | null
  requestMethod: string | null
  ipAddress: string | null
  status: 'SUCCESS' | 'FAILED'
  errorMessage: string | null
  durationMs: number | null
  createdAt: string
}

export interface OperationLogPage {
  items: OperationLog[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface RoleCreateRequest {
  name: string
  code: string
  description?: string
  permissions?: string[]
}

export interface SystemConfigUpdateRequest {
  configValue?: string
  configType?: string
  description?: string
  category?: string
  isEditable?: boolean
  items?: Record<string, string>
}

// 权限选项定义
export interface PermissionOption {
  value: string
  label: string
  group: string
}

export const PERMISSION_OPTIONS: PermissionOption[] = [
  // 模型
  { value: 'model:read', label: '查看模型', group: '模型管理' },
  { value: 'model:create', label: '创建模型', group: '模型管理' },
  { value: 'model:update', label: '更新模型', group: '模型管理' },
  { value: 'model:delete', label: '删除模型', group: '模型管理' },
  { value: 'model:*', label: '模型全部权限', group: '模型管理' },
  // 场景
  { value: 'scene:read', label: '查看场景', group: '场景应用' },
  { value: 'scene:create', label: '创建场景', group: '场景应用' },
  { value: 'scene:update', label: '更新场景', group: '场景应用' },
  { value: 'scene:delete', label: '删除场景', group: '场景应用' },
  { value: 'scene:*', label: '场景全部权限', group: '场景应用' },
  // 数据源
  { value: 'datasource:read', label: '查看数据源', group: '数据中心' },
  { value: 'datasource:create', label: '创建数据源', group: '数据中心' },
  { value: 'datasource:update', label: '更新数据源', group: '数据中心' },
  { value: 'datasource:delete', label: '删除数据源', group: '数据中心' },
  { value: 'datasource:*', label: '数据源全部权限', group: '数据中心' },
  // 数据分析
  { value: 'analysis:read', label: '查看分析', group: '数据分析' },
  { value: 'analysis:create', label: '创建分析', group: '数据分析' },
  { value: 'analysis:update', label: '更新分析', group: '数据分析' },
  { value: 'analysis:delete', label: '删除分析', group: '数据分析' },
  { value: 'analysis:*', label: '分析全部权限', group: '数据分析' },
  // 指标
  { value: 'indicator:read', label: '查看指标', group: '指标管理' },
  { value: 'indicator:*', label: '指标全部权限', group: '指标管理' },
  // 用户管理
  { value: 'user:read', label: '查看用户', group: '系统管理' },
  { value: 'user:*', label: '用户全部权限', group: '系统管理' },
  // 全部
  { value: '*', label: '超级管理员', group: '全局' },
]
