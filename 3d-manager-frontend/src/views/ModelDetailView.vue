<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getModel, deleteModel, downloadModel, getModelUrl } from '@/api/model'
import type { Model } from '@/types/model'
import ModelPreview from '@/components/model/ModelPreview.vue'
import { Download, Trash2, ArrowLeft, Calendar, HardDrive, User, Tag, Folder } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const model = ref<Model | null>(null)
const loading = ref(true)
const modelUrl = ref('')

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getModel(id)
    model.value = res.data.data
    // 通过后端接口获取预签名 URL（避免 CORS 问题）
    const dlRes = await getModelUrl(model.value.id)
    modelUrl.value = dlRes.data.data.downloadUrl
  } catch (e) {
    console.error('获取模型详情失败:', e)
  } finally {
    loading.value = false
  }
})

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleDownload() {
  if (!model.value) return
  try {
    const res = await downloadModel(model.value.id)
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${model.value.name}.${model.value.format}`
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('下载失败:', e)
  }
}

async function handleDelete() {
  if (!model.value) return
  if (!confirm('确定删除此模型？此操作不可撤销。')) return
  try {
    await deleteModel(model.value.id)
    router.push('/')
  } catch (e) {
    console.error('删除失败:', e)
  }
}
</script>

<template>
  <div class="p-6">
    <button
      class="flex items-center gap-2 text-gray-400 hover:text-primary-light transition-colors mb-4"
      @click="router.back()"
    >
      <ArrowLeft :size="18" />
      <span class="text-sm">返回列表</span>
    </button>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="model" class="flex gap-6 h-[calc(100vh-10rem)]">
      <div class="flex-[3] glass-card overflow-hidden">
        <ModelPreview :model-url="modelUrl" />
      </div>

      <div class="flex-[2] space-y-4 overflow-y-auto">
        <div class="glass-card p-6">
          <h1 class="text-xl font-semibold text-white mb-4">{{ model.name }}</h1>
          <p class="text-sm text-gray-400 leading-relaxed">{{ model.description || '暂无描述' }}</p>
        </div>

        <div class="glass-card p-6">
          <h3 class="text-sm font-medium text-primary-light mb-4">模型信息</h3>
          <div class="space-y-3">
            <div class="flex items-center gap-3 text-sm">
              <HardDrive :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">格式</span>
              <span class="text-white ml-auto">.{{ model.format }}</span>
            </div>
            <div class="flex items-center gap-3 text-sm">
              <HardDrive :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">大小</span>
              <span class="text-white ml-auto">{{ formatSize(model.fileSize) }}</span>
            </div>
            <div class="flex items-center gap-3 text-sm">
              <Download :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">下载次数</span>
              <span class="text-white ml-auto">{{ model.downloadCount }}</span>
            </div>
            <div class="flex items-center gap-3 text-sm">
              <Folder :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">分类</span>
              <span class="text-white ml-auto">{{ model.categoryName || '未分类' }}</span>
            </div>
            <div class="flex items-center gap-3 text-sm">
              <User :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">上传者</span>
              <span class="text-white ml-auto">{{ model.uploaderName }}</span>
            </div>
            <div class="flex items-center gap-3 text-sm">
              <Calendar :size="16" class="text-gray-500 shrink-0" />
              <span class="text-gray-400">上传时间</span>
              <span class="text-white ml-auto">{{ formatDate(model.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div v-if="model.tags?.length" class="glass-card p-6">
          <h3 class="text-sm font-medium text-primary-light mb-3 flex items-center gap-2">
            <Tag :size="14" />
            标签
          </h3>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="tag in model.tags"
              :key="tag.id"
              class="px-3 py-1 rounded-full text-xs bg-primary/10 text-primary-light border border-primary/15"
            >
              {{ tag.name }}
            </span>
          </div>
        </div>

        <div class="flex gap-3">
          <button class="flex-1 btn-primary text-sm flex items-center justify-center gap-2" @click="handleDownload">
            <Download :size="16" />
            下载
          </button>
          <button class="btn-ghost text-sm flex items-center gap-2 px-4" @click="handleDelete">
            <Trash2 :size="16" />
            删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
