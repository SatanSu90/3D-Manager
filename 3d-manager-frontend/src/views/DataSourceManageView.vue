<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Database, Plus, Pencil, Trash2, Plug, Table2, Eye, Search, ChevronRight, X } from 'lucide-vue-next'
import { useDataSourceStore } from '@/stores/datasource'
import type { DataSource, DataSourceType, DataSourceCreateRequest } from '@/types/datasource'
import type { TableInfo, ColumnInfo, DataPreviewResult, ConnectionTestResult } from '@/types/datasource'

const dsStore = useDataSourceStore()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const formData = ref<DataSourceCreateRequest>(getEmptyForm())
const testing = ref(false)
const testResult = ref<ConnectionTestResult | null>(null)

// 表浏览
const showTableBrowser = ref(false)
const browsingDs = ref<DataSource | null>(null)
const tables = ref<TableInfo[]>([])
const selectedTable = ref<string | null>(null)
const columns = ref<ColumnInfo[]>([])
const previewData = ref<DataPreviewResult | null>(null)
const loadingTables = ref(false)
const loadingColumns = ref(false)
const loadingPreview = ref(false)

const dbTypeOptions: { value: DataSourceType; label: string }[] = [
  { value: 'MYSQL', label: 'MySQL' },
  { value: 'POSTGRESQL', label: 'PostgreSQL' },
  { value: 'ORACLE', label: 'Oracle' },
  { value: 'SQLSERVER', label: 'SQL Server' },
  { value: 'DM', label: '达梦 DM' },
]

function getEmptyForm(): DataSourceCreateRequest {
  return {
    name: '',
    type: 'MYSQL',
    host: 'localhost',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    sslEnabled: false,
    visibility: 'PRIVATE',
  }
}

onMounted(() => {
  dsStore.loadDataSources()
})

function openCreate() {
  editingId.value = null
  formData.value = getEmptyForm()
  testResult.value = null
  showForm.value = true
}

function openEdit(ds: DataSource) {
  editingId.value = ds.id
  formData.value = {
    name: ds.name,
    type: ds.type,
    host: ds.host,
    port: ds.port,
    databaseName: ds.databaseName,
    username: ds.username,
    password: '',
    sslEnabled: ds.sslEnabled,
    visibility: ds.visibility,
  }
  testResult.value = null
  showForm.value = true
}

function onTypeChange() {
  const defaultPorts: Record<DataSourceType, number> = {
    MYSQL: 3306,
    POSTGRESQL: 5432,
    ORACLE: 1521,
    SQLSERVER: 1433,
    DM: 5236,
  }
  formData.value.port = defaultPorts[formData.value.type] || 3306
}

async function handleTestDirect() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await dsStore.testConnectionDirect({
      type: formData.value.type,
      host: formData.value.host,
      port: formData.value.port,
      databaseName: formData.value.databaseName,
      username: formData.value.username,
      password: formData.value.password,
      sslEnabled: formData.value.sslEnabled,
    })
  } catch (e: unknown) {
    testResult.value = { success: false, message: (e as Error).message }
  } finally {
    testing.value = false
  }
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await dsStore.updateDataSource(editingId.value, formData.value)
    } else {
      await dsStore.createDataSource(formData.value)
    }
    showForm.value = false
  } catch (e: unknown) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function handleDelete(ds: DataSource) {
  if (!confirm(`确定删除数据源「${ds.name}」？此操作不可撤销。`)) return
  try {
    await dsStore.deleteDataSource(ds.id)
  } catch (e: unknown) {
    alert('删除失败: ' + (e as Error).message)
  }
}

async function handleTestSaved(ds: DataSource) {
  try {
    const result = await dsStore.testConnection(ds.id)
    alert(result.success ? `连接成功！\n${result.databaseProductName} ${result.databaseProductVersion}` : `连接失败: ${result.message}`)
  } catch (e: unknown) {
    alert('测试失败: ' + (e as Error).message)
  }
}

async function openTableBrowser(ds: DataSource) {
  browsingDs.value = ds
  showTableBrowser.value = true
  selectedTable.value = null
  columns.value = []
  previewData.value = null
  loadingTables.value = true
  try {
    tables.value = await dsStore.getTables(ds.id)
  } catch (e: unknown) {
    alert('获取表列表失败: ' + (e as Error).message)
  } finally {
    loadingTables.value = false
  }
}

async function selectTable(tableName: string) {
  if (!browsingDs.value) return
  selectedTable.value = tableName
  columns.value = []
  previewData.value = null
  loadingColumns.value = true
  loadingPreview.value = true
  try {
    columns.value = await dsStore.getColumns(browsingDs.value.id, tableName)
  } catch (e: unknown) {
    console.error('获取字段失败', e)
  } finally {
    loadingColumns.value = false
  }
  try {
    previewData.value = await dsStore.previewData(browsingDs.value.id, tableName, 100)
  } catch (e: unknown) {
    console.error('预览数据失败', e)
  } finally {
    loadingPreview.value = false
  }
}

