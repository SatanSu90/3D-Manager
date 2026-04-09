<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, ArrowLeft, FolderTree } from 'lucide-vue-next'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

interface CategoryItem {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
}

const categories = ref<CategoryItem[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const formName = ref('')
const formSortOrder = ref(0)

onMounted(loadCategories)

function loadCategories() {
  loading.value = true
  getCategories()
    .then((res) => { categories.value = res.data.data ?? [] })
    .catch((e) => console.error('加载分类失败', e))
    .finally(() => { loading.value = false })
}

function openCreate() {
  editingId.value = null
  formName.value = ''
  formSortOrder.value = categories.value.length + 1
  showForm.value = true
}

function openEdit(cat: CategoryItem) {
  editingId.value = cat.id
  formName.value = cat.name
  formSortOrder.value = cat.sortOrder
  showForm.value = true
}

function handleSubmit() {
  if (!formName.value.trim()) return
  const data = { name: formName.value.trim(), parentId: null, sortOrder: formSortOrder.value }
  const action = editingId.value
    ? updateCategory(editingId.value, data)
    : createCategory(data)

  action
    .then(() => {
      showForm.value = false
      loadCategories()
    })
    .catch((e) => alert('保存失败: ' + (e.response?.data?.message || e.message)))
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <button class="text-gray-400 hover:text-white transition-colors" @click="$router.back()">
          <ArrowLeft :size="20" />
        </button>
        <div>
          <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
            <FolderTree :size="24" class="text-primary-light" />
            分类管理
          </h1>
          <p class="text-sm text-gray-500 mt-1">共 {{ categories.length }} 个分类</p>
        </div>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新增分类
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="categories.length === 0" class="text-center text-gray-500 py-20">
      <p class="text-lg mb-2">暂无分类</p>
      <p class="text-sm">点击上方按钮添加第一个分类</p>
    </div>

    <div v-else class="glass-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-primary/10 text-xs text-gray-500">
            <th class="text-left px-6 py-3 font-medium">排序</th>
            <th class="text-left px-6 py-3 font-medium">名称</th>
            <th class="text-right px-6 py-3 font-medium pr-4">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="cat in categories"
            :key="cat.id"
            class="border-b border-primary/5 hover:bg-white/5 transition-colors group"
          >
            <td class="px-6 py-3 text-sm text-gray-500">{{ cat.sortOrder }}</td>
            <td class="px-6 py-3 text-sm text-white">{{ cat.name }}</td>
            <td class="px-6 py-3 pr-4">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  class="p-1.5 rounded-lg text-gray-400 hover:text-primary-light hover:bg-primary/10 transition-all"
                  title="编辑"
                  @click="openEdit(cat)"
                >
                  <Pencil :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-lg text-gray-400 hover:text-danger hover:bg-danger/10 transition-all"
                  title="删除"
                  @click="deleteCategory(cat.id).then(loadCategories).catch((e) => alert('删除失败: ' + (e.response?.data?.message || e.message)))"
                >
                  <Trash2 :size="14" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-sm mx-4 p-6 animate-slide-up">
          <h3 class="text-lg font-semibold text-white mb-4">{{ editingId ? '编辑分类' : '新增分类' }}</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">名称</label>
              <input
                v-model="formName"
                type="text"
                placeholder="分类名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
                @keyup.enter="handleSubmit"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">排序</label>
              <input
                v-model.number="formSortOrder"
                type="number"
                min="1"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none transition-all"
              />
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formName.trim()" @click="handleSubmit">
              保存
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
