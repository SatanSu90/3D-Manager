<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ScrollText, Search, Trash2, X, ChevronLeft, ChevronRight, Eye } from 'lucide-vue-next'
import { getOperationLogs, cleanupLogs } from '@/api/system'
import type { OperationLog } from '@/types/system'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)

const filters = ref({
  module: '',
  action: '',
  status: '',
  startDate: '',
  endDate: '',
})

const showDetail = ref(false)
const detailLog = ref<OperationLog | null>(null)

const moduleOptions = [
  { value: '', label: '全部模块' },
  { value: 'auth', label: '认证' },
  { value: 'user', label: '用户' },
  { value: 'role', label: '角色' },
  { value: 'department', label: '部门' },
  { value: 'model', label: '模型' },
  { value: 'scene', label: '场景' },
  { value: 'datasource', label: '数据源' },
  { value: 'analysis', label: '分析' },
  { value: 'indicator', label: '指标' },
  { value: 'category', label: '分类' },
  { value: 'tag', label: '标签' },
  { value: 'system', label: '系统' },
]

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILED', label: '失败' },
]

const moduleLabels: Record<string, string> = {
  auth: '认证',
  user: '用户',
  role: '角色',
  department: '部门',
  model: '模型',
  scene: '场景',
  datasource: '数据源',
  analysis: '分析',
  indicator: '指标',
  category: '分类',
  tag: '标签',
  system: '系统',
  unknown: '未知',
}

const actionLabels: Record<string, string> = {
  create: '创建',
  update: '更新',
  delete: '删除',
  login: '登录',
  logout: '登出',
  register: '注册',
  test: '测试',
  query: '查询',
  assign: '分配',
  removeUser: '移除用户',
  assignUser: '分配用户',
  assignRole: '分配角色',
  removeRole: '移除角色',
  updateStatus: '状态更新',
  batchUpdate: '批量更新',
  cleanup: '清理',
  operation: '操作',
}

const hasFilters = computed(() => {
  return !!(filters.value.module || filters.value.action || filters.value.status
    || filters.value.startDate || filters.value.endDate)
})

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: page.value,
      size: size.value,
    }
    if (filters.value.module) params.module = filters.value.module
    if (filters.value.action) params.action = filters.value.action
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate

    const res = await getOperationLogs(params as {
      module?: string; action?: string; status?: string;
      startDate?: string; endDate?: string; page?: number; size?: number;
    })
    const data = res.data.data
    logs.value = data.items ?? []
    total.value = data.total ?? 0
    page.value = data.page ?? 0
    totalPages.value = data.totalPages ?? 0
  } catch (e: unknown) {
    console.error('加载日志失败', e)
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  loadLogs()
}

function resetFilters() {
  filters.value = { module: '', action: '', status: '', startDate: '', endDate: '' }
  page.value = 0
  loadLogs()
}

function gotoPage(p: number) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadLogs()
}

function viewDetail(log: OperationLog) {
  detailLog.value = log
  showDetail.value = true
}

async function handleCleanup() {
  const daysStr = prompt('清理多少天前的日志？(默认30天)', '30')
  if (!daysStr) return
  const days = parseInt(daysStr, 10)
  if (isNaN(days) || days <= 0) {
    alert('请输入有效的天数')
    return
  }
  if (!confirm(`确定清理 ${days} 天前的所有操作日志？此操作不可撤销。`)) return
  try {
    const res = await cleanupLogs(days)
    alert(`成功清理 ${res.data.data.deleted} 条日志`)
    await loadLogs()
  } catch (e: unknown) {
    alert('清理失败: ' + (e as Error).message)
  }
}

function statusBadge(status: string): string {
  return status === 'SUCCESS'
    ? 'text-green-400 bg-green-500/10'
    : 'text-red-400 bg-red-500/10'
}

