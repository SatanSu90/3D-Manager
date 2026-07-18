export type ReportType = 'DASHBOARD' | 'TABLE' | 'CHART' | 'CUSTOM'
export type ReportStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type ReportVisibility = 'PRIVATE' | 'DEPARTMENT_SHARED' | 'PUBLIC'

export interface Report {
  id: number
  name: string
  description: string | null
  type: ReportType
  config: string | null
  sceneId: number | null
  status: ReportStatus
  ownerId: number
  ownerName: string | null
  visibility: ReportVisibility
  thumbnailKey: string | null
  createdAt: string
  updatedAt: string
}

export interface ReportCreateRequest {
  name: string
  description?: string
  type?: ReportType
  config?: string
  sceneId?: number
  status?: ReportStatus
  visibility?: ReportVisibility
  thumbnailKey?: string
}

export type TemplateCategory = 'SCENE' | 'REPORT' | 'DASHBOARD'

export interface Template {
  id: number
  name: string
  description: string | null
  category: TemplateCategory
  config: string
  previewImage: string | null
  isOfficial: boolean
  useCount: number
  ownerId: number
  ownerName: string | null
  createdAt: string
  updatedAt: string
}

export interface TemplateCreateRequest {
  name: string
  description?: string
  category?: TemplateCategory
  config: string
  previewImage?: string
  isOfficial?: boolean
}

export interface TemplateApplyResult {
  templateId: number
  templateName: string
  category: TemplateCategory
  config: string
  previewImage: string | null
}

export type ResourceType = 'IMAGE' | 'ICON' | 'TEXTURE' | 'MATERIAL' | 'AUDIO' | 'VIDEO'

export interface Resource {
  id: number
  name: string
  type: ResourceType
  fileKey: string
  fileUrl: string | null
  fileSize: number | null
  mimeType: string | null
  width: number | null
  height: number | null
  tags: string | null
  ownerId: number
  ownerName: string | null
  createdAt: string
  updatedAt: string
}

export interface ResourceBatchDeleteResult {
  success: number
  failed: number
  total: number
}
