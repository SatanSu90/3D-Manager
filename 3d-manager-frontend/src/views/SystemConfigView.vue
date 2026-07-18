<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Settings, Pencil, X, Save, RotateCcw } from 'lucide-vue-next'
import { getSystemConfigs, updateSystemConfig } from '@/api/system'
import type { SystemConfig } from '@/types/system'

const groupedConfigs = ref<Record<string, SystemConfig[]>>({})
const loading = ref(true)

const showForm = ref(false)
const editingConfig = ref<SystemConfig | null>(null)
const formValue = ref('')

const categoryLabels: Record<string, string> = {
  general: '通用',
  scene: '场景',
  storage: '存储',
  security: '安全',
}

const sortedCategories = computed(() => {
  return Object.keys(groupedConfigs.value).sort()
})

onMounted(loadConfigs)

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    groupedConfigs.value = res.data.data ?? {}
  } catch (e: unknown) {
    console.error('加载配置失败', e)
  } finally {
    loading.value = false
  }
}

function openEdit(config: SystemConfig) {
  editingConfig.value = config
  formValue.value = config.configValue ?? ''
  showForm.value = true
}

async function handleSubmit() {
  if (!editingConfig.value) return
  try {
    await updateSystemConfig(editingConfig.value.configKey, {
      configValue: formValue.value,
    })
    showForm.value = false
    await loadConfigs()
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

function categoryLabel(key: string): string {
  return categoryLabels[key] || key
}

function typeBadgeClass(type: string): string {
  const map: Record<string, string> = {
    STRING: 'text-blue-400 bg-blue-500/10',
    NUMBER: 'text-green-400 bg-green-500/10',
    BOOLEAN: 'text-yellow-400 bg-yellow-500/10',
    JSON: 'text-purple-400 bg-purple-500/10',
  }
  return map[type] || 'text-gray-400 bg-gray-500/10'
}

function displayValue(config: SystemConfig): string {
  if (config.configType === 'BOOLEAN') {
    return config.configValue === 'true' ? '是' : '否'
  }
  if (!config.configValue) return '—'
  return config.configValue.length > 60 ? config.configValue.slice(0, 60) + '...' : config.configValue
}

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <Settings :size="24" class="text-primary-light" />
          系统配置
        </h1>
        <p class="text-sm text-gray-500 mt-1">管理系统全局配置参数</p>
      </div>
      <button class="btn-ghost text-sm flex items-center gap-2" @click="loadConfigs">
        <RotateCcw :size="14" />
        刷新
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="sortedCategories.length === 0" class="text-center text-gray-500 py-20">
      <Settings :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无配置项</p>
    </div>

    <div v-else class="space-y-6">
      <section v-for="category in sortedCategories" :key="category">
        <h2 class="text-sm font-medium text-gray-400 mb-3 flex items-center gap-2">
          <span class="w-1 h-4 bg-primary-light rounded"></span>
          {{ categoryLabel(category) }}
          <span class="text-xs text-gray-600">({{ groupedConfigs[category].length }})</span>
        </h2>
        <div class="glass-card overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="border-b border-primary/10">
                <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">配置项</th>
                <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">类型</th>
                <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">当前值</th>
                <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">描述</th>
                <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">更新时间</th>
                <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="config in groupedConfigs[category]"
                :key="config.id"
                class="border-b border-primary/5 hover:bg-primary/5 transition-colors"
              >
                <td class="px-4 py-3">
                  <div class="text-sm text-white font-medium font-mono">{{ config.configKey }}</div>
                </td>
                <td class="px-4 py-3">
                  <span class="px-2 py-0.5 rounded text-xs font-mono" :class="typeBadgeClass(config.configType)">
                    {{ config.configType }}
                  </span>
                </td>
                <td class="px-4 py-3 text-sm text-gray-300 max-w-xs truncate">{{ displayValue(config) }}</td>
                <td class="px-4 py-3 text-xs text-gray-500">{{ config.description || '—' }}</td>
                <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(config.updatedAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-end">
                    <button
                      class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                      title="编辑"
                      :disabled="!config.isEditable"
                      @click="openEdit(config)"
                    >
                      <Pencil :size="14" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- 编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">编辑配置项</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div v-if="editingConfig" class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">配置键</label>
              <div class="text-sm text-white font-mono px-4 py-2.5 rounded-xl bg-dark-surface/30 border border-primary/10">
                {{ editingConfig.configKey }}
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <div class="text-xs text-gray-500 px-4 py-2 rounded-xl bg-dark-surface/20">
                {{ editingConfig.description || '无描述' }}
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">
                配置值
                <span class="ml-2 text-xs text-gray-600">({{ editingConfig.configType }})</span>
              </label>

              <!-- BOOLEAN 类型用 switch -->
              <label
                v-if="editingConfig.configType === 'BOOLEAN'"
                class="flex items-center gap-3 cursor-pointer"
              >
                <button
                  type="button"
                  class="relative w-12 h-6 rounded-full transition-colors"
                  :class="formValue === 'true' ? 'bg-primary/60' : 'bg-dark-surface'"
                  @click="formValue = formValue === 'true' ? 'false' : 'true'"
                >
                  <span
                    class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform"
                    :class="formValue === 'true' ? 'translate-x-6' : 'translate-x-0'"
                  />
                </button>
                <span class="text-sm" :class="formValue === 'true' ? 'text-primary-light' : 'text-gray-500'">
                  {{ formValue === 'true' ? '已启用' : '已禁用' }}
                </span>
              </label>

              <!-- NUMBER 类型用 number input -->
              <input
                v-else-if="editingConfig.configType === 'NUMBER'"
                v-model="formValue" type="number"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none" />

              <!-- JSON 类型用 textarea -->
              <textarea
                v-else-if="editingConfig.configType === 'JSON'"
                v-model="formValue" rows="6"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white font-mono focus:border-primary/40 focus:outline-none resize-y" />

              <!-- STRING 类型用 input -->
              <input
                v-else
                v-model="formValue" type="text"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none" />
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm flex items-center justify-center gap-2" @click="handleSubmit">
              <Save :size="14" />
              保存
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
