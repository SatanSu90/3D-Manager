<script setup lang="ts">
import { ref } from 'vue'
import { Upload, X, Loader2 } from 'lucide-vue-next'
import { uploadModel } from '@/api/model'
import { thumbnailGenerator } from '@/three/ThumbnailGenerator'
import { useModelStore } from '@/stores/model'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'uploaded'): void
}>()

const modelStore = useModelStore()

const name = ref('')
const description = ref('')
const categoryId = ref<number | null>(null)
const selectedTagIds = ref<number[]>([])
const file = ref<File | null>(null)
const thumbnailBlob = ref<Blob | null>(null)
const thumbnailUrl = ref('')
const uploading = ref(false)
const generatingThumb = ref(false)
const dragOver = ref(false)

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files?.length) {
    setFile(input.files[0])
  }
}

function handleDrop(event: DragEvent) {
  dragOver.value = false
  const dropped = event.dataTransfer?.files
  if (dropped?.length) {
    setFile(dropped[0])
  }
}

async function setFile(f: File) {
  const ext = f.name.split('.').pop()?.toLowerCase()
  if (!['glb', 'gltf'].includes(ext || '')) {
    alert('仅支持 .glb 和 .gltf 格式')
    return
  }
  file.value = f
  if (!name.value) {
    name.value = f.name.replace(/\.(glb|gltf)$/i, '')
  }
  
  generatingThumb.value = true
  try {
    const blob = await thumbnailGenerator.generateFromFile(f)
    thumbnailBlob.value = blob
    thumbnailUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    console.error('缩略图生成失败:', e)
  } finally {
    generatingThumb.value = false
  }
}

function removeFile() {
  file.value = null
  thumbnailBlob.value = null
  if (thumbnailUrl.value) URL.revokeObjectURL(thumbnailUrl.value)
  thumbnailUrl.value = ''
}

async function handleSubmit() {
  if (!file.value || !thumbnailBlob.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('thumbnail', thumbnailBlob.value, 'thumbnail.jpg')
    formData.append('name', name.value)
    formData.append('description', description.value)
    if (categoryId.value) formData.append('categoryId', String(categoryId.value))
    selectedTagIds.value.forEach((id) => formData.append('tagIds', String(id)))

    await uploadModel(formData)
    modelStore.loadModels()
    emit('uploaded')
    emit('close')
  } catch (e: unknown) {
    console.error('上传失败:', e)
    const err = e as Error & { response?: { data?: unknown } }
    const detail = err.response?.data ? JSON.stringify(err.response.data) : err.message
    alert('上传失败: ' + detail)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="emit('close')">
    <div class="glass-card w-full max-w-lg mx-4 p-6 animate-slide-up">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-lg font-semibold text-white">上传模型</h2>
        <button class="text-gray-400 hover:text-white transition-colors" @click="emit('close')">
          <X :size="20" />
        </button>
      </div>

      <div class="space-y-4">
        <div>
          <label class="block text-sm text-gray-400 mb-1.5">模型文件</label>
          <div
            class="border-2 border-dashed rounded-xl p-6 text-center transition-all cursor-pointer"
            :class="dragOver ? 'border-primary bg-primary/5' : 'border-primary/20 hover:border-primary/40'"
            @dragover.prevent="dragOver = true"
            @dragleave="dragOver = false"
            @drop.prevent="handleDrop"
            @click="($refs.fileInput as HTMLInputElement)?.click()"
          >
            <input ref="fileInput" type="file" accept=".glb,.gltf" class="hidden" @change="handleFileSelect" />
            <template v-if="!file">
              <Upload :size="32" class="mx-auto text-gray-500 mb-2" />
              <p class="text-sm text-gray-400">拖拽文件到此处或点击选择</p>
              <p class="text-xs text-gray-600 mt-1">支持 .glb、.gltf 格式</p>
            </template>
            <template v-else>
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div v-if="generatingThumb" class="w-12 h-12 rounded-lg bg-dark-surface/50 flex items-center justify-center">
                    <Loader2 :size="20" class="animate-spin text-primary-light" />
                  </div>
                  <img v-else-if="thumbnailUrl" :src="thumbnailUrl" class="w-12 h-12 rounded-lg object-cover" />
                  <div class="text-left">
                    <p class="text-sm text-white">{{ file.name }}</p>
                    <p class="text-xs text-gray-500">{{ (file.size / 1024 / 1024).toFixed(2) }} MB</p>
                    <p v-if="generatingThumb" class="text-xs text-primary-light">正在生成缩略图...</p>
                  </div>
                </div>
                <button class="text-gray-400 hover:text-danger transition-colors" @click.stop="removeFile">
                  <X :size="18" />
                </button>
              </div>
            </template>
          </div>
        </div>

        <div>
          <label class="block text-sm text-gray-400 mb-1.5">模型名称</label>
          <input
            v-model="name"
            type="text"
            placeholder="输入模型名称"
            class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          />
        </div>

        <div>
          <label class="block text-sm text-gray-400 mb-1.5">描述</label>
          <textarea
            v-model="description"
            rows="3"
            placeholder="输入模型描述..."
            class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all resize-none"
          />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1.5">分类</label>
            <select
              v-model="categoryId"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none transition-all"
            >
              <option :value="null">未分类</option>
              <option v-for="cat in modelStore.categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1.5">标签</label>
            <div class="flex flex-wrap gap-1.5">
              <button
                v-for="tag in modelStore.tags"
                :key="tag.id"
                class="px-2 py-0.5 rounded-full text-xs transition-all"
                :class="selectedTagIds.includes(tag.id)
                  ? 'bg-primary/20 text-primary-light border border-primary/30'
                  : 'bg-dark-surface/50 text-gray-400 border border-transparent hover:border-primary/20'"
                @click="selectedTagIds.includes(tag.id) ? selectedTagIds.splice(selectedTagIds.indexOf(tag.id), 1) : selectedTagIds.push(tag.id)"
              >
                {{ tag.name }}
              </button>
            </div>
          </div>
        </div>

        <div class="flex gap-3 pt-2">
          <button class="flex-1 btn-ghost text-sm" @click="emit('close')">取消</button>
          <button
            class="flex-1 btn-primary text-sm"
            :disabled="!file || !name || uploading || generatingThumb"
            @click="handleSubmit"
          >
            <span v-if="uploading" class="inline-flex items-center gap-2">
              <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              上传中...
            </span>
            <span v-else>上传</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
