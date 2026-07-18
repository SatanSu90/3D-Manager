<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Image as ImageIcon, Plus, Trash2, Search, X, Upload,
  FileImage, Music, Video, Square, Layers as LayersIcon,
} from 'lucide-vue-next'
import {
  getResources,
  uploadResource,
  deleteResource,
  batchDeleteResources,
  downloadResource,
} from '@/api/resource'
import type { Resource, ResourceType } from '@/types/report'
import type { PageData } from '@/types/api'

const resources = ref<Resource[]>([])
const loading = ref(true)
const keyword = ref('')
const typeFilter = ref<ResourceType | ''>('')

const currentPage = ref(0)
const pageSize = ref(24)
const total = ref(0)

const showUpload = ref(false)
const uploadFile = ref<File | null>(null)
const uploadType = ref<ResourceType>('IMAGE')
const uploadTags = ref('')
const uploading = ref(false)

const selectedIds = ref<Set<number>>(new Set())

const typeOptions: { value: ResourceType; label: string; icon: typeof ImageIcon; color: string }[] = [
  { value: 'IMAGE', label: '图片', icon: ImageIcon, color: 'text-blue-400' },
  { value: 'ICON', label: '图标', icon: FileImage, color: 'text-cyan-400' },
  { value: 'TEXTURE', label: '纹理', icon: LayersIcon, color: 'text-purple-400' },
  { value: 'MATERIAL', label: '材质', icon: Square, color: 'text-pink-400' },
  { value: 'AUDIO', label: '音频', icon: Music, color: 'text-yellow-400' },
  { value: 'VIDEO', label: '视频', icon: Video, color: 'text-green-400' },
]

onMounted(loadResources)

function loadResources() {
  loading.value = true
  getResources({
    keyword: keyword.value || undefined,
    type: typeFilter.value || undefined,
    page: currentPage.value,
    size: pageSize.value,
  })
    .then((res) => {
      const data = res.data.data as PageData<Resource>
      resources.value = data.content ?? []
      total.value = data.totalElements ?? 0
      selectedIds.value.clear()
    })
    .catch((e) => {
      console.error('加载资源失败', e)
      alert('加载资源失败: ' + (e as Error).message)
    })
    .finally(() => { loading.value = false })
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function applyTypeFilter(value: ResourceType | '') {
  typeFilter.value = value
  currentPage.value = 0
  loadResources()
}

function onSearchInput() {
  currentPage.value = 0
  loadResources()
}

function setPage(p: number) {
  currentPage.value = p
  loadResources()
}

function typeInfo(type: ResourceType) {
  return typeOptions.find((o) => o.value === type) || typeOptions[0]
}

function formatSize(bytes: number | null): string {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  })
}

function openUpload() {
  uploadFile.value = null
  uploadType.value = 'IMAGE'
  uploadTags.value = ''
  showUpload.value = true
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    uploadFile.value = input.files[0]
    // 根据文件类型自动推断
    const mime = uploadFile.value.type
    if (mime.startsWith('image/')) uploadType.value = 'IMAGE'
    else if (mime.startsWith('audio/')) uploadType.value = 'AUDIO'
    else if (mime.startsWith('video/')) uploadType.value = 'VIDEO'
  }
}

async function handleUpload() {
  if (!uploadFile.value) {
    alert('请选择文件')
    return
  }
  uploading.value = true
  try {
    await uploadResource({
      file: uploadFile.value,
      type: uploadType.value,
      tags: uploadTags.value || undefined,
    })
    showUpload.value = false
    loadResources()
  } catch (e: unknown) {
    alert('上传失败: ' + (e as Error).message)
  } finally {
    uploading.value = false
  }
}

async function handleDelete(resource: Resource) {
  if (!confirm(`确定删除资源「${resource.name}」？此操作不可撤销。`)) return
  try {
    await deleteResource(resource.id)
    loadResources()
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.size === 0) {
    alert('请先选择要删除的资源')
    return
  }
  if (!confirm(`确定批量删除 ${selectedIds.value.size} 个资源？此操作不可撤销。`)) return
  try {
    const res = await batchDeleteResources(Array.from(selectedIds.value))
    const result = res.data.data
    alert(`批量删除完成：成功 ${result.success} 个，失败 ${result.failed} 个`)
    loadResources()
  } catch (e: unknown) {
    alert('批量删除失败: ' + (e as Error).message)
  }
}

async function handleDownload(resource: Resource) {
  try {
    const res = await downloadResource(resource.id)
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = resource.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: unknown) {
    alert('下载失败: ' + (e as Error).message)
  }
}

function toggleSelect(id: number) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

function openImage(resource: Resource) {
  if (isImage(resource) && resource.fileUrl) {
    window.open(resource.fileUrl, '_blank')
  }
}

