<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { X, Search, Box } from 'lucide-vue-next'
import { getModels } from '@/api/model'
import { getModelUrl } from '@/api/model'
import { getCategoryTree, type CategoryTreeNode } from '@/api/category'
import type { Model } from '@/types/model'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'select', model: Model, url: string): void
}>()

const models = ref<Model[]>([])
const loading = ref(true)
const searchKeyword = ref('')
const selectedModel = ref<Model | null>(null)
const previewUrl = ref('')

// 分类相关
const categories = ref<CategoryTreeNode[]>([])
const activeCategoryId = ref<number | undefined>(undefined)

onMounted(async () => {
  // 并行加载分类和模型
  const [, modelsRes] = await Promise.all([
    getCategoryTree()
      .then((res) => { categories.value = res.data.data ?? [] })
      .catch((e) => console.error('加载分类失败', e)),
    getModels({ keyword: '', page: 0, size: 50 })
      .then((res) => { models.value = res.data.data?.content ?? [] })
      .catch((e) => console.error('加载模型失败', e)),
  ])
  loading.value = false
})

function selectCategory(categoryId: number | undefined) {
  activeCategoryId.value = categoryId
  loading.value = true
  getModels({ keyword: searchKeyword.value, categoryId, page: 0, size: 50 })
    .then((res) => { models.value = res.data.data?.content ?? [] })
    .catch((e) => console.error('加载模型失败', e))
    .finally(() => { loading.value = false })
}

function selectModel(model: Model) {
  selectedModel.value = model
  // 获取预签名 URL 用于预览
  getModelUrl(model.id)
    .then((res) => { previewUrl.value = res.data.data.downloadUrl })
    .catch(() => { previewUrl.value = '' })
}

function confirmSelect() {
  if (!selectedModel.value) return
  // 优先使用预签名 URL，否则用直连 MinIO URL
  const url = previewUrl.value || `${import.meta.env.VITE_MINIO_URL || 'http://localhost:9000'}/3d-models/${selectedModel.value.fileKey}`
  emit('select', selectedModel.value, url)
}

function filteredModels() {
  if (!searchKeyword.value) return models.value
  const kw = searchKeyword.value.toLowerCase()
  return models.value.filter((m) => m.name.toLowerCase().includes(kw))
}

// 搜索防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    selectCategory(activeCategoryId.value)
  }, 300)
}

// 按分类计算总模型数
const totalModelCount = computed(() =>
  categories.value.reduce((sum, c) => sum + (c.modelCount ?? 0), 0)
)

// 获取子分类
function getSubCategories(parentId: number | null): CategoryTreeNode[] {
  return categories.value.filter((c) => c.parentId === parentId)
}

// 顶级分类
const topCategories = computed(() => getSubCategories(null))
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="emit('close')">
    <div class="glass-card w-full max-w-4xl mx-4 h-[80vh] flex flex-col animate-slide-up">
      <!-- 头部 -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-primary/10 shrink-0">
        <h2 class="text-lg font-semibold text-white">从模型库选择</h2>
        <button class="text-gray-400 hover:text-white transition-colors" @click="emit('close')">
          <X :size="20" />
        </button>
      </div>

      <!-- 分类标签栏 -->
      <div class="px-4 py-2 border-b border-primary/10 shrink-0">
        <div class="flex items-center gap-1.5 overflow-x-auto scrollbar-thin">
          <button
            class="shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
            :class="activeCategoryId === undefined
              ? 'bg-primary/20 text-primary-light border border-primary/30'
              : 'text-gray-400 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
            @click="selectCategory(undefined)"
          >
            全部 <span class="ml-0.5 opacity-60">{{ totalModelCount }}</span>
          </button>
          <button
            v-for="cat in topCategories"
            :key="cat.id"
            class="shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
            :class="activeCategoryId === cat.id
              ? 'bg-primary/20 text-primary-light border border-primary/30'
              : 'text-gray-400 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }} <span class="ml-0.5 opacity-60">{{ cat.modelCount ?? 0 }}</span>
          </button>
        </div>
      </div>

      <div class="flex flex-1 overflow-hidden">
        <!-- 左侧：模型列表 -->
        <div class="w-1/2 border-r border-primary/10 overflow-y-auto p-3">
          <div class="relative mb-3">
            <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索模型..."
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-lg pl-8 pr-3 py-2 text-xs text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
              @input="onSearchInput"
            />
          </div>

          <div v-if="loading" class="flex justify-center py-8">
            <div class="w-6 h-6 border border-primary/30 border-t-primary-light rounded-full animate-spin" />
          </div>

          <div v-else class="space-y-1.5">
            <button
              v-for="model in filteredModels()"
              :key="model.id"
              class="w-full text-left flex items-center gap-3 p-2 rounded-lg transition-all"
              :class="selectedModel?.id === model.id
                ? 'bg-primary/15 border border-primary/30'
                : 'hover:bg-primary/5 border border-transparent'"
              @click="selectModel(model)"
            >
              <img
                v-if="model.thumbnailUrl"
                :src="model.thumbnailUrl"
                class="w-10 h-10 rounded-lg object-cover shrink-0"
              />
              <div v-else class="w-10 h-10 rounded-lg bg-dark-surface/50 flex items-center justify-center shrink-0">
                <Box :size="18" class="text-gray-600" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-xs text-white truncate">{{ model.name }}</p>
                <p class="text-[10px] text-gray-500">.{{ model.format }} · {{ (model.fileSize / 1024 / 1024).toFixed(1) }} MB</p>
              </div>
            </button>

            <p v-if="filteredModels().length === 0" class="text-xs text-gray-600 text-center py-8">
              暂无模型
            </p>
          </div>
        </div>

        <!-- 右侧：预览 -->
        <div class="w-1/2 flex flex-col">
          <div class="flex-1 flex items-center justify-center bg-dark-surface/20">
            <template v-if="selectedModel">
              <iframe
                v-if="previewUrl"
                :src="previewUrl"
                class="w-full h-full"
                frameborder="0"
              />
              <div v-else class="text-center text-gray-500">
                <Box :size="48" class="mx-auto mb-2" />
                <p class="text-sm">{{ selectedModel.name }}</p>
              </div>
            </template>
            <div v-else class="text-center text-gray-600">
              <Box :size="48" class="mx-auto mb-2" />
              <p class="text-xs">从左侧选择一个模型</p>
            </div>
          </div>

          <!-- 底部确认 -->
          <div class="px-4 py-3 border-t border-primary/10 flex justify-end gap-3 shrink-0">
            <button class="btn-ghost text-sm" @click="emit('close')">取消</button>
            <button
              class="btn-primary text-sm"
              :disabled="!selectedModel"
              @click="confirmSelect"
            >
              添加到场景
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
