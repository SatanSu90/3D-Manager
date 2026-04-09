<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Pencil, Trash2, ExternalLink, Layers, Search, Box } from 'lucide-vue-next'
import { getScenes, deleteScene } from '@/api/scene'
import type { Scene } from '@/types/scene'

const router = useRouter()
const scenes = ref<Scene[]>([])
const loading = ref(true)
const searchKeyword = ref('')
const showEdit = ref(false)
const editingScene = ref<Scene | null>(null)
const editName = ref('')

onMounted(loadScenes)

function loadScenes() {
  loading.value = true
  getScenes()
    .then((res) => { scenes.value = res.data.data ?? [] })
    .catch((e) => console.error('加载场景失败', e))
    .finally(() => { loading.value = false })
}

function filteredScenes() {
  if (!searchKeyword.value) return scenes.value
  const kw = searchKeyword.value.toLowerCase()
  return scenes.value.filter((s) => s.name.toLowerCase().includes(kw))
}

function handleNewScene() {
  router.push('/editor')
}

function openEdit(scene: Scene) {
  editingScene.value = scene
  editName.value = scene.name
  showEdit.value = true
}

function handleEditSubmit() {
  // 编辑器内保存时改名，这里只改名称
  editingScene.value = null
  showEdit.value = false
}

function handlePreview(scene: Scene) {
  window.open(`/preview/${scene.id}`, '_blank')
}

function handleEdit(scene: Scene) {
  router.push(`/editor/${scene.id}`)
}

function handleDelete(scene: Scene) {
  if (!confirm(`确定删除场景「${scene.name}」？此操作不可撤销。`)) return
  deleteScene(scene.id)
    .then(loadScenes)
    .catch((e) => alert('删除失败: ' + (e.response?.data?.message || e.message)))
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <Layers :size="24" class="text-primary-light" />
          场景管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ scenes.length }} 个场景</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="handleNewScene">
        <Plus :size="16" />
        新建场景
      </button>
    </div>

    <!-- 搜索 -->
    <div class="mb-6">
      <div class="relative max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索场景..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
        />
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="filteredScenes().length === 0" class="text-center text-gray-500 py-20">
      <Layers :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无场景</p>
      <p class="text-sm">点击上方按钮创建你的第一个场景</p>
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="scene in filteredScenes()"
        :key="scene.id"
        class="glass-card hover-glow group overflow-hidden cursor-pointer"
        @click="handleEdit(scene)"
      >
        <!-- 缩略图 -->
        <div class="aspect-video relative overflow-hidden bg-dark-surface/30">
          <img
            v-if="scene.thumbnailUrl"
            :src="scene.thumbnailUrl"
            :alt="scene.name"
            class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          <div v-else class="w-full h-full flex items-center justify-center">
            <Layers :size="48" class="text-gray-600" />
          </div>

          <!-- 操作按钮 -->
          <div class="absolute top-2 right-2 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-all">
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-white transition-colors"
              title="预览"
              @click.stop="handlePreview(scene)"
            >
              <ExternalLink :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-primary-light transition-colors"
              title="编辑"
              @click.stop="handleEdit(scene)"
            >
              <Pencil :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-danger transition-colors"
              title="删除"
              @click.stop="handleDelete(scene)"
            >
              <Trash2 :size="14" />
            </button>
          </div>

          <span class="absolute bottom-2 left-2 px-2 py-0.5 rounded-md text-[10px] bg-dark/70 text-gray-400">
            {{ formatDate(scene.updatedAt) }}
          </span>
        </div>

        <div class="p-3">
          <h3 class="text-sm font-medium text-white truncate">{{ scene.name }}</h3>
          <p class="text-xs text-gray-500 mt-0.5">创建者: {{ scene.creatorName }}</p>
        </div>
      </div>
    </div>

    <!-- 编辑名称弹窗 -->
    <Teleport to="body">
      <div v-if="showEdit" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showEdit = false">
        <div class="glass-card w-full max-w-sm mx-4 p-6 animate-slide-up">
          <h3 class="text-lg font-semibold text-white mb-4">编辑场景</h3>
          <div>
            <label class="block text-sm text-gray-400 mb-1.5">场景名称</label>
            <input
              v-model="editName"
              type="text"
              placeholder="场景名称"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
              @keyup.enter="handleEditSubmit"
            />
          </div>
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showEdit = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!editName.trim()" @click="handleEditSubmit">
              保存并编辑
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
