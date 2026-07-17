<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Gauge, Plus, Pencil, Trash2, Search, X, Eye, Tag,
} from 'lucide-vue-next'
import * as indicatorApi from '@/api/indicator'
import type { PageData } from '@/types/api'
import type {
  Indicator, IndicatorType, IndicatorValueType, IndicatorVisibility,
  IndicatorCreateRequest,
} from '@/types/indicator'

const indicators = ref<Indicator[]>([])
const total = ref(0)
const currentPage = ref(0)
const pageSize = ref(20)
const loading = ref(false)
const keyword = ref('')
const typeFilter = ref<string>('')

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<IndicatorCreateRequest>(getEmptyForm())

const showViewDialog = ref(false)
const viewingIndicator = ref<Indicator | null>(null)

const typeOptions: { value: IndicatorType; label: string; class: string }[] = [
  { value: 'ATOMIC', label: '原子指标', class: 'text-cyan-400 bg-cyan-500/10' },
  { value: 'DERIVED', label: '派生指标', class: 'text-blue-400 bg-blue-500/10' },
  { value: 'COMPOSITE', label: '复合指标', class: 'text-purple-400 bg-purple-500/10' },
]

const valueTypeOptions: { value: IndicatorValueType; label: string }[] = [
  { value: 'NUMBER', label: '数值' },
  { value: 'STRING', label: '字符串' },
  { value: 'JSON', label: 'JSON' },
  { value: 'TABLE', label: '表格' },
]

