<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { getUsers, updateUserRole, deleteUser } from '@/api/user'
import { getCategories, createCategory, deleteCategory } from '@/api/category'
import { getTags, createTag, deleteTag } from '@/api/tag'
import type { Category, Tag } from '@/types/model'
import { Users, FolderTree, Tag as TagIcon, Plus, Trash2, Shield, UserCircle } from 'lucide-vue-next'

const authStore = useAuthStore()
const router = useRouter()
const activeTab = ref('users')

interface UserItem {
  id: number
  username: string
  role: string
  createdAt: string
}

const users = ref<UserItem[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const newCategoryName = ref('')
const newCategoryParent = ref<number | null>(null)
const newTagName = ref('')

onMounted(() => {
  if (!authStore.isAdmin) {
    router.push('/')
    return
  }
  loadUsers()
  loadCategories()
  loadTags()
})

async function loadUsers() {
  try {
    const res = await getUsers()
    users.value = res.data.data ?? []
  } catch (e) {
    console.error('加载用户列表失败:', e)
  }
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data.data ?? []
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

async function loadTags() {
  try {
    const res = await getTags()
    tags.value = res.data.data ?? []
  } catch (e) {
    console.error('加载标签失败:', e)
  }
}

async function handleToggleRole(user: UserItem) {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await updateUserRole(user.id, newRole)
    user.role = newRole
  } catch (e) {
    console.error('更新角色失败:', e)
  }
}

async function handleDeleteUser(id: number) {
  if (!confirm('确定删除此用户？')) return
  try {
    await deleteUser(id)
    users.value = users.value.filter((u) => u.id !== id)
  } catch (e) {
    console.error('删除用户失败:', e)
  }
}

async function handleAddCategory() {
  if (!newCategoryName.value.trim()) return
  try {
    await createCategory({ name: newCategoryName.value, parentId: newCategoryParent.value, sortOrder: 0 })
    newCategoryName.value = ''
    loadCategories()
  } catch (e) {
    console.error('添加分类失败:', e)
  }
}

async function handleDeleteCategory(id: number) {
  if (!confirm('确定删除此分类？')) return
  try {
    await deleteCategory(id)
    categories.value = categories.value.filter((c) => c.id !== id)
  } catch (e) {
    console.error('删除分类失败:', e)
  }
}

async function handleAddTag() {
  if (!newTagName.value.trim()) return
  try {
    await createTag({ name: newTagName.value })
    newTagName.value = ''
    loadTags()
  } catch (e) {
    console.error('添加标签失败:', e)
  }
}

async function handleDeleteTag(id: number) {
  if (!confirm('确定删除此标签？')) return
  try {
    await deleteTag(id)
    tags.value = tags.value.filter((t) => t.id !== id)
  } catch (e) {
    console.error('删除标签失败:', e)
  }
}
</script>

<template>
  <div class="p-6">
    <h1 class="text-2xl font-semibold text-white mb-6">管理后台</h1>

    <div class="flex gap-2 mb-6">
      <button
        v-for="tab in [
          { key: 'users', label: '用户管理', icon: Users },
          { key: 'categories', label: '分类管理', icon: FolderTree },
          { key: 'tags', label: '标签管理', icon: TagIcon },
        ]"
        :key="tab.key"
        class="px-4 py-2 rounded-xl text-sm transition-all flex items-center gap-2"
        :class="activeTab === tab.key
          ? 'bg-primary/15 text-primary-light border border-primary/30'
          : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300 border border-transparent'"
        @click="activeTab = tab.key"
      >
        <component :is="tab.icon" :size="16" />
        {{ tab.label }}
      </button>
    </div>

    <!-- 用户管理 -->
    <div v-if="activeTab === 'users'" class="glass-card p-6">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-primary/10">
              <th class="text-left py-3 px-4 text-primary-light font-medium">用户名</th>
              <th class="text-left py-3 px-4 text-primary-light font-medium">角色</th>
              <th class="text-left py-3 px-4 text-primary-light font-medium">注册时间</th>
              <th class="text-right py-3 px-4 text-primary-light font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" class="border-b border-primary/5 hover:bg-primary/5 transition-colors">
              <td class="py-3 px-4 text-white flex items-center gap-2">
                <UserCircle :size="16" class="text-gray-500" />
                {{ user.username }}
              </td>
              <td class="py-3 px-4">
                <span
                  class="px-2 py-0.5 rounded-full text-xs"
                  :class="user.role === 'ADMIN' ? 'bg-primary/15 text-primary-light' : 'bg-dark-surface text-gray-400'"
                >
                  <Shield :size="10" class="inline mr-1" />
                  {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </span>
              </td>
              <td class="py-3 px-4 text-gray-400">{{ new Date(user.createdAt).toLocaleDateString('zh-CN') }}</td>
              <td class="py-3 px-4 text-right">
                <button
                  class="text-xs text-primary-light hover:text-primary-accent transition-colors mr-3"
                  @click="handleToggleRole(user)"
                >
                  切换角色
                </button>
                <button
                  class="text-xs text-gray-500 hover:text-danger transition-colors"
                  @click="handleDeleteUser(user.id)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分类管理 -->
    <div v-if="activeTab === 'categories'" class="glass-card p-6">
      <div class="flex gap-3 mb-4">
        <input
          v-model="newCategoryName"
          type="text"
          placeholder="新分类名称"
          class="flex-1 bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          @keyup.enter="handleAddCategory"
        />
        <button class="btn-primary text-sm flex items-center gap-1.5" @click="handleAddCategory">
          <Plus :size="14" /> 添加
        </button>
      </div>
      <div class="space-y-2">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="flex items-center justify-between px-4 py-2.5 rounded-lg hover:bg-primary/5 transition-colors"
        >
          <div class="flex items-center gap-2">
            <FolderTree :size="14" class="text-primary-light" />
            <span class="text-sm text-white">{{ cat.name }}</span>
          </div>
          <button class="text-gray-500 hover:text-danger transition-colors" @click="handleDeleteCategory(cat.id)">
            <Trash2 :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- 标签管理 -->
    <div v-if="activeTab === 'tags'" class="glass-card p-6">
      <div class="flex gap-3 mb-4">
        <input
          v-model="newTagName"
          type="text"
          placeholder="新标签名称"
          class="flex-1 bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          @keyup.enter="handleAddTag"
        />
        <button class="btn-primary text-sm flex items-center gap-1.5" @click="handleAddTag">
          <Plus :size="14" /> 添加
        </button>
      </div>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="tag in tags"
          :key="tag.id"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs bg-primary/10 text-primary-light border border-primary/15"
        >
          {{ tag.name }}
          <button class="hover:text-danger transition-colors" @click="handleDeleteTag(tag.id)">
            <Trash2 :size="10" />
          </button>
        </span>
      </div>
    </div>
  </div>
</template>
