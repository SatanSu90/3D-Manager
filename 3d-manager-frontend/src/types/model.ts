export interface Model {
  id: number
  name: string
  description: string
  fileKey: string
  thumbnailKey: string
  thumbnailUrl: string
  fileSize: number
  format: 'glb' | 'gltf'
  categoryId: number | null
  categoryName: string | null
  tags: Tag[]
  uploaderId: number
  uploaderName: string
  downloadCount: number
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  children?: Category[]
}

export interface Tag {
  id: number
  name: string
}

export interface ModelQuery {
  keyword?: string
  categoryId?: number
  tagIds?: number[]
  page: number
  size: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface ModelUpload {
  name: string
  description: string
  categoryId: number | null
  tagIds: number[]
  file: File
  thumbnail: Blob
}
