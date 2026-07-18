<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ShieldCheck, Plus, Pencil, Trash2, Users, X, Check } from 'lucide-vue-next'
import { getRoles, createRole, updateRole, deleteRole, getRoleUsers, assignRole, removeRole } from '@/api/system'
import { getUsers } from '@/api/user'
import type { Role, RoleCreateRequest } from '@/types/system'
import { PERMISSION_OPTIONS } from '@/types/system'

const roles = ref<Role[]>([])
const loading = ref(true)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<RoleCreateRequest>({ name: '', code: '', description: '', permissions: [] })

const showUsers = ref(false)
const viewingRole = ref<Role | null>(null)
const roleUsers = ref<Array<{ id: number; username: string; role: string; email: string; phone: string; status: string }>>([])
const allUsers = ref<{ id: number; username: string; role: string }[]>([])
const selectedUserIds = ref<number[]>([])

// 权限按组分类
const permissionsByGroup = computed(() => {
  const groups: Record<string, typeof PERMISSION_OPTIONS> = {}
  for (const opt of PERMISSION_OPTIONS) {
    if (!groups[opt.group]) groups[opt.group] = []
    groups[opt.group].push(opt)
  }
  return groups
})

onMounted(loadRoles)

async function loadRoles() {
  loading.value = true
  try {
    const res = await getRoles()
    roles.value = res.data.data ?? []
  } catch (e: unknown) {
    console.error('加载角色失败', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  formData.value = { name: '', code: '', description: '', permissions: [] }
  showForm.value = true
}

function openEdit(role: Role) {
  editingId.value = role.id
  formData.value = {
    name: role.name,
    code: role.code,
    description: role.description,
    permissions: [...role.permissions],
  }
  showForm.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await updateRole(editingId.value, formData.value)
    } else {
      await createRole(formData.value)
    }
    showForm.value = false
    await loadRoles()
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleDelete(role: Role) {
  if (role.isSystem) {
    alert('系统内置角色不可删除')
    return
  }
  if (!confirm(`确定删除角色「${role.name}」？此操作不可撤销。`)) return
  try {
    await deleteRole(role.id)
    await loadRoles()
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function openUsers(role: Role) {
  viewingRole.value = role
  showUsers.value = true
  selectedUserIds.value = []
  try {
    const [usersRes, allUsersRes] = await Promise.all([getRoleUsers(role.id), getUsers()])
    roleUsers.value = usersRes.data.data ?? []
    allUsers.value = (allUsersRes.data.data ?? []).map((u) => ({ id: u.id, username: u.username, role: u.role }))
  } catch (e: unknown) {
    console.error('加载用户失败', e)
  }
}

async function handleAssignUser(userId: number) {
  if (!viewingRole.value) return
  try {
    await assignRole(viewingRole.value.id, userId)
    const res = await getRoleUsers(viewingRole.value.id)
    roleUsers.value = res.data.data ?? []
    selectedUserIds.value = selectedUserIds.value.filter((id) => id !== userId)
  } catch (e: unknown) {
    alert('分配失败: ' + (e as Error).message)
  }
}

async function handleRemoveUser(userId: number) {
  if (!viewingRole.value) return
  if (!confirm('确定从该角色中移除此用户？')) return
  try {
    await removeRole(viewingRole.value.id, userId)
    roleUsers.value = roleUsers.value.filter((u) => u.id !== userId)
  } catch (e: unknown) {
    alert('移除失败: ' + (e as Error).message)
  }
}

function togglePermission(value: string) {
  const idx = formData.value.permissions?.indexOf(value) ?? -1
  if (idx >= 0) {
    formData.value.permissions?.splice(idx, 1)
  } else {
    formData.value.permissions?.push(value)
  }
}

function isPermissionSelected(value: string): boolean {
  return formData.value.permissions?.includes(value) ?? false
}

function codeLabel(code: string): string {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    EDITOR: '编辑者',
    VIEWER: '查看者',
  }
  return map[code] || code
}

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <ShieldCheck :size="24" class="text-primary-light" />
          角色权限管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">管理系统角色及其权限配置</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新建角色
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="roles.length === 0" class="text-center text-gray-500 py-20">
      <ShieldCheck :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无角色</p>
      <p class="text-sm">点击上方按钮创建角色</p>
    </div>

    <div v-else class="glass-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-primary/10">
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">名称</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">编码</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">描述</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">权限数</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">用户数</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">创建时间</th>
            <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="role in roles"
            :key="role.id"
            class="border-b border-primary/5 hover:bg-primary/5 transition-colors"
          >
            <td class="px-4 py-3">
              <div class="text-sm text-white font-medium flex items-center gap-2">
                {{ role.name }}
                <span v-if="role.isSystem" class="px-1.5 py-0.5 rounded text-xs bg-primary/10 text-primary-light">系统</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs bg-primary/10 text-primary-light font-mono">{{ role.code }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ role.description || '—' }}</td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ role.permissions.length }}</td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ role.userCount ?? 0 }}</td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(role.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex justify-end gap-1">
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-green-400 transition-colors"
                  title="用户分配"
                  @click="openUsers(role)"
                >
                  <Users :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                  title="编辑"
                  @click="openEdit(role)"
                >
                  <Pencil :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-danger transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                  title="删除"
                  :disabled="role.isSystem"
                  @click="handleDelete(role)"
                >
                  <Trash2 :size="14" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建/编辑角色弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑角色' : '新建角色' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">角色名称 *</label>
                <input v-model="formData.name" type="text" placeholder="如：编辑者"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">角色编码 *</label>
                <input
                  v-model="formData.code" type="text" placeholder="如：EDITOR"
                  :disabled="!!editingId"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none font-mono disabled:opacity-60 disabled:cursor-not-allowed" />
                <p v-if="editingId" class="text-xs text-gray-600 mt-1">编码不可修改</p>
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <textarea v-model="formData.description" rows="2" placeholder="角色描述"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none resize-none" />
            </div>

            <div>
              <label class="block text-sm text-gray-400 mb-2">权限分配</label>
              <div class="space-y-3 max-h-72 overflow-y-auto p-3 rounded-lg bg-dark-surface/30 border border-primary/10">
                <div v-for="(options, group) in permissionsByGroup" :key="group">
                  <div class="text-xs text-gray-500 mb-1.5 uppercase tracking-wider">{{ group }}</div>
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="opt in options"
                      :key="opt.value"
                      type="button"
                      class="px-2.5 py-1 rounded-md text-xs transition-all flex items-center gap-1"
                      :class="isPermissionSelected(opt.value)
                        ? 'bg-primary/20 text-primary-light border border-primary/40'
                        : 'bg-dark-surface/50 text-gray-400 border border-primary/10 hover:border-primary/30'"
                      @click="togglePermission(opt.value)"
                    >
                      <Check v-if="isPermissionSelected(opt.value)" :size="10" />
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>
              <p class="text-xs text-gray-600 mt-2">
                已选 {{ formData.permissions?.length ?? 0 }} 项权限。
                <span v-if="formData.permissions?.includes('*')" class="text-primary-light">超级管理员拥有全部权限</span>
              </p>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formData.name.trim() || !formData.code.trim()" @click="handleSubmit">
              {{ editingId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 用户分配弹窗 -->
    <Teleport to="body">
      <div v-if="showUsers" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showUsers = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Users :size="18" class="text-primary-light" />
              {{ viewingRole?.name }} - 用户分配
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showUsers = false"><X :size="18" /></button>
          </div>

          <div class="mb-6">
            <h4 class="text-sm text-gray-400 mb-2">当前用户 ({{ roleUsers.length }})</h4>
            <div v-if="roleUsers.length === 0" class="text-sm text-gray-600 py-2">暂无用户</div>
            <div v-else class="space-y-1">
              <div
                v-for="u in roleUsers"
                :key="u.id"
                class="flex items-center gap-3 px-3 py-2 rounded-lg bg-dark-surface/30"
              >
                <div class="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-xs text-primary-light">
                  {{ u.username.charAt(0).toUpperCase() }}
                </div>
                <div class="flex-1">
                  <div class="text-sm text-white">{{ u.username }}</div>
                  <div class="text-xs text-gray-500">{{ codeLabel(u.role) }}</div>
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

          <div class="border-t border-primary/10 pt-4">
            <h4 class="text-sm text-gray-400 mb-2">添加用户</h4>
            <div class="space-y-1 max-h-48 overflow-y-auto">
              <div
                v-for="u in allUsers.filter(au => !roleUsers.some(ru => ru.id === au.id))"
                :key="u.id"
                class="flex items-center gap-3 px-3 py-1.5 rounded-lg hover:bg-primary/5"
              >
                <div class="w-7 h-7 rounded-full bg-primary/15 flex items-center justify-center text-xs text-primary-light">
                  {{ u.username.charAt(0).toUpperCase() }}
                </div>
                <span class="text-sm text-gray-300 flex-1">{{ u.username }}</span>
                <span class="text-xs text-gray-600">{{ codeLabel(u.role) }}</span>
                <button
                  class="p-1 rounded text-gray-500 hover:text-primary-light transition-colors"
                  title="添加"
                  @click="handleAssignUser(u.id)"
                >
                  <Plus :size="14" />
                </button>
              </div>
              <div
                v-if="allUsers.filter(au => !roleUsers.some(ru => ru.id === au.id)).length === 0"
                class="text-sm text-gray-600 text-center py-3"
              >
                所有用户已分配
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
