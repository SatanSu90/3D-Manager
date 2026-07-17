<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Building2, Plus, Pencil, Trash2, ChevronRight, ChevronDown, Users, X, UserPlus } from 'lucide-vue-next'
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment, getDepartmentUsers, assignUsers, removeUser } from '@/api/department'
import { getUsers } from '@/api/user'
import type { Department, DepartmentCreateRequest } from '@/types/department'

const departments = ref<Department[]>([])
const loading = ref(true)
const expandedIds = ref<Set<number>>(new Set())

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<DepartmentCreateRequest>({ name: '', parentId: null, sortOrder: 0 })

const showUsers = ref(false)
const viewingDept = ref<Department | null>(null)
const deptUsers = ref<Department['users']>([])
const allUsers = ref<{ id: number; username: string; role: string }[]>([])
const selectedUserIds = ref<number[]>([])

onMounted(loadDepartments)

async function loadDepartments() {
  loading.value = true
  try {
    const res = await getDepartmentTree()
    departments.value = res.data.data ?? []
  } catch (e: unknown) {
    console.error('加载部门失败', e)
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function openCreate(parent?: Department) {
  editingId.value = null
  formData.value = { name: '', parentId: parent?.id ?? null, sortOrder: 0 }
  showForm.value = true
}

function openEdit(dept: Department) {
  editingId.value = dept.id
  formData.value = { name: dept.name, parentId: dept.parentId, sortOrder: dept.sortOrder }
  showForm.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await updateDepartment(editingId.value, formData.value)
    } else {
      await createDepartment(formData.value)
    }
    showForm.value = false
    await loadDepartments()
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleDelete(dept: Department) {
  if (!confirm(`确定删除部门「${dept.name}」？子部门也会被删除。`)) return
  try {
    await deleteDepartment(dept.id)
    await loadDepartments()
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function openUsers(dept: Department) {
  viewingDept.value = dept
  showUsers.value = true
  selectedUserIds.value = []
  try {
    const [usersRes, allUsersRes] = await Promise.all([getDepartmentUsers(dept.id), getUsers()])
    deptUsers.value = usersRes.data.data ?? []
    allUsers.value = (allUsersRes.data.data ?? []).map((u) => ({ id: u.id, username: u.username, role: u.role }))
  } catch (e: unknown) {
    console.error('加载用户失败', e)
  }
}

async function handleAssignUsers() {
  if (!viewingDept.value || selectedUserIds.value.length === 0) return
  try {
    await assignUsers(viewingDept.value.id, selectedUserIds.value)
    selectedUserIds.value = []
    const res = await getDepartmentUsers(viewingDept.value.id)
    deptUsers.value = res.data.data ?? []
  } catch (e: unknown) {
    alert('分配失败: ' + (e as Error).message)
  }
}

async function handleRemoveUser(userId: number) {
  if (!viewingDept.value) return
  if (!confirm('确定从该部门移除此用户？')) return
  try {
    await removeUser(viewingDept.value.id, userId)
    deptUsers.value = (deptUsers.value ?? []).filter((u) => u.id !== userId)
  } catch (e: unknown) {
    alert('移除失败: ' + (e as Error).message)
  }
}

function isExpanded(id: number) {
  return expandedIds.value.has(id)
}

// 递归渲染树
function flattenTree(depts: Department[], level = 0): { dept: Department; level: number }[] {
  const result: { dept: Department; level: number }[] = []
  for (const d of depts) {
    result.push({ dept: d, level })
    if (d.children?.length && isExpanded(d.id)) {
      result.push(...flattenTree(d.children, level + 1))
    }
  }
  return result
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <Building2 :size="24" class="text-primary-light" />
          部门管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">管理部门组织架构</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate()">
        <Plus :size="16" />
        新建部门
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="departments.length === 0" class="text-center text-gray-500 py-20">
      <Building2 :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无部门</p>
      <p class="text-sm">点击上方按钮创建部门组织架构</p>
    </div>

    <!-- 部门树 -->
    <div v-else class="glass-card p-4">
      <div class="space-y-0.5">
        <template v-for="{ dept, level } in flattenTree(departments)" :key="dept.id">
          <div
            class="flex items-center gap-2 rounded-lg transition-all group hover:bg-primary/5"
            :style="{ paddingLeft: `${level * 24 + 12}px` }"
          >
            <button
              v-if="dept.children?.length"
              class="p-1 text-gray-500 hover:text-primary-light"
              @click="toggleExpand(dept.id)"
            >
              <ChevronDown v-if="isExpanded(dept.id)" :size="14" />
              <ChevronRight v-else :size="14" />
            </button>
            <div v-else class="w-7" />

            <Building2 :size="14" class="text-primary-light shrink-0" />
            <span class="text-sm text-white flex-1 truncate">{{ dept.name }}</span>
            <span v-if="dept.users?.length" class="text-xs text-gray-500">{{ dept.users.length }} 人</span>

            <!-- 操作按钮 -->
            <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                class="p-1.5 rounded hover:bg-primary/10 text-gray-400 hover:text-green-400 transition-colors"
                title="添加子部门"
                @click="openCreate(dept)"
              >
                <Plus :size="13" />
              </button>
              <button
                class="p-1.5 rounded hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                title="管理部门用户"
                @click="openUsers(dept)"
              >
                <Users :size="13" />
              </button>
              <button
                class="p-1.5 rounded hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                title="编辑"
                @click="openEdit(dept)"
              >
                <Pencil :size="13" />
              </button>
              <button
                class="p-1.5 rounded hover:bg-primary/10 text-gray-400 hover:text-danger transition-colors"
                title="删除"
                @click="handleDelete(dept)"
              >
                <Trash2 :size="13" />
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-sm mx-4 p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑部门' : '新建部门' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>
          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">部门名称</label>
              <input v-model="formData.name" type="text" placeholder="部门名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">排序</label>
              <input v-model.number="formData.sortOrder" type="number"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none" />
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formData.name.trim()" @click="handleSubmit">
              {{ editingId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 用户管理弹窗 -->
    <Teleport to="body">
      <div v-if="showUsers" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showUsers = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Users :size="18" class="text-primary-light" />
              {{ viewingDept?.name }} - 成员管理
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showUsers = false"><X :size="18" /></button>
          </div>

          <!-- 当前成员 -->
          <div class="mb-6">
            <h4 class="text-sm text-gray-400 mb-2">当前成员 ({{ deptUsers?.length ?? 0 }})</h4>
            <div v-if="(deptUsers?.length ?? 0) === 0" class="text-sm text-gray-600 py-2">暂无成员</div>
            <div v-else class="space-y-1">
              <div
                v-for="u in deptUsers"
                :key="u.id"
                class="flex items-center gap-3 px-3 py-2 rounded-lg bg-dark-surface/30"
              >
                <div class="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-xs text-primary-light">
                  {{ u.username.charAt(0).toUpperCase() }}
                </div>
                <div class="flex-1">
                  <div class="text-sm text-white">{{ u.username }}</div>
                  <div class="text-xs text-gray-500">{{ u.role }}</div>
                </div>
                <button
                  class="p-1 rounded text-gray-500 hover:text-danger transition-colors"
                  title="移除"
                  @click="handleRemoveUser(u.id)"
                >
                  <X :size="14" />
                </button>
              </div>
            </div>
          </div>

          <!-- 添加成员 -->
          <div class="border-t border-primary/10 pt-4">
            <h4 class="text-sm text-gray-400 mb-2 flex items-center gap-2">
              <UserPlus :size="14" />
              添加成员
            </h4>
            <div class="space-y-1 max-h-48 overflow-y-auto">
              <label
                v-for="u in allUsers.filter(au => !(deptUsers ?? []).some(du => du.id === au.id))"
                :key="u.id"
                class="flex items-center gap-3 px-3 py-1.5 rounded-lg hover:bg-primary/5 cursor-pointer"
              >
                <input type="checkbox" :value="u.id" v-model="selectedUserIds" class="accent-primary" />
                <span class="text-sm text-gray-300">{{ u.username }}</span>
                <span class="text-xs text-gray-600">{{ u.role }}</span>
              </label>
            </div>
            <button
              v-if="selectedUserIds.length > 0"
              class="mt-3 w-full btn-primary text-sm"
              @click="handleAssignUsers"
            >
              添加选中的 {{ selectedUserIds.length }} 个用户
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
