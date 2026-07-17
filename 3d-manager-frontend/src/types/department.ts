export interface Department {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  children?: Department[]
  users?: DepartmentUser[]
}

export interface DepartmentUser {
  id: number
  username: string
  role: string
  avatar: string | null
}

export interface DepartmentCreateRequest {
  name: string
  parentId?: number | null
  sortOrder?: number
}
