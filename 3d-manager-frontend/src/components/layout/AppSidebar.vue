<script setup lang="ts">
import { useModelStore } from '@/stores/model'
import { FolderTree, Tag, ChevronRight, ChevronDown, Box, Layers } from 'lucide-vue-next'
import { ref } from 'vue'

const modelStore = useModelStore()
const expandedCategories = ref<Set<number>>(new Set())

function toggleCategory(categoryId: number) {
  if (expandedCategories.value.has(categoryId)) {
    expandedCategories.value.delete(categoryId)
  } else {
    expandedCategories.value.add(categoryId)
  }
}

function selectCategory(categoryId: number | undefined) {
  modelStore.setCategory(categoryId)
}

function toggleTag(tagId: number) {
  modelStore.toggleTag(tagId)
}

function clearFilters() {
  modelStore.setCategory(undefined)
  modelStore.query.tagIds = []
  modelStore.loadModels()
}
</script>

<template>
  <aside class="fixed left-0 top-16 bottom-0 w-64 glass-card border-r border-primary/10 rounded-none overflow-y-auto p-4">
    <!-- 顶部导航 -->
    <div class="space-y-1 mb-6">
      <button
        class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all"
        :class="$route.path === '/' ? 'bg-primary/15 text-primary-light' : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
        @click="$router.push('/')"
      >
        <div class="flex items-center gap-2">
          <Box :size="14" />
          模型库
        </div>
      </button>
      <button
        class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all"
        :class="$route.path === '/scenes' ? 'bg-primary/15 text-primary-light' : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
        @click="$router.push('/scenes')"
      >
        <div class="flex items-center gap-2">
          <Layers :size="14" />
          场景管理
        </div>
      </button>
    </div>

    <!-- 分类筛选 -->
    <div class="mb-6">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-medium text-primary-light flex items-center gap-2">
          <FolderTree :size="16" />
          分类
        </h3>
        <button
          v-if="modelStore.query.categoryId"
          class="text-xs text-gray-500 hover:text-primary-light transition-colors"
          @click="clearFilters"
        >
          清除
        </button>
      </div>
      <div class="space-y-1">
        <button
          class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all"
          :class="!modelStore.query.categoryId ? 'bg-primary/15 text-primary-light' : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
          @click="selectCategory(undefined)"
        >
          全部模型
        </button>
        <div v-for="cat in (modelStore.categories ?? [])" :key="cat.id">
          <button
            class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-all"
            :class="modelStore.query.categoryId === cat.id ? 'bg-primary/15 text-primary-light' : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
            @click="selectCategory(cat.id)"
          >
            <component
              :is="cat.children?.length ? (expandedCategories.has(cat.id) ? ChevronDown : ChevronRight) : ChevronRight"
              v-if="cat.children?.length"
              :size="14"
              class="shrink-0"
              @click.stop="toggleCategory(cat.id)"
            />
            <span class="truncate">{{ cat.name }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="border-t border-primary/10 pt-4">
      <h3 class="text-sm font-medium text-primary-light flex items-center gap-2 mb-3">
        <Tag :size="16" />
        标签
      </h3>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tag in (modelStore.tags ?? [])"
          :key="tag.id"
          class="px-3 py-1 rounded-full text-xs transition-all"
          :class="modelStore.query.tagIds.includes(tag.id)
            ? 'bg-primary/20 text-primary-light border border-primary/30'
            : 'bg-dark-surface/50 text-gray-400 border border-transparent hover:border-primary/20 hover:text-gray-300'"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}
        </button>
      </div>
    </div>

    <!-- 管理入口 -->
    <div class="border-t border-primary/10 pt-4 mt-2">
      <h3 class="text-xs font-medium text-gray-600 mb-2 px-1">数据管理</h3>
      <div class="space-y-1">
        <button
          class="w-full text-left px-3 py-1.5 rounded-lg text-xs transition-all"
          :class="$route.path === '/categories' ? 'bg-primary/10 text-primary-light' : 'text-gray-500 hover:bg-primary/5 hover:text-gray-400'"
          @click="$router.push('/categories')"
        >
          <div class="flex items-center gap-2">
            <FolderTree :size="12" />
            分类管理
          </div>
        </button>
        <button
          class="w-full text-left px-3 py-1.5 rounded-lg text-xs transition-all"
          :class="$route.path === '/tags' ? 'bg-primary/10 text-primary-light' : 'text-gray-500 hover:bg-primary/5 hover:text-gray-400'"
          @click="$router.push('/tags')"
        >
          <div class="flex items-center gap-2">
            <Tag :size="12" />
            标签管理
          </div>
        </button>
      </div>
    </div>
  </aside>
</template>