const visibilityOptions: { value: IndicatorVisibility; label: string }[] = [
  { value: 'PRIVATE', label: '私有' },
  { value: 'DEPARTMENT_SHARED', label: '部门共享' },
  { value: 'PUBLIC', label: '公开' },
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function getEmptyForm(): IndicatorCreateRequest {
  return {
    name: '',
    description: '',
    type: 'ATOMIC',
    valueType: 'NUMBER',
    value: '',
    tags: '',
    visibility: 'PRIVATE',
  }
}

function typeMeta(type: IndicatorType) {
  return typeOptions.find((o) => o.value === type) || typeOptions[0]
}

function valueTypeLabel(vt: IndicatorValueType): string {
  return valueTypeOptions.find((o) => o.value === vt)?.label || vt
}

function visibilityLabel(v: IndicatorVisibility): string {
  return visibilityOptions.find((o) => o.value === v)?.label || v
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function truncate(s: string, n: number): string {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

function parseTags(tags?: string): string[] {
  if (!tags) return []
  return tags.split(',').map((t) => t.trim()).filter(Boolean)
}

onMounted(loadIndicators)

async function loadIndicators() {
  loading.value = true
  try {
    const res = await indicatorApi.getIndicators({
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    const page = res.data.data as PageData<Indicator>
    indicators.value = page.content
    total.value = page.totalElements
  } catch (e) {
    console.error('加载指标失败', e)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  currentPage.value = 0
  loadIndicators()
}

function onTypeFilterChange() {
  currentPage.value = 0
  loadIndicators()
}

function setPage(page: number) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  loadIndicators()
}

function openCreate() {
  editingId.value = null
  formData.value = getEmptyForm()
  showForm.value = true
}

function openEdit(ind: Indicator) {
  editingId.value = ind.id
  formData.value = {
    name: ind.name,
    description: ind.description,
    type: ind.type,
    valueType: ind.valueType,
    value: ind.value,
    tags: ind.tags || '',
    visibility: ind.visibility,
  }
  showForm.value = true
}

function openView(ind: Indicator) {
  viewingIndicator.value = ind
  showViewDialog.value = true
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    alert('请填写指标名称')
    return
  }
  if (!formData.value.value.trim()) {
    alert('请填写指标值')
    return
  }
  try {
    if (editingId.value) {
      await indicatorApi.updateIndicator(editingId.value, formData.value)
    } else {
      await indicatorApi.createIndicator(formData.value)
    }
    showForm.value = false
    await loadIndicators()
  } catch (e) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleDelete(ind: Indicator) {
  if (!confirm(`确定删除指标「${ind.name}」？`)) return
  try {
    await indicatorApi.deleteIndicator(ind.id)
    await loadIndicators()
  } catch (e) {
    alert('删除失败: ' + (e as Error).message)
  }
}

function formatValuePreview(ind: Indicator): string {
  return truncate(ind.value, 60)
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <Gauge :size="24" class="text-primary-light" />
          指标管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ total }} 个指标</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        创建指标
      </button>
    </div>

    <!-- 搜索与筛选 -->
    <div class="flex gap-4 mb-6">
      <div class="relative flex-1 max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索指标名称..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none"
          @keyup.enter="onSearch"
        />
      </div>
      <select
        v-model="typeFilter"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
        @change="onTypeFilterChange"
      >
        <option value="">全部类型</option>
        <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <button class="btn-ghost text-sm flex items-center gap-1.5" @click="onSearch">
        <Search :size="14" />
        搜索
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="indicators.length === 0" class="text-center text-gray-500 py-20">
      <Gauge :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无指标</p>
      <p class="text-sm">点击上方按钮创建你的第一个指标</p>
    </div>

    <!-- 指标列表 -->
    <div v-else class="glass-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-primary/10">
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">名称</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">类型</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">值类型</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">值预览</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">标签</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">来源</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">创建时间</th>
            <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="ind in indicators"
            :key="ind.id"
            class="border-b border-primary/5 hover:bg-primary/5 transition-colors"
          >
            <td class="px-4 py-3">
              <div class="text-sm text-white font-medium">{{ ind.name }}</div>
              <div class="text-xs text-gray-500">{{ ind.ownerUsername || `用户#${ind.ownerId}` }}</div>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs" :class="typeMeta(ind.type).class">
                {{ typeMeta(ind.type).label }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ valueTypeLabel(ind.valueType) }}</td>
            <td class="px-4 py-3 text-sm text-gray-400 max-w-xs">
              <span class="font-mono text-xs">{{ formatValuePreview(ind) }}</span>
            </td>
            <td class="px-4 py-3">
              <div v-if="parseTags(ind.tags).length > 0" class="flex flex-wrap gap-1">
                <span
                  v-for="t in parseTags(ind.tags)"
                  :key="t"
                  class="px-1.5 py-0.5 rounded text-xs bg-primary/10 text-primary-light"
                >
                  {{ t }}
                </span>
              </div>
              <span v-else class="text-xs text-gray-600">—</span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-500">
              <span v-if="ind.taskId">分析任务 #{{ ind.taskId }}</span>
              <span v-else-if="ind.dataSourceId">数据源 #{{ ind.dataSourceId }}</span>
              <span v-else>手动创建</span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(ind.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex justify-end gap-1">
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-green-400 transition-colors"
                  title="查看详情"
                  @click="openView(ind)"
                >
                  <Eye :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                  title="编辑"
                  @click="openEdit(ind)"
                >
                  <Pencil :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-danger transition-colors"
                  title="删除"
                  @click="handleDelete(ind)"
                >
                  <Trash2 :size="14" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-4">
      <button
        class="px-3 py-1 rounded text-sm transition-all text-gray-500 hover:bg-primary/5"
        :disabled="currentPage === 0"
        @click="setPage(currentPage - 1)"
      >
        上一页
      </button>
      <button
        v-for="p in totalPages"
        :key="p"
        class="px-3 py-1 rounded text-sm transition-all"
        :class="p - 1 === currentPage ? 'bg-primary/20 text-primary-light' : 'text-gray-500 hover:bg-primary/5'"
        @click="setPage(p - 1)"
      >
        {{ p }}
      </button>
      <button
        class="px-3 py-1 rounded text-sm transition-all text-gray-500 hover:bg-primary/5"
        :disabled="currentPage >= totalPages - 1"
        @click="setPage(currentPage + 1)"
      >
        下一页
      </button>
    </div>

    <!-- 创建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑指标' : '创建指标' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">名称 <span class="text-danger">*</span></label>
              <input
                v-model="formData.name"
                type="text"
                placeholder="指标名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none"
              />
            </div>

            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <textarea
                v-model="formData.description"
                rows="2"
                placeholder="指标描述"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">类型</label>
                <select
                  v-model="formData.type"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
                >
                  <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">值类型</label>
                <select
                  v-model="formData.valueType"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
                >
                  <option v-for="opt in valueTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-sm text-gray-400 mb-1.5">
                值 <span class="text-danger">*</span>
                <span class="text-xs text-gray-600 ml-1">
                 （{{ formData.valueType === 'JSON' ? 'JSON 格式' : formData.valueType === 'TABLE' ? 'JSON 表格' : '直接输入' }}）
                </span>
              </label>
              <textarea
                v-model="formData.value"
                rows="5"
                :placeholder="formData.valueType === 'JSON' || formData.valueType === 'TABLE' ? '{...}' : '指标值'"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white font-mono placeholder-gray-600 focus:border-primary/40 focus:outline-none"
              />
            </div>

            <div>
              <label class="block text-sm text-gray-400 mb-1.5">标签（逗号分隔）</label>
              <input
                v-model="formData.tags"
                type="text"
                placeholder="如: 销售,月度,营收"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none"
              />
            </div>

            <div>
              <label class="block text-sm text-gray-400 mb-1.5">可见性</label>
              <select
                v-model="formData.visibility"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
              >
                <option v-for="opt in visibilityOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formData.name.trim() || !formData.value.trim()" @click="handleSubmit">
              {{ editingId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 查看详情弹窗 -->
    <Teleport to="body">
      <div v-if="showViewDialog && viewingIndicator" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showViewDialog = false">
        <div class="glass-card w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Gauge :size="18" class="text-primary-light" />
              {{ viewingIndicator.name }}
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showViewDialog = false"><X :size="18" /></button>
          </div>

          <div class="space-y-3">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="px-2 py-0.5 rounded text-xs" :class="typeMeta(viewingIndicator.type).class">
                {{ typeMeta(viewingIndicator.type).label }}
              </span>
              <span class="px-2 py-0.5 rounded text-xs bg-primary/10 text-primary-light">
                {{ valueTypeLabel(viewingIndicator.valueType) }}
              </span>
              <span class="px-2 py-0.5 rounded text-xs bg-gray-500/10 text-gray-400">
                {{ visibilityLabel(viewingIndicator.visibility) }}
              </span>
              <span v-for="t in parseTags(viewingIndicator.tags)" :key="t"
                class="px-1.5 py-0.5 rounded text-xs bg-primary/10 text-primary-light flex items-center gap-0.5"
              >
                <Tag :size="10" />
                {{ t }}
              </span>
            </div>

            <div v-if="viewingIndicator.description" class="text-sm text-gray-300">
              {{ viewingIndicator.description }}
            </div>

            <div>
              <div class="text-xs text-gray-500 mb-1">值</div>
              <pre class="bg-dark-surface/70 border border-primary/10 rounded-lg p-3 text-xs text-gray-300 font-mono whitespace-pre-wrap break-all">{{ viewingIndicator.value }}</pre>
            </div>

            <div class="grid grid-cols-2 gap-3 text-xs">
              <div>
                <span class="text-gray-500">所有者:</span>
                <span class="text-gray-300 ml-1">{{ viewingIndicator.ownerUsername || `#${viewingIndicator.ownerId}` }}</span>
              </div>
              <div v-if="viewingIndicator.dataSourceId">
                <span class="text-gray-500">数据源:</span>
                <span class="text-gray-300 ml-1">#{{ viewingIndicator.dataSourceId }}</span>
              </div>
              <div v-if="viewingIndicator.taskId">
                <span class="text-gray-500">分析任务:</span>
                <span class="text-gray-300 ml-1">#{{ viewingIndicator.taskId }}</span>
              </div>
              <div>
                <span class="text-gray-500">创建:</span>
                <span class="text-gray-300 ml-1">{{ formatDate(viewingIndicator.createdAt) }}</span>
              </div>
              <div>
                <span class="text-gray-500">更新:</span>
                <span class="text-gray-300 ml-1">{{ formatDate(viewingIndicator.updatedAt) }}</span>
              </div>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showViewDialog = false">关闭</button>
            <button class="flex-1 btn-primary text-sm flex items-center justify-center gap-1.5" @click="() => { const ind = viewingIndicator!; showViewDialog = false; openEdit(ind) }">
              <Pencil :size="14" />
              编辑
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