function statusBadge(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'text-green-400 bg-green-500/10',
    ERROR: 'text-red-400 bg-red-500/10',
    DISABLED: 'text-gray-400 bg-gray-500/10',
  }
  return map[status] || 'text-gray-400 bg-gray-500/10'
}

function typeLabel(type: string) {
  return dbTypeOptions.find((o) => o.value === type)?.label || type
}

function formatDate(dateStr: string | null): string {
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
          <Database :size="24" class="text-primary-light" />
          数据源管理
        </h1>
        <p class="text-sm text-gray-500 mt-1">共 {{ dsStore.total }} 个数据源</p>
      </div>
      <button class="btn-primary text-sm flex items-center gap-2" @click="openCreate">
        <Plus :size="16" />
        新建数据源
      </button>
    </div>

    <!-- 搜索 -->
    <div class="flex gap-4 mb-6">
      <div class="relative flex-1 max-w-md">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          :value="dsStore.keyword"
          @input="dsStore.setKeyword(($event.target as HTMLInputElement).value)"
          type="text"
          placeholder="搜索数据源..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
        />
      </div>
      <select
        :value="dsStore.typeFilter"
        @change="dsStore.setTypeFilter(($event.target as HTMLSelectElement).value)"
        class="bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
      >
        <option value="">全部类型</option>
        <option v-for="opt in dbTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <div v-if="dsStore.loading" class="flex items-center justify-center py-20">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <div v-else-if="dsStore.dataSources.length === 0" class="text-center text-gray-500 py-20">
      <Database :size="48" class="mx-auto mb-3 text-gray-700" />
      <p class="text-lg mb-2">暂无数据源</p>
      <p class="text-sm">点击上方按钮创建你的第一个数据源</p>
    </div>

    <!-- 数据源列表 -->
    <div v-else class="glass-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-primary/10">
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">名称</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">类型</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">主机</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">数据库</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">状态</th>
            <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">最后测试</th>
            <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="ds in dsStore.dataSources"
            :key="ds.id"
            class="border-b border-primary/5 hover:bg-primary/5 transition-colors"
          >
            <td class="px-4 py-3">
              <div class="text-sm text-white font-medium">{{ ds.name }}</div>
              <div class="text-xs text-gray-500">{{ ds.ownerName }}</div>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs bg-primary/10 text-primary-light">{{ typeLabel(ds.type) }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ ds.host }}:{{ ds.port }}</td>
            <td class="px-4 py-3 text-sm text-gray-400">{{ ds.databaseName }}</td>
            <td class="px-4 py-3">
              <span class="px-2 py-0.5 rounded text-xs" :class="statusBadge(ds.status)">{{ ds.status }}</span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(ds.lastTestTime) }}</td>
            <td class="px-4 py-3">
              <div class="flex justify-end gap-1">
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                  title="测试连接"
                  @click="handleTestSaved(ds)"
                >
                  <Plug :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-green-400 transition-colors"
                  title="浏览表"
                  @click="openTableBrowser(ds)"
                >
                  <Table2 :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors"
                  title="编辑"
                  @click="openEdit(ds)"
                >
                  <Pencil :size="14" />
                </button>
                <button
                  class="p-1.5 rounded-md hover:bg-primary/10 text-gray-400 hover:text-danger transition-colors"
                  title="删除"
                  @click="handleDelete(ds)"
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
    <div v-if="dsStore.total > dsStore.pageSize" class="flex items-center justify-center gap-2 mt-4">
      <button
        v-for="p in Math.ceil(dsStore.total / dsStore.pageSize)"
        :key="p"
        class="px-3 py-1 rounded text-sm transition-all"
        :class="p - 1 === dsStore.currentPage ? 'bg-primary/20 text-primary-light' : 'text-gray-500 hover:bg-primary/5'"
        @click="dsStore.setPage(p - 1)"
      >{{ p }}</button>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showForm = false">
        <div class="glass-card w-full max-w-lg mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">{{ editingId ? '编辑数据源' : '新建数据源' }}</h3>
            <button class="text-gray-500 hover:text-white" @click="showForm = false"><X :size="18" /></button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">名称</label>
              <input v-model="formData.name" type="text" placeholder="数据源名称"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">数据库类型</label>
                <select v-model="formData.type" @change="onTypeChange"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-gray-300 focus:border-primary/40 focus:outline-none">
                  <option v-for="opt in dbTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
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
            </div>
            <div class="grid grid-cols-3 gap-4">
              <div class="col-span-2">
                <label class="block text-sm text-gray-400 mb-1.5">主机地址</label>
                <input v-model="formData.host" type="text" placeholder="localhost"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">端口</label>
                <input v-model.number="formData.port" type="number"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">数据库名</label>
              <input v-model="formData.databaseName" type="text" placeholder="database_name"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">用户名</label>
                <input v-model="formData.username" type="text" placeholder="username"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">密码{{ editingId ? '（留空不修改）' : '' }}</label>
                <input v-model="formData.password" type="password" placeholder="••••••••"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none" />
              </div>
            </div>
            <label class="flex items-center gap-2 text-sm text-gray-400">
              <input type="checkbox" v-model="formData.sslEnabled" class="accent-primary" />
              启用 SSL
            </label>

            <!-- 测试结果 -->
            <div v-if="testResult" class="p-3 rounded-lg text-sm" :class="testResult.success ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'">
              {{ testResult.message }}
              <span v-if="testResult.databaseProductName" class="block text-xs mt-1 text-gray-500">
                {{ testResult.databaseProductName }} {{ testResult.databaseProductVersion }}
              </span>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm flex items-center justify-center gap-2" :disabled="testing" @click="handleTestDirect">
              <Plug :size="14" />
              {{ testing ? '测试中...' : '测试连接' }}
            </button>
            <button class="flex-1 btn-ghost text-sm" @click="showForm = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!formData.name.trim()" @click="handleSubmit">
              {{ editingId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 表浏览弹窗 -->
    <Teleport to="body">
      <div v-if="showTableBrowser" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showTableBrowser = false">
        <div class="glass-card w-full max-w-5xl mx-4 p-6 max-h-[90vh] overflow-hidden flex flex-col">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Table2 :size="18" class="text-primary-light" />
              {{ browsingDs?.name }} - 表浏览
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showTableBrowser = false"><X :size="18" /></button>
          </div>

          <div class="flex gap-4 flex-1 overflow-hidden">
            <!-- 表列表 -->
            <div class="w-56 shrink-0 overflow-y-auto">
              <div v-if="loadingTables" class="text-center text-gray-500 py-4">
                <div class="w-6 h-6 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto" />
              </div>
              <div v-else-if="tables.length === 0" class="text-sm text-gray-500 text-center py-4">暂无表</div>
              <div v-else class="space-y-0.5">
                <button
                  v-for="tbl in tables"
                  :key="tbl.name"
                  class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all flex items-center gap-2"
                  :class="selectedTable === tbl.name ? 'bg-primary/15 text-primary-light' : 'text-gray-400 hover:bg-primary/5'"
                  @click="selectTable(tbl.name)"
                >
                  <ChevronRight :size="12" class="shrink-0" />
                  <span class="truncate">{{ tbl.name }}</span>
                </button>
              </div>
            </div>

            <!-- 字段 & 数据预览 -->
            <div class="flex-1 overflow-y-auto" v-if="selectedTable">
              <!-- 字段信息 -->
              <div class="mb-4">
                <h4 class="text-sm font-medium text-gray-400 mb-2 flex items-center gap-2">
                  <Eye :size="14" />
                  字段信息
                </h4>
                <div v-if="loadingColumns" class="text-sm text-gray-500">加载中...</div>
                <table v-else class="w-full glass-panel">
                  <thead>
                    <tr class="border-b border-primary/10">
                      <th class="text-left px-3 py-2 text-xs text-gray-500">字段名</th>
                      <th class="text-left px-3 py-2 text-xs text-gray-500">类型</th>
                      <th class="text-left px-3 py-2 text-xs text-gray-500">长度</th>
                      <th class="text-left px-3 py-2 text-xs text-gray-500">可空</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="col in columns" :key="col.name" class="border-b border-primary/5">
                      <td class="px-3 py-1.5 text-sm text-white">{{ col.name }}</td>
                      <td class="px-3 py-1.5 text-sm text-gray-400">{{ col.type }}</td>
                      <td class="px-3 py-1.5 text-sm text-gray-400">{{ col.size }}</td>
                      <td class="px-3 py-1.5 text-sm" :class="col.nullable ? 'text-green-400' : 'text-red-400'">{{ col.nullable ? '是' : '否' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 数据预览 -->
              <div>
                <h4 class="text-sm font-medium text-gray-400 mb-2 flex items-center gap-2">
                  <Eye :size="14" />
                  数据预览 (前100条)
                </h4>
                <div v-if="loadingPreview" class="text-sm text-gray-500">加载中...</div>
                <div v-else-if="previewData && previewData.rows.length > 0" class="overflow-x-auto">
                  <table class="w-full glass-panel">
                    <thead>
                      <tr class="border-b border-primary/10">
                        <th v-for="col in previewData.columns" :key="col" class="text-left px-3 py-2 text-xs text-gray-500 whitespace-nowrap">{{ col }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, idx) in previewData.rows" :key="idx" class="border-b border-primary/5">
                        <td v-for="col in previewData.columns" :key="col" class="px-3 py-1.5 text-sm text-gray-400 whitespace-nowrap max-w-xs truncate">{{ row[col] ?? 'NULL' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="text-sm text-gray-500">暂无数据</div>
              </div>
            </div>
            <div v-else class="flex-1 flex items-center justify-center text-gray-500">
              <div class="text-center">
                <Table2 :size="40" class="mx-auto mb-2 text-gray-700" />
                <p class="text-sm">选择左侧表名查看字段和数据</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
