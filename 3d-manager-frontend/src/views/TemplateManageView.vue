<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { LayoutTemplate, Plus, Pencil, Trash2, Search, X, Sparkles, Check } from 'lucide-vue-next'
import {
  getTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  applyTemplate,
} from '@/api/template'
import type { Template, TemplateCreateRequest, TemplateCategory } from '@/types/report'
import type { PageData } from '@/types/api'

const templates = ref<Template[]>([])
const loading = ref(true)
const keyword = ref('')
const categoryFilter = ref<TemplateCategory | ''>('')

const currentPage = ref(0)
const pageSize = ref(20)
const total = ref(0)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<TemplateCreateRequest>(getEmptyForm())

const categoryOptions: { value: TemplateCategory; label: string; color: string }[] = [
  { value: 'SCENE', label: '场景', color: 'bg-blue-500/20 text-blue-400' },
  { value: 'REPORT', label: '报表', color: 'bg-purple-500/20 text-purple-400' },
  { value: 'DASHBOARD', label: '仪表盘', color: 'bg-cyan-500/20 text-cyan-400' },
]

function getEmptyForm(): TemplateCreateRequest {
  return {
    name: '',
    description: '',
    category: 'SCENE',
    config: '{}',
    isOfficial: false,
  }
}

onMounted(loadTemplates)

function loadTemplates() {
  loading.value = true
  getTemplates({
    keyword: keyword.value || undefined,
    category: categoryFilter.value || undefined,
    page: currentPage.value,
    size: pageSize.value,
  })
    .then((res) => {
      const data = res.data.data as PageData<Template>
      templates.value = data.content ?? []
      total.value = data.totalElements ?? 0
    })
    .catch((e) => {
      console.error('加载模板失败', e)
      alert('加载模板失败: ' + (e as Error).message)
    })
    .finally(() => { loading.value = false })
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function openCreate() {
  editingId.value = null
  formData.value = getEmptyForm()
  showForm.value = true
}

function openEdit(template: Template) {
  editingId.value = template.id
  formData.value = {
    name: template.name,
    description: template.description ?? '',
    category: template.category,
    config: template.config,
    previewImage: template.previewImage ?? '',
    isOfficial: template.isOfficial,
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    alert('请输入模板名称')
    return
  }
  if (!formData.value.config) {
    formData.value.config = '{}'
  }
  try {
    if (editingId.value) {
      await updateTemplate(editingId.value, formData.value)
    } else {
      await createTemplate(formData.value)
    }
    showForm.value = false
    loadTemplates()
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleDelete(template: Template) {
  if (!confirm(`确定删除模板「${template.name}」？此操作不可撤销。`)) return
  try {
    await deleteTemplate(template.id)
    loadTemplates()
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function handleApply(template: Template) {
  if (!confirm(`应用模板「${template.name}」？将复制其配置到剪贴板供您创建新对象使用。`)) return
  try {
    const res = await applyTemplate(template.id)
    const result = res.data.data
    // 复制 config 到剪贴板
    try {
      await navigator.clipboard.writeText(result.config)
      alert(`模板「${result.templateName}」配置已复制到剪贴板，可在新建场景/报表时粘贴使用。`)
    } catch {
      alert(`模板「${result.templateName}」已应用，使用次数 +1`)
    }
    loadTemplates()
  } catch (e: unknown) {
    alert('应用失败: ' + (e as Error).message)
  }
}

function applyCategoryFilter(value: TemplateCategory | '') {
  categoryFilter.value = value
  currentPage.value = 0
  loadTemplates()
}

function onSearchInput() {
  currentPage.value = 0
  loadTemplates()
}

function setPage(p: number) {
  currentPage.value = p
  loadTemplates()
}

function categoryInfo(category: TemplateCategory) {
  return categoryOptions.find((o) => o.value === category) || categoryOptions[0]
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  })
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <LayoutTemplate :size="24" class="text-primary-light" />
          模板管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ total }} 个模板</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新建模板
      </button>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="flex flex-wrap gap-3 mb-6">
      <div class="relative flex-1 min-w-[240px] max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索模板..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          @input="onSearchInput"
        />
      </div>
      <select
        :value="categoryFilter"
        @change="applyCategoryFilter(($event.target as HTMLSelectElement).value as TemplateCategory | '')"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
      >
        <option value="">全部分类</option>
        <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="templates.length === 0" class="text-center text-gray-500 py-20">
      <LayoutTemplate :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无模板</p>
      <p class="text-sm">点击上方按钮创建你的第一个模板</p>
    </div>

    <!-- 卡片网格 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="template in templates"
        :key="template.id"
        class="glass-card hover-glow group overflow-hidden"
      >
        <div class="aspect-video relative overflow-hidden bg-dark-surface/30 flex items-center justify-center">
          <img
            v-if="template.previewImage"
            :src="template.previewImage"
            :alt="template.name"
            class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          <LayoutTemplate v-else :size="48" class="text-gray-600" />

          <span
            class="absolute top-2 left-2 px-2 py-0.5 rounded-md text-[10px]"
            :class="categoryInfo(template.category).color"
          >
            {{ categoryInfo(template.category).label }}
          </span>
          <span
            v-if="template.isOfficial"
            class="absolute top-2 right-2 px-2 py-0.5 rounded-md text-[10px] bg-yellow-500/20 text-yellow-400 flex items-center gap-1"
          >
            <Sparkles :size="10" />
            官方
          </span>

          <div class="absolute bottom-2 right-2 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-all">
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-green-400 transition-colors"
              title="应用模板"
              @click="handleApply(template)"
            >
              <Check :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-primary-light transition-colors"
              title="编辑"
              @click="openEdit(template)"
            >
              <Pencil :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-danger transition-colors"
              title="删除"
              @click="handleDelete(template)"
            >
              <Trash2 :size="14" />
            </button>
          </div>
        </div>

        <div class="p-3">
          <h3 class="text-sm font-medium text-white truncate">{{ template.name }}</h3>
          <p class="text-xs text-gray-500 mt-1 line-clamp-2 min-h-[1.5rem]">
            {{ template.description || '—' }}
          </p>
          <div class="flex items-center justify-between mt-2 text-[10px] text-gray-600">
            <span>使用 {{ template.useCount }} 次</span>
            <span>{{ formatDate(template.updatedAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
      <button
        v-for="p in totalPages"
        :key="p"
        class="px-3 py-1 rounded text-sm transition-all"
        :class="p - 1 === currentPage ? 'bg-primary/20 text-primary-light' : 'text-gray-500 hover:bg-primary/5'"
        @click="setPage(p - 1)"
      >{{ p }}</button>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑模板' : '新建模板' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">模板名称</label>
              <input v-model="formData.name" type="text" placeholder="请输入模板名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <textarea v-model="formData.description" rows="2" placeholder="可选"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none resize-none"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">分类</label>
                <select v-model="formData.category"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                  <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">预览图URL</label>
                <input v-model="formData.previewImage" type="text" placeholder="可选"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">模板配置 (JSON)</label>
              <textarea v-model="formData.config" rows="6" placeholder='{"layout": [], "components": []}'
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-xs text-gray-300 placeholder-gray-600 focus:border-primary/40 focus:outline-none font-mono"></textarea>
            </div>
            <label class="flex items-center gap-2 text-sm text-gray-400">
              <input type="checkbox" v-model="formData.isOfficial" class="accent-primary" />
              标记为官方模板
            </label>
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
  </div>
</template>