function isImage(resource: Resource) {
  return resource.type === 'IMAGE' || resource.type === 'ICON' || resource.type === 'TEXTURE'
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <ImageIcon :size="24" class="text-primary-light" />
          资源库
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ total }} 个资源{{ selectedIds.size > 0 ? `（已选 ${selectedIds.size}）` : '' }}</p>
      </div>
      <div class="flex gap-2">
        <button
          v-if="selectedIds.size > 0"
          class="btn-ghost text-sm flex items-center gap-2 text-danger"
          @click="handleBatchDelete"
        >
          <Trash2 :size="16" />
          批量删除
        </button>
        <button class="btn-primary text-sm flex items-center gap-2" @click="openUpload">
          <Plus :size="16" />
          上传资源
        </button>
      </div>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="flex flex-wrap gap-3 mb-6">
      <div class="relative flex-1 min-w-[240px] max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索资源名/标签..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          @input="onSearchInput"
        />
      </div>
      <select
        :value="typeFilter"
        @change="applyTypeFilter(($event.target as HTMLSelectElement).value as ResourceType | '')"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
      >
        <option value="">全部类型</option>
        <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="resources.length === 0" class="text-center text-gray-500 py-20">
      <ImageIcon :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无资源</p>
      <p class="text-sm">点击上方按钮上传你的第一个资源</p>
    </div>

    <!-- 资源网格 -->
    <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
      <div
        v-for="resource in resources"
        :key="resource.id"
        class="glass-card hover-glow group overflow-hidden relative"
        :class="selectedIds.has(resource.id) ? 'ring-2 ring-primary' : ''"
      >
        <!-- 选择框 -->
        <input
          type="checkbox"
          :checked="selectedIds.has(resource.id)"
          class="absolute top-2 left-2 z-10 w-4 h-4 accent-primary opacity-0 group-hover:opacity-100 transition-all"
          :class="selectedIds.has(resource.id) ? 'opacity-100' : ''"
          @change="toggleSelect(resource.id)"
        />

        <div
          class="aspect-square relative overflow-hidden bg-dark-surface/30 flex items-center justify-center cursor-pointer"
          @click="openImage(resource)"
        >
          <img
            v-if="isImage(resource) && resource.fileUrl"
            :src="resource.fileUrl"
            :alt="resource.name"
            class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          <component
            v-else
            :is="typeInfo(resource.type).icon"
            :size="36"
            :class="typeInfo(resource.type).color"
          />

          <span class="absolute top-2 right-2 px-1.5 py-0.5 rounded text-[9px] bg-dark/70 text-gray-300">
            {{ typeInfo(resource.type).label }}
          </span>

          <div class="absolute bottom-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-all">
            <button
              class="p-1 rounded bg-dark/70 text-gray-300 hover:text-primary-light transition-colors"
              title="下载"
              @click.stop="handleDownload(resource)"
            >
              <Upload :size="12" />
            </button>
            <button
              class="p-1 rounded bg-dark/70 text-gray-300 hover:text-danger transition-colors"
              title="删除"
              @click.stop="handleDelete(resource)"
            >
              <Trash2 :size="12" />
            </button>
          </div>
        </div>

        <div class="p-2">
          <h3 class="text-xs font-medium text-white truncate" :title="resource.name">{{ resource.name }}</h3>
          <div class="flex items-center justify-between mt-1 text-[10px] text-gray-600">
            <span>{{ formatSize(resource.fileSize) }}</span>
            <span v-if="resource.width && resource.height">{{ resource.width }}×{{ resource.height }}</span>
          </div>
          <p class="text-[10px] text-gray-700 mt-0.5">{{ formatDate(resource.createdAt) }}</p>
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

    <!-- 上传弹窗 -->
    <Teleport to="body">
      <div v-if="showUpload" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showUpload = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">上传资源</h3>
            <button class="text-gray-500 hover:text-white" @click="showUpload = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">选择文件</label>
              <input
                type="file"
                @change="onFileChange"
                class="w-full text-sm text-gray-300 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-primary/20 file:text-primary-light file:cursor-pointer hover:file:bg-primary/30 cursor-pointer"
              />
              <p v-if="uploadFile" class="text-xs text-gray-500 mt-1">
                {{ uploadFile.name }} ({{ formatSize(uploadFile.size) }})
              </p>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">资源类型</label>
              <select v-model="uploadType"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">标签 (逗号分隔)</label>
              <input v-model="uploadTags" type="text" placeholder="可选，如：风景,蓝天,参考"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showUpload = false">取消</button>
            <button class="flex-1 btn-primary text-sm flex items-center justify-center gap-2" :disabled="!uploadFile || uploading" @click="handleUpload">
              <Upload :size="14" />
              {{ uploading ? '上传中...' : '上传' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
