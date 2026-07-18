<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { FileText, Plus, Pencil, Trash2, Copy, Search, X, Send, Archive, Edit3 } from 'lucide-vue-next'
import {
  getReports,
  createReport,
  updateReport,
  deleteReport,
  copyReport,
  updateReportStatus,
} from '@/api/report'
import type { Report, ReportCreateRequest, ReportType, ReportStatus } from '@/types/report'
import type { PageData } from '@/types/api'

const reports = ref<Report[]>([])
const loading = ref(true)
const keyword = ref('')
const typeFilter = ref<ReportType | ''>('')
const statusFilter = ref<ReportStatus | ''>('')

const currentPage = ref(0)
const pageSize = ref(20)
const total = ref(0)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<ReportCreateRequest>(getEmptyForm())

const typeOptions: { value: ReportType; label: string }[] = [
  { value: 'DASHBOARD', label: '仪表盘' },
  { value: 'TABLE', label: '表格' },
  { value: 'CHART', label: '图表' },
  { value: 'CUSTOM', label: '自定义' },
]

const statusOptions: { value: ReportStatus; label: string; color: string }[] = [
  { value: 'DRAFT', label: '草稿', color: 'bg-yellow-500/20 text-yellow-400' },
  { value: 'PUBLISHED', label: '已发布', color: 'bg-green-500/20 text-green-400' },
  { value: 'ARCHIVED', label: '已归档', color: 'bg-gray-500/20 text-gray-400' },
]

function getEmptyForm(): ReportCreateRequest {
  return {
    name: '',
    description: '',
    type: 'DASHBOARD',
    config: '',
    status: 'DRAFT',
    visibility: 'PRIVATE',
  }
}

onMounted(loadReports)

