<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, ArrowLeft, Tag } from 'lucide-vue-next'
import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'

interface TagItem {
  id: number
  name: string
}

const tags = ref<TagItem[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const formName = ref('')

onMounted(loadTags)

function loadTags() {
  loading.value = true
  getTags()
    .then((res) => { tags.value = res.data.data ?? [] })
    .catch((e) => console.error('加载标签失败', e))
    .finally(() => { loading.value = false })
}

function openCreate() {
  editingId.value = null
  formName.value = ''
  showForm.value = true
}

function openEdit(tag: TagItem) {
  editingId.value = tag.id
  formName.value = tag.name
  showForm.value = true
}

function handleSubmit() {
  if (!formName.value.trim()) return
  const action = editingId.value
    ? updateTag(editingId.value, { name: formName.value.trim() })
    : createTag({ name: formName.value.trim() })

  action
    .then(() => {
      showForm.value = false
      loadTags()
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
            <Tag :size="24" class="text-primary-light" />
            标签管理
          </h1>
          <p class="text-sm text-gray-500 mt-1">共 {{ tags.length }} 个标签</p>
        </div>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新增标签
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="tags.length === 0" class="text-center text-gray-500 py-20">
      <p class="text-lg mb-2">暂无标签</p>
      <p class="text-sm">点击上方按钮添加第一个标签</p>
    </div>

    <div v-else class="flex flex-wrap gap-3">
      <div
        v-for="tag in tags"
        :key="tag.id"
        class="glass-card px-4 py-2 flex items-center gap-2 group hover:border-primary/30 transition-all"
      >
        <span class="px-2 py-0.5 rounded-full text-sm bg-primary/10 text-primary-light border border-primary/15">
          {{ tag.name }}
        </span>
        <button
          class="p-0.5 rounded text-gray-500 hover:text-primary-light transition-colors"
          title="编辑"
          @click="openEdit(tag)"
        >
          <Pencil :size="12" />
        </button>
        <button
          class="p-0.5 rounded text-gray-500 hover:text-danger transition-colors"
          title="删除"
          @click="deleteTag(tag.id).then(loadTags).catch((e) => alert('删除失败: ' + (e.response?.data?.message || e.message)))"
        >
          <Trash2 :size="12" />
        </button>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-sm mx-4 p-6 animate-slide-up">
          <h3 class="text-lg font-semibold text-white mb-4">{{ editingId ? '编辑标签' : '新增标签' }}</h3>
          <div>
            <label class="block text-sm text-gray-400 mb-1.5">名称</label>
            <input
              v-model="formName"
              type="text"
              placeholder="标签名称"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
              @keyup.enter="handleSubmit"
            />
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