function methodBadge(method: string | null): string {
  if (!method) return 'text-gray-400 bg-gray-500/10'
  const map: Record<string, string> = {
    GET: 'text-blue-400 bg-blue-500/10',
    POST: 'text-green-400 bg-green-500/10',
    PUT: 'text-yellow-400 bg-yellow-500/10',
    DELETE: 'text-red-400 bg-red-500/10',
    PATCH: 'text-purple-400 bg-purple-500/10',
  }
  return map[method] || 'text-gray-400 bg-gray-500/10'
}

function moduleLabel(module: string): string {
  return moduleLabels[module] || module
}

function actionLabel(action: string): string {
  return actionLabels[action] || action
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

// 日期范围快捷选项
function setQuickRange(days: number) {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - days)
  filters.value.startDate = start.toISOString().slice(0, 19)
  filters.value.endDate = end.toISOString().slice(0, 19)
}
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <ScrollText :size="24" class="text-primary-light" />
          操作日志
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ total }} 条日志记录</p>
      </div>
      <button class="btn-ghost text-sm flex items-center gap-2 text-red-400 hover:text-red-300" @click="handleCleanup">
        <Trash2 :size="14" />
        清理旧日志
      </button>
    </div>

    <!-- 筛选 -->
    <div class="glass-card p-4 mb-4">
      <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <select
          v-model="filters.module"
          class="bg-dark-surface/50 border border-primary/15 rounded-xl px-3 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
        >
          <option v-for="opt in moduleOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <input
          v-model="filters.action" type="text" placeholder="操作类型"
          class="bg-dark-surface/50 border border-primary/15 rounded-xl px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
        <select
          v-model="filters.status"
          class="bg-dark-surface/50 border border-primary/15 rounded-xl px-3 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
        >
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <input
          v-model="filters.startDate" type="datetime-local"
          class="bg-dark-surface/50 border border-primary/15 rounded-xl px-3 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none" />
        <input
          v-model="filters.endDate" type="datetime-local"
          class="bg-dark-surface/50 border border-primary/15 rounded-xl px-3 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none" />
        <div class="flex gap-2">
          <button class="flex-1 btn-primary text-sm flex items-center justify-center gap-1" @click="applyFilters">
            <Search :size="14" />
            查询
          </button>
          <button v-if="hasFilters" class="px-3 btn-ghost text-sm" title="重置" @click="resetFilters">
            <X :size="14" />
          </button>
        </div>
      </div>
      <div class="flex gap-2 mt-2">
        <button class="text-xs text-gray-500 hover:text-primary-light" @click="setQuickRange(1)">近1天</button>
        <button class="text-xs text-gray-500 hover:text-primary-light" @click="setQuickRange(7)">近7天</button>
        <button class="text-xs text-gray-500 hover:text-primary-light" @click="setQuickRange(30)">近30天</button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="logs.length === 0" class="text-center text-gray-500 py-20">
      <ScrollText :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无日志记录</p>
    </div>

    <div v-else class="glass-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-primary/10">
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">时间</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">用户</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">模块</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">方法</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">状态</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">耗时</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">IP</th>
            <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="log in logs"
            :key="log.id"
            class="border-b border-primary/5 hover:bg-primary/5 transition-colors"
          >
            <td class="px-4 py-3 text-xs text-gray-400 whitespace-nowrap">{{ formatDate(log.createdAt) }}</td>
            <td class="px-4 py-3 text-sm text-white">{{ log.username || '—' }}</td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs bg-primary/10 text-primary-light">{{ moduleLabel(log.module) }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-300">{{ actionLabel(log.action) }}</td>
            <td class="px-4 py-3">
              <span v-if="log.requestMethod" class="px-2 py-0.5 rounded text-xs font-mono" :class="methodBadge(log.requestMethod)">
                {{ log.requestMethod }}
              </span>
              <span v-else class="text-gray-600">—</span>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs" :class="statusBadge(log.status)">{{ log.status === 'SUCCESS' ? '成功' : '失败' }}</span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ log.durationMs != null ? log.durationMs + 'ms' : '—' }}</td>
            <td class="px-4 py-3 text-xs text-gray-500 font-mono">{{ log.ipAddress || '—' }}</td>
            <td class="px-4 py-3">
              <div class="flex justify-end">
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                  title="查看详情"
                  @click="viewDetail(log)"
                >
                  <Eye :size="14" />
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
        class="p-2 rounded-md text-gray-400 hover:bg-primary/5 disabled:opacity-30 disabled:cursor-not-allowed"
        :disabled="page === 0"
        @click="gotoPage(page - 1)"
      >
        <ChevronLeft :size="16" />
      </button>
      <span class="text-sm text-gray-400 px-3">{{ page + 1 }} / {{ totalPages }}</span>
      <button
        class="p-2 rounded-md text-gray-400 hover:bg-primary/5 disabled:opacity-30 disabled:cursor-not-allowed"
        :disabled="page >= totalPages - 1"
        @click="gotoPage(page + 1)"
      >
        <ChevronRight :size="16" />
      </button>
    </div>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="showDetail && detailLog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showDetail = false">
        <div class="glass-card w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <ScrollText :size="18" class="text-primary-light" />
              日志详情 #{{ detailLog.id }}
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showDetail = false"><X :size="18" /></button>
          </div>

          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-4">
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">用户</div>
                <div class="text-sm text-white">{{ detailLog.username || '—' }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">IP 地址</div>
                <div class="text-sm text-white font-mono">{{ detailLog.ipAddress || '—' }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">模块</div>
                <div class="text-sm text-white">{{ moduleLabel(detailLog.module) }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">操作</div>
                <div class="text-sm text-white">{{ actionLabel(detailLog.action) }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">目标类型</div>
                <div class="text-sm text-white">{{ detailLog.targetType || '—' }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">目标 ID</div>
                <div class="text-sm text-white font-mono">{{ detailLog.targetId || '—' }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">请求方法</div>
                <div class="text-sm">
                  <span v-if="detailLog.requestMethod" class="px-2 py-0.5 rounded text-xs font-mono" :class="methodBadge(detailLog.requestMethod)">
                    {{ detailLog.requestMethod }}
                  </span>
                  <span v-else class="text-gray-600">—</span>
                </div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">状态</div>
                <div>
                  <span class="px-2 py-0.5 rounded text-xs" :class="statusBadge(detailLog.status)">
                    {{ detailLog.status === 'SUCCESS' ? '成功' : '失败' }}
                  </span>
                </div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">耗时</div>
                <div class="text-sm text-white">{{ detailLog.durationMs != null ? detailLog.durationMs + ' ms' : '—' }}</div>
              </div>
              <div class="p-3 rounded-lg bg-dark-surface/30">
                <div class="text-xs text-gray-500 mb-1">时间</div>
                <div class="text-sm text-white">{{ formatDate(detailLog.createdAt) }}</div>
              </div>
            </div>

            <div class="p-3 rounded-lg bg-dark-surface/30">
              <div class="text-xs text-gray-500 mb-1">描述</div>
              <div class="text-sm text-gray-300">{{ detailLog.description || '—' }}</div>
            </div>

            <div class="p-3 rounded-lg bg-dark-surface/30">
              <div class="text-xs text-gray-500 mb-1">请求 URL</div>
              <div class="text-sm text-gray-300 font-mono break-all">{{ detailLog.requestUrl || '—' }}</div>
            </div>

            <div v-if="detailLog.errorMessage" class="p-3 rounded-lg bg-red-500/10 border border-red-500/20">
              <div class="text-xs text-red-400 mb-1">错误信息</div>
              <div class="text-sm text-red-300 font-mono whitespace-pre-wrap">{{ detailLog.errorMessage }}</div>
            </div>
          </div>

          <div class="flex justify-end mt-6">
            <button class="btn-ghost text-sm" @click="showDetail = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
