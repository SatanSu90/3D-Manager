export type SceneVisibility = 'PRIVATE' | 'DEPARTMENT_SHARED' | 'PUBLIC'
export type SceneStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface Scene {
  id: number
  name: string
  sceneData: string
  thumbnailKey: string
  thumbnailUrl: string
  description: string | null
  creatorId: number
  creatorName: string
  categoryId: number | null
  categoryName: string | null
  ownerId: number
  ownerName: string
  visibility: SceneVisibility
  status: SceneStatus
  resolution: string | null
  previewPassword: string | null
  createdAt: string
  updatedAt: string
}

export interface SceneData {
  objects: SceneObject[]
  environment: SceneEnvironment
}

export interface SceneObject {
  id: string
  name: string
  type: 'model' | 'light' | 'camera'
  visible?: boolean
  modelRef?: number
  transform: {
    position: [number, number, number]
    rotation: [number, number, number]
    scale: [number, number, number]
  }
  material?: {
    color: string
    metalness: number
    roughness: number
    map?: string
  }
  light?: {
    type: 'ambient' | 'directional' | 'point' | 'spot'
    color: string
    intensity: number
    distance?: number
    angle?: number
  }
}

export interface SceneEnvironment {
  hdrFile?: string
  ambientIntensity: number
  backgroundColor: string
}
