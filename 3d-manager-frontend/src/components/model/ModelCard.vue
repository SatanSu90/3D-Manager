<script setup lang="ts">
import { ref } from 'vue'
import type { Model } from '@/types/model'
import { Download, Eye, Box, Trash2 } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { deleteModel } from '@/api/model'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  model: Model
}>()

const emit = defineEmits<{
  (e: 'deleted'): void
}>()

const router = useRouter()
const authStore = useAuthStore()
const deleting = ref(false)

function goToDetail() {
  router.push(`/model/${props.model.id}`)
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function handleDelete(e: MouseEvent) {
  e.stopPropagation()
  if (!confirm(`确定删除模型「${props.model.name}」？此操作不可撤销。`)) return
  deleting.value = true
  deleteModel(props.model.id)
    .then(() => {
      emit('deleted')
    })
    .catch((err) => {
      console.error('删除失败:', err)
      alert('删除失败: ' + (err.response?.data?.message || err.message))
    })
    .finally(() => {
      deleting.value = false
    })
}
</script>

<template>
  <div
    class="glass-card hover-glow cursor-pointer group overflow-hidden animate-fade-in"
    @click="goToDetail"
  >
    <div class="aspect-square relative overflow-hidden bg-dark-surface/30">
      <img
        v-if="model.thumbnailUrl"
        :src="model.thumbnailUrl"
        :alt="model.name"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        loading="lazy"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <Box :size="48" class="text-gray-600" />
      </div>
      
      <div class="absolute inset-0 bg-gradient-to-t from-dark/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
      
      <div class="absolute bottom-2 right-2 flex gap-2 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-y-2 group-hover:translate-y-0">
        <span class="px-2 py-1 rounded-md bg-dark/70 text-xs text-gray-300 flex items-center gap-1">
          <Eye :size="12" />
          预览
        </span>
        <button
          class="px-2 py-1 rounded-md bg-danger/80 text-white text-xs flex items-center gap-1 hover:bg-danger transition-colors disabled:opacity-50"
          :disabled="deleting"
          title="删除模型"
          @click="handleDelete"
        >
          <span v-if="deleting" class="w-3 h-3 border border-white/50 border-t-white rounded-full animate-spin" />
          <Trash2 v-else :size="12" />
          删除
        </button>
      </div>

      <span class="absolute top-2 right-2 px-2 py-0.5 rounded-md text-xs font-medium bg-primary/80 text-white">
        .{{ model.format }}
      </span>
    </div>

    <div class="p-3">
      <h3 class="text-sm font-medium text-white truncate mb-1">{{ model.name }}</h3>
      <div class="flex items-center justify-between text-xs text-gray-500 mb-2">
        <span>{{ formatSize(model.fileSize) }}</span>
        <span class="flex items-center gap-1">
          <Download :size="12" />
          {{ model.downloadCount }}
        </span>
      </div>
      <div class="flex flex-wrap gap-1">
        <span
          v-for="tag in model.tags?.slice(0, 3)"
          :key="tag.id"
          class="px-2 py-0.5 rounded-full text-[10px] bg-primary/10 text-primary-light border border-primary/15"
        >
          {{ tag.name }}
        </span>
      </div>
    </div>
  </div>
</template>