function loadReports() {
  loading.value = true
  getReports({
    keyword: keyword.value || undefined,
    type: typeFilter.value || undefined,
    status: statusFilter.value || undefined,
    page: currentPage.value,
    size: pageSize.value,
  })
    .then((res) => {
      const data = res.data.data as PageData<Report>
      reports.value = data.content ?? []
      total.value = data.totalElements ?? 0
    })
    .catch((e) => {
      console.error('加载报表失败', e)
      alert('加载报表失败: ' + (e as Error).message)
    })
    .finally(() => { loading.value = false })
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function openCreate() {
  editingId.value = null
  formData.value = getEmptyForm()
  showForm.value = true
}

function openEdit(report: Report) {
  editingId.value = report.id
  formData.value = {
    name: report.name,
    description: report.description ?? '',
    type: report.type,
    config: report.config ?? '',
    status: report.status,
    visibility: report.visibility,
    sceneId: report.sceneId ?? undefined,
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    alert('请输入报表名称')
    return
  }
  try {
    if (editingId.value) {
      await updateReport(editingId.value, formData.value)
    } else {
      await createReport(formData.value)
    }
    showForm.value = false
    loadReports()
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleCopy(report: Report) {
  try {
    await copyReport(report.id)
    loadReports()
  } catch (e: unknown) {
    alert('复制失败: ' + (e as Error).message)
  }
}

async function handleDelete(report: Report) {
  if (!confirm(`确定删除报表「${report.name}」？此操作不可撤销。`)) return
  try {
    await deleteReport(report.id)
    loadReports()
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function handleStatusChange(report: Report, status: ReportStatus) {
  try {
    await updateReportStatus(report.id, status)
    loadReports()
  } catch (e: unknown) {
    alert('状态更新失败: ' + (e as Error).message)
  }
}

function applyTypeFilter(value: ReportType | '') {
  typeFilter.value = value
  currentPage.value = 0
  loadReports()
}

function applyStatusFilter(value: ReportStatus | '') {
  statusFilter.value = value
  currentPage.value = 0
  loadReports()
}

function onSearchInput() {
  currentPage.value = 0
  loadReports()
}

function setPage(p: number) {
  currentPage.value = p
  loadReports()
}

function typeLabel(type: ReportType) {
  return typeOptions.find((o) => o.value === type)?.label || type
}

function statusInfo(status: ReportStatus) {
  return statusOptions.find((o) => o.value === status) || statusOptions[0]
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
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
          <FileText :size="24" class="text-primary-light" />
          报表管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ total }} 个报表</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新建报表
      </button>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="flex flex-wrap gap-3 mb-6">
      <div class="relative flex-1 min-w-[240px] max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索报表..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          @input="onSearchInput"
        />
      </div>
      <select
        :value="typeFilter"
        @change="applyTypeFilter(($event.target as HTMLSelectElement).value as ReportType | '')"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
      >
        <option value="">全部类型</option>
        <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <select
        :value="statusFilter"
        @change="applyStatusFilter(($event.target as HTMLSelectElement).value as ReportStatus | '')"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
      >
        <option value="">全部状态</option>
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="reports.length === 0" class="text-center text-gray-500 py-20">
      <FileText :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无报表</p>
      <p class="text-sm">点击上方按钮创建你的第一个报表</p>
    </div>

    <!-- 卡片网格 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="report in reports"
        :key="report.id"
        class="glass-card hover-glow group overflow-hidden"
      >
        <div class="aspect-video relative overflow-hidden bg-dark-surface/30 flex items-center justify-center">
          <FileText :size="48" class="text-gray-600" />
          <span
            class="absolute top-2 left-2 px-2 py-0.5 rounded-md text-[10px]"
            :class="statusInfo(report.status).color"
          >
            {{ statusInfo(report.status).label }}
          </span>
          <span class="absolute top-2 right-2 px-2 py-0.5 rounded-md text-[10px] bg-primary/15 text-primary-light">
            {{ typeLabel(report.type) }}
          </span>

          <div class="absolute bottom-2 right-2 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-all">
            <button
              v-if="report.status !== 'PUBLISHED'"
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-green-400 transition-colors"
              title="发布"
              @click="handleStatusChange(report, 'PUBLISHED')"
            >
              <Send :size="14" />
            </button>
            <button
              v-if="report.status !== 'ARCHIVED'"
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-yellow-400 transition-colors"
              title="归档"
              @click="handleStatusChange(report, 'ARCHIVED')"
            >
              <Archive :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-primary-light transition-colors"
              title="复制"
              @click="handleCopy(report)"
            >
              <Copy :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-primary-light transition-colors"
              title="编辑"
              @click="openEdit(report)"
            >
              <Pencil :size="14" />
            </button>
            <button
              class="p-1.5 rounded-md bg-dark/70 text-gray-300 hover:text-danger transition-colors"
              title="删除"
              @click="handleDelete(report)"
            >
              <Trash2 :size="14" />
            </button>
          </div>
        </div>

        <div class="p-3">
          <h3 class="text-sm font-medium text-white truncate flex items-center gap-1.5">
            <Edit3 :size="12" class="text-gray-500 shrink-0" />
            {{ report.name }}
          </h3>
          <p class="text-xs text-gray-500 mt-1 line-clamp-2 min-h-[1.5rem]">
            {{ report.description || '—' }}
          </p>
          <div class="flex items-center justify-between mt-2 text-[10px] text-gray-600">
            <span>{{ report.ownerName || '—' }}</span>
            <span>{{ formatDate(report.updatedAt) }}</span>
          </div>
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

    <!-- 新建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑报表' : '新建报表' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">报表名称</label>
              <input v-model="formData.name" type="text" placeholder="请输入报表名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <textarea v-model="formData.description" rows="2" placeholder="可选"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none resize-none"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">报表类型</label>
                <select v-model="formData.type"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                  <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">状态</label>
                <select v-model="formData.status"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                  <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">可见性</label>
              <select v-model="formData.visibility"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                <option value="PRIVATE">私有</option>
                <option value="DEPARTMENT_SHARED">部门共享</option>
                <option value="PUBLIC">公开</option>
              </select>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">报表配置 (JSON)</label>
              <textarea v-model="formData.config" rows="5" placeholder='{"layout": [], "components": [], "bindings": []}'
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-xs text-gray-300 placeholder-gray-600 focus:border-primary/40 focus:outline-none font-mono"></textarea>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formData.name.trim()" @click="handleSubmit">
              {{ editingId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
