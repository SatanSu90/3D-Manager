<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useModelStore } from '@/stores/model'
import ModelCard from '@/components/model/ModelCard.vue'
import ModelUpload from '@/components/model/ModelUpload.vue'
import { Plus, LayoutGrid, List, Search, Box } from 'lucide-vue-next'

const modelStore = useModelStore()
const showUpload = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const searchKeyword = ref('')

// 分类过滤：当前选中的分类 ID
const activeCategoryId = computed(() => modelStore.query.categoryId)

// 顶级分类列表
const topCategories = computed(() =>
  modelStore.categories.filter((c) => c.parentId === null || c.parentId === undefined)
)

let searchTimer: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  modelStore.loadModels()
  modelStore.loadCategories()
  modelStore.loadTags()
})

function handleSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    modelStore.setKeyword(searchKeyword.value)
  }, 300)
}

function selectCategory(categoryId: number | undefined) {
  modelStore.setCategory(categoryId)
}

function handlePageChange(page: number) {
  modelStore.setPage(page - 1)
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white">模型库</h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ modelStore.totalElements }} 个模型</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative">
          <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索模型..."
            class="bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all w-64"
            @input="handleSearch"
          />
        </div>
        <div class="flex glass-panel p-1">
          <button
            class="p-1.5 rounded-md transition-all"
            :class="viewMode === 'grid' ? 'bg-primary/20 text-primary-light' : 'text-gray-400 hover:text-gray-300'"
            @click="viewMode = 'grid'"
          >
            <LayoutGrid :size="16" />
          </button>
          <button
            class="p-1.5 rounded-md transition-all"
            :class="viewMode === 'list' ? 'bg-primary/20 text-primary-light' : 'text-gray-400 hover:text-gray-300'"
            @click="viewMode = 'list'"
          >
            <List :size="16" />
          </button>
        </div>
        <button class="btn-primary text-sm flex items-center gap-2" @click="showUpload = true">
          <Plus :size="16" />
          上传模型
        </button>
      </div>
    </div>

    <!-- 分类标签栏 -->
    <div class="mb-5">
      <div class="flex items-center gap-2 overflow-x-auto scrollbar-thin pb-1">
        <button
          class="shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-all"
          :class="activeCategoryId === undefined
            ? 'bg-primary/20 text-primary-light border border-primary/30'
            : 'text-gray-400 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
          @click="selectCategory(undefined)"
        >
          全部
        </button>
        <button
          v-for="cat in topCategories"
          :key="cat.id"
          class="shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-all"
          :class="activeCategoryId === cat.id
            ? 'bg-primary/20 text-primary-light border border-primary/30'
            : 'text-gray-400 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>
    </div>

    <div v-if="modelStore.loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="modelStore.models.length === 0" class="flex flex-col items-center justify-center py-20 text-gray-500">
      <p class="text-lg mb-2">暂无模型</p>
      <p class="text-sm">点击上方按钮上传你的第一个 3D 模型</p>
    </div>

    <template v-else>
      <div
        v-if="viewMode === 'grid'"
        class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4"
      >
        <ModelCard v-for="model in modelStore.models" :key="model.id" :model="model" @deleted="modelStore.loadModels()" />
      </div>

      <div v-else class="space-y-2">
        <div
          v-for="model in modelStore.models"
          :key="model.id"
          class="glass-card p-4 flex items-center gap-4 hover-glow cursor-pointer"
          @click="$router.push(`/model/${model.id}`)"
        >
          <img
            v-if="model.thumbnailUrl"
            :src="model.thumbnailUrl"
            class="w-16 h-16 rounded-lg object-cover"
            loading="lazy"
          />
          <div v-else class="w-16 h-16 rounded-lg bg-dark-surface/50 flex items-center justify-center">
            <Box :size="24" class="text-gray-600" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-sm font-medium text-white truncate">{{ model.name }}</h3>
            <p class="text-xs text-gray-500">{{ model.format }} · {{ (model.fileSize / 1024 / 1024).toFixed(2) }} MB</p>
          </div>
          <div class="flex items-center gap-2">
            <span
              v-for="tag in model.tags?.slice(0, 2)"
              :key="tag.id"
              class="px-2 py-0.5 rounded-full text-[10px] bg-primary/10 text-primary-light"
            >
              {{ tag.name }}
            </span>
          </div>
        </div>
      </div>

      <div class="flex justify-center mt-8">
        <div class="flex gap-1">
          <button
            v-for="page in modelStore.totalPages"
            :key="page"
            class="w-9 h-9 rounded-lg text-sm transition-all"
            :class="modelStore.query.page === page - 1
              ? 'bg-primary/20 text-primary-light border border-primary/30'
              : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </button>
        </div>
      </div>
    </template>

    <ModelUpload v-if="showUpload" @close="showUpload = false" @uploaded="showUpload = false" />
  </div>
</template>
