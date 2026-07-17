<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3, Plus, Play, Save, Trash2, X, Database, ChevronDown, ChevronRight,
  Sparkles, Filter, Calculator, Brain, Eraser, RefreshCw, CheckCircle2, XCircle,
  Clock, ListTree, Table2, FileJson, Search, Pencil,
} from 'lucide-vue-next'
import * as analysisApi from '@/api/analysis'
import { getDataSources, getColumns } from '@/api/datasource'
import { useDataSourceStore } from '@/stores/datasource'
import type { PageData } from '@/types/api'
import type { DataSource, ColumnInfo } from '@/types/datasource'
import type {
  AnalysisTask, AnalysisStep, AnalysisDataSourceRef, AnalysisResult,
  AnalysisStepType, AnalysisTaskStatus,
  CleanConfig, FilterConfig, StatsConfig, AdvancedStatsConfig, MLConfig,
} from '@/types/analysis'

const route = useRoute()
const router = useRouter()
const dsStore = useDataSourceStore()

// 任务列表与当前任务
const tasks = ref<AnalysisTask[]>([])
const currentTask = ref<AnalysisTask | null>(null)
const loadingTasks = ref(false)
const loadingDetail = ref(false)
const executing = ref(false)
const showTaskList = ref(false)

// 顶部任务名称编辑
const taskNameDraft = ref('')
const taskDescDraft = ref('')

// 数据源关联
const showDsDialog = ref(false)
const dsCandidates = ref<DataSource[]>([])
const loadingDsCandidates = ref(false)
const newDsAlias = ref('')
const newDsTable = ref('')
const newDsSql = ref('')
const newDsMode = ref<'TABLE' | 'SQL'>('TABLE')
const selectedDsRefId = ref<number | null>(null)
const fields = ref<ColumnInfo[]>([])
const loadingFields = ref(false)
const dsTables = ref<string[]>([])

// 步骤管理
const expandedStepId = ref<number | null>(null)
const showStepTypePicker = ref(false)
const stepConfigDrafts = ref<Record<number, string>>({})
const savingStepId = ref<number | null>(null)

// 结果
const result = ref<AnalysisResult | null>(null)
const loadingResult = ref(false)
const resultViewMode = ref<'TABLE' | 'JSON'>('TABLE')

// 保存为指标弹窗
const showSaveDialog = ref(false)
const saveForm = ref({ name: '', description: '', type: 'DERIVED', valueType: 'TABLE', tags: '' })

// 轮询
let pollTimer: ReturnType<typeof setTimeout> | null = null

const statusMap: Record<AnalysisTaskStatus, { label: string; class: string }> = {
  DRAFT: { label: '草稿', class: 'text-gray-400 bg-gray-500/10' },
  RUNNING: { label: '运行中', class: 'text-blue-400 bg-blue-500/10' },
  SUCCESS: { label: '成功', class: 'text-green-400 bg-green-500/10' },
  FAILED: { label: '失败', class: 'text-red-400 bg-red-500/10' },
}

const stepTypeMeta: Record<AnalysisStepType, { label: string; icon: typeof Eraser; color: string }> = {
  CLEAN: { label: '数据清洗', icon: Eraser, color: 'text-cyan-400' },
  FILTER: { label: '数据过滤', icon: Filter, color: 'text-blue-400' },
  STATS: { label: '基础统计', icon: Calculator, color: 'text-green-400' },
  ADVANCED_STATS: { label: '高级统计', icon: ListTree, color: 'text-amber-400' },
  ML: { label: '机器学习', icon: Brain, color: 'text-purple-400' },
}

const stepStatusMeta = {
  PENDING: { label: '待执行', class: 'text-gray-400 bg-gray-500/10' },
  SUCCESS: { label: '成功', class: 'text-green-400 bg-green-500/10' },
  FAILED: { label: '失败', class: 'text-red-400 bg-red-500/10' },
}

const statsMethods = ['COUNT', 'MEAN', 'MEDIAN', 'STD_DEV', 'MIN', 'MAX', 'SUM', 'Q1', 'Q3']
const filterOperators = ['=', '!=', '>', '<', '>=', '<=', 'LIKE', 'IN', 'IS_NULL', 'IS_NOT_NULL']
const mlAlgorithms: { value: MLConfig['algorithm']; label: string }[] = [
  { value: 'LINEAR_REGRESSION', label: '线性回归' },
  { value: 'KMEANS', label: 'K-Means 聚类' },
  { value: 'DECISION_TREE', label: '决策树' },
]

const allFields = computed<string[]>(() => fields.value.map((f) => f.name))

const sortedSteps = computed<AnalysisStep[]>(() => {
  if (!currentTask.value?.steps) return []
  return [...currentTask.value.steps].sort((a, b) => a.stepOrder - b.stepOrder)
})

onMounted(async () => {
  await loadTaskList()
  const taskIdParam = route.query.taskId
  if (taskIdParam) {
    const id = Number(taskIdParam)
    if (!Number.isNaN(id)) {
      await selectTask(id)
      return
    }
  }
  showTaskList.value = tasks.value.length === 0
})

onUnmounted(() => {
  if (pollTimer) clearTimeout(pollTimer)
})

watch(() => route.query.taskId, (val) => {
  if (val && Number(val) !== currentTask.value?.id) {
    selectTask(Number(val))
  }
})

async function loadTaskList() {
  loadingTasks.value = true
  try {
    const res = await analysisApi.getAnalysisTasks({ page: 0, size: 50 })
    const page = res.data.data as PageData<AnalysisTask>
    tasks.value = page.content
  } catch (e) {
    console.error('加载任务列表失败', e)
  } finally {
    loadingTasks.value = false
  }
}

async function selectTask(id: number) {
  loadingDetail.value = true
  showTaskList.value = false
  try {
    const res = await analysisApi.getAnalysisTask(id)
    currentTask.value = res.data.data
    taskNameDraft.value = currentTask.value.name
    taskDescDraft.value = currentTask.value.description || ''
    expandedStepId.value = null
    stepConfigDrafts.value = {}
    result.value = null
    if (currentTask.value.status === 'SUCCESS') {
      loadResult(id)
    }
    if (currentTask.value.dataSources && currentTask.value.dataSources.length > 0) {
      selectedDsRefId.value = currentTask.value.dataSources[0].id
      await loadFieldsForRef(currentTask.value.dataSources[0])
    }
    if (currentTask.value.status === 'RUNNING') {
      startPolling(id)
    }
  } catch (e) {
    console.error('加载任务详情失败', e)
  } finally {
    loadingDetail.value = false
  }
}

async function createNewTask() {
  const name = `分析任务-${new Date().toLocaleString('zh-CN')}`
  try {
    const res = await analysisApi.createAnalysisTask({ name, description: '' })
    const task = res.data.data
    await loadTaskList()
    await selectTask(task.id)
  } catch (e) {
    alert('创建任务失败: ' + (e as Error).message)
  }
}

async function saveTaskMeta() {
  if (!currentTask.value) return
  try {
    await analysisApi.updateAnalysisTask(currentTask.value.id, {
      name: taskNameDraft.value,
      description: taskDescDraft.value,
    })
    currentTask.value.name = taskNameDraft.value
    currentTask.value.description = taskDescDraft.value
    await loadTaskList()
  } catch (e) {
    alert('保存失败: ' + (e as Error).message)
  }
}

async function deleteCurrentTask() {
  if (!currentTask.value) return
  if (!confirm(`确定删除任务「${currentTask.value.name}」？`)) return
  try {
    await analysisApi.deleteAnalysisTask(currentTask.value.id)
    currentTask.value = null
    result.value = null
    await loadTaskList()
    showTaskList.value = true
  } catch (e) {
    alert('删除失败: ' + (e as Error).message)
  }
}

// 数据源关联
async function openDsDialog() {
  if (!currentTask.value) return
  showDsDialog.value = true
  newDsAlias.value = ''
  newDsTable.value = ''
  newDsSql.value = ''
  newDsMode.value = 'TABLE'
  loadingDsCandidates.value = true
  try {
    const res = await getDataSources({ page: 0, size: 100 })
    const page = res.data.data as PageData<DataSource>
    dsCandidates.value = page.content
    dsTables.value = []
  } catch (e) {
    console.error('加载数据源失败', e)
  } finally {
    loadingDsCandidates.value = false
  }
}

async function onPickCandidate(ds: DataSource) {
  newDsAlias.value = ds.name
  // 加载该数据源的表
  try {
    const tables = await dsStore.getTables(ds.id)
    dsTables.value = tables.map((t) => t.name)
  } catch (e) {
    console.error('加载表失败', e)
    dsTables.value = []
  }
  // 标记选中样式（用 alias 字段做简单指示）
  newDsAlias.value = ds.name
  ;(openDsDialog as unknown as { _pickedDsId?: number })._pickedDsId = ds.id
}

function getPickedDsId(): number | undefined {
  return (openDsDialog as unknown as { _pickedDsId?: number })._pickedDsId
}

async function confirmAddDs() {
  if (!currentTask.value) return
  const dsId = getPickedDsId()
  if (!dsId) {
    alert('请选择一个数据源')
    return
  }
  if (!newDsAlias.value.trim()) {
    alert('请填写别名')
    return
  }
  if (newDsMode.value === 'TABLE' && !newDsTable.value.trim()) {
    alert('请选择表')
    return
  }
  if (newDsMode.value === 'SQL' && !newDsSql.value.trim()) {
    alert('请填写 SQL')
    return
  }
  try {
    await analysisApi.addTaskDataSource(currentTask.value.id, {
      dataSourceId: dsId,
      alias: newDsAlias.value,
      tableName: newDsMode.value === 'TABLE' ? newDsTable.value : undefined,
      querySql: newDsMode.value === 'SQL' ? newDsSql.value : undefined,
    })
    showDsDialog.value = false
    await refreshCurrentTask()
  } catch (e) {
    alert('关联失败: ' + (e as Error).message)
  }
}

async function removeDsRef(dsRef: AnalysisDataSourceRef) {
  if (!currentTask.value) return
  if (!confirm(`移除数据源「${dsRef.alias}」？`)) return
  try {
    await analysisApi.removeTaskDataSource(currentTask.value.id, dsRef.id)
    if (selectedDsRefId.value === dsRef.id) {
      selectedDsRefId.value = null
      fields.value = []
    }
    await refreshCurrentTask()
  } catch (e) {
    alert('移除失败: ' + (e as Error).message)
  }
}

async function selectDsRef(dsRef: AnalysisDataSourceRef) {
  selectedDsRefId.value = dsRef.id
  await loadFieldsForRef(dsRef)
}

async function loadFieldsForRef(dsRef: AnalysisDataSourceRef) {
  fields.value = []
  loadingFields.value = true
  try {
    if (dsRef.tableName) {
      const cols = await getColumns(dsRef.dataSourceId, dsRef.tableName)
      fields.value = cols
    }
    // SQL 模式无字段元数据，留空
  } catch (e) {
    console.error('加载字段失败', e)
  } finally {
    loadingFields.value = false
  }
}

async function refreshCurrentTask() {
  if (!currentTask.value) return
  const res = await analysisApi.getAnalysisTask(currentTask.value.id)
  currentTask.value = res.data.data
}

// 步骤管理
function openStepPicker() {
  if (!currentTask.value) return
  showStepTypePicker.value = true
}

async function addStep(type: AnalysisStepType) {
  if (!currentTask.value) return
  showStepTypePicker.value = false
  const meta = stepTypeMeta[type]
  const order = (currentTask.value.steps?.length ?? 0) + 1
  const initialConfig = getDefaultConfig(type)
  try {
    const res = await analysisApi.addAnalysisStep(currentTask.value.id, {
      stepType: type,
      stepName: `${meta.label} ${order}`,
      config: JSON.stringify(initialConfig),
      stepOrder: order,
    })
    await refreshCurrentTask()
    expandedStepId.value = res.data.data.id
    stepConfigDrafts.value[res.data.data.id] = JSON.stringify(initialConfig, null, 2)
  } catch (e) {
    alert('添加步骤失败: ' + (e as Error).message)
  }
}

function getDefaultConfig(type: AnalysisStepType): unknown {
  switch (type) {
    case 'CLEAN':
      return { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false } as CleanConfig
    case 'FILTER':
      return { conditions: [], logic: 'AND' } as FilterConfig
    case 'STATS':
      return { fields: [], methods: ['COUNT', 'MEAN'] } as StatsConfig
    case 'ADVANCED_STATS':
      return { type: 'CORRELATION', fields: [] } as AdvancedStatsConfig
    case 'ML':
      return { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 } as MLConfig
  }
}

async function deleteStep(step: AnalysisStep) {
  if (!confirm(`删除步骤「${step.stepName}」？`)) return
  try {
    await analysisApi.deleteAnalysisStep(step.id)
    await refreshCurrentTask()
  } catch (e) {
    alert('删除失败: ' + (e as Error).message)
  }
}

function toggleStep(step: AnalysisStep) {
  if (expandedStepId.value === step.id) {
    expandedStepId.value = null
  } else {
    expandedStepId.value = step.id
    if (!stepConfigDrafts.value[step.id]) {
      stepConfigDrafts.value[step.id] = step.config || '{}'
    }
  }
}

function getStepDraft(step: AnalysisStep): string {
  if (!stepConfigDrafts.value[step.id]) {
    stepConfigDrafts.value[step.id] = step.config || '{}'
  }
  return stepConfigDrafts.value[step.id]
}

function setStepDraft(step: AnalysisStep, val: string) {
  stepConfigDrafts.value[step.id] = val
}

// 解析配置为对象（用于表单双向绑定）
function parseConfig<T>(step: AnalysisStep, fallback: T): T {
  try {
    return JSON.parse(stepConfigDrafts.value[step.id] ?? step.config ?? '{}') as T
  } catch {
    return fallback
  }
}

function updateConfigField(step: AnalysisStep, patch: Record<string, unknown>) {
  const current = parseConfig<Record<string, unknown>>(step, {})
  const next = { ...current, ...patch }
  stepConfigDrafts.value[step.id] = JSON.stringify(next, null, 2)
  void saveStep(step)
}

function updateConfigFull(step: AnalysisStep, next: unknown) {
  stepConfigDrafts.value[step.id] = JSON.stringify(next, null, 2)
  void saveStep(step)
}

async function saveStep(step: AnalysisStep) {
  savingStepId.value = step.id
  try {
    await analysisApi.updateAnalysisStep(step.id, {
      config: stepConfigDrafts.value[step.id],
      stepName: step.stepName,
      stepType: step.stepType,
      stepOrder: step.stepOrder,
    })
    step.config = stepConfigDrafts.value[step.id]
  } catch (e) {
    console.error('保存步骤失败', e)
  } finally {
    savingStepId.value = null
  }
}

// 执行
async function execute() {
  if (!currentTask.value) return
  executing.value = true
  try {
    await analysisApi.executeAnalysisTask(currentTask.value.id)
    await refreshCurrentTask()
    startPolling(currentTask.value.id)
  } catch (e) {
    alert('执行失败: ' + (e as Error).message)
    executing.value = false
  }
}

function startPolling(taskId: number) {
  if (pollTimer) clearTimeout(pollTimer)
  pollTimer = setTimeout(() => pollStatus(taskId), 2000)
}

async function pollStatus(taskId: number) {
  try {
    const res = await analysisApi.getAnalysisTask(taskId)
    currentTask.value = res.data.data
    if (res.data.data.status === 'RUNNING') {
      pollTimer = setTimeout(() => pollStatus(taskId), 2000)
    } else {
      executing.value = false
      if (res.data.data.status === 'SUCCESS') {
        await loadResult(taskId)
      }
    }
  } catch {
    executing.value = false
  }
}

async function loadResult(taskId: number) {
  loadingResult.value = true
  try {
    const res = await analysisApi.getAnalysisResults(taskId)
    result.value = res.data.data
    // 自动选择结果展示模式：有列则表格，否则 JSON
    resultViewMode.value = (result.value?.columns?.length ?? 0) > 0 ? 'TABLE' : 'JSON'
  } catch (e) {
    console.error('加载结果失败', e)
  } finally {
    loadingResult.value = false
  }
}

// 保存为指标
function openSaveDialog() {
  if (!currentTask.value) return
  if (currentTask.value.status !== 'SUCCESS') {
    alert('请先执行任务并确保成功')
    return
  }
  saveForm.value = {
    name: currentTask.value.name + '-指标',
    description: currentTask.value.description || '',
    type: 'DERIVED',
    valueType: 'TABLE',
    tags: '',
  }
  showSaveDialog.value = true
}

async function confirmSaveIndicator() {
  if (!currentTask.value) return
  if (!saveForm.value.name.trim()) {
    alert('请填写指标名称')
    return
  }
  try {
    await analysisApi.saveAsIndicator(currentTask.value.id, saveForm.value)
    showSaveDialog.value = false
    alert('已保存为指标')
  } catch (e) {
    alert('保存失败: ' + (e as Error).message)
  }
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function stepResultSummary(step: AnalysisStep): string {
  if (!step.result) return ''
  try {
    const r = JSON.parse(step.result) as Record<string, unknown>
    const rowCount = r['rowCount'] ?? r['count'] ?? r['total']
    if (typeof rowCount === 'number') return `${rowCount} 行`
    return Object.keys(r).slice(0, 3).join(', ')
  } catch {
    return ''
  }
}

function previewJsonValue(val: unknown): string {
  if (val === null || val === undefined) return 'NULL'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

function truncate(s: string, n: number): string {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}
</script>

<template>
  <div class="p-6 h-full flex flex-col">
    <!-- 顶部标题栏 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-semibold text-white flex items-center gap-2">
          <BarChart3 :size="24" class="text-primary-light" />
          数据分析工作台
        </h1>
        <button
          class="text-xs px-2 py-1 rounded-md bg-primary/10 text-primary-light hover:bg-primary/20 transition-colors"
          @click="showTaskList = !showTaskList"
        >
          {{ showTaskList ? '收起任务列表' : '切换任务' }}
        </button>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm flex items-center gap-2" @click="createNewTask">
          <Plus :size="14" />
          新建任务
        </button>
      </div>
    </div>

    <!-- 任务列表面板 -->
    <div v-if="showTaskList" class="glass-card p-4 mb-4">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-medium text-gray-300">任务列表</h3>
        <button class="text-gray-500 hover:text-white" @click="showTaskList = false"><X :size="16" /></button>
      </div>
      <div v-if="loadingTasks" class="text-center py-4">
        <div class="w-6 h-6 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto" />
      </div>
      <div v-else-if="tasks.length === 0" class="text-center text-gray-500 py-6 text-sm">
        暂无任务，点击"新建任务"开始
      </div>
      <div v-else class="space-y-1 max-h-64 overflow-y-auto">
        <div
          v-for="t in tasks"
          :key="t.id"
          class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-primary/5 cursor-pointer transition-colors"
          :class="currentTask?.id === t.id ? 'bg-primary/10' : ''"
          @click="selectTask(t.id)"
        >
          <div class="flex-1">
            <div class="text-sm text-white">{{ t.name }}</div>
            <div class="text-xs text-gray-500">{{ formatDate(t.updatedAt) }}</div>
          </div>
          <span class="px-2 py-0.5 rounded text-xs" :class="statusMap[t.status].class">
            {{ statusMap[t.status].label }}
          </span>
        </div>
      </div>
    </div>

    <!-- 无任务空状态 -->
    <div v-if="!currentTask && !loadingDetail" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <BarChart3 :size="64" class="mx-auto mb-4 text-gray-700" />
        <p class="text-lg text-gray-400 mb-2">尚未选择分析任务</p>
        <p class="text-sm text-gray-600 mb-4">从上方任务列表选择，或创建新任务</p>
        <button class="btn-primary text-sm flex items-center gap-2 mx-auto" @click="createNewTask">
          <Plus :size="14" />
          创建第一个任务
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loadingDetail && !currentTask" class="flex-1 flex items-center justify-center">
      <div class="w-10 h-10 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin" />
    </div>

    <!-- 工作台主区域 -->
    <div v-else-if="currentTask" class="flex-1 flex flex-col gap-4 min-h-0">
      <!-- 任务管理条 -->
      <div class="glass-card p-4">
        <div class="flex items-center gap-3 flex-wrap">
          <input
            v-model="taskNameDraft"
            type="text"
            placeholder="任务名称"
            class="flex-1 min-w-[200px] bg-dark-surface/50 border border-primary/15 rounded-lg px-3 py-2 text-sm text-white focus:border-primary/40 focus:outline-none"
            @blur="saveTaskMeta"
          />
          <span class="px-2 py-1 rounded text-xs" :class="statusMap[currentTask.status].class">
            {{ statusMap[currentTask.status].label }}
          </span>
          <button
            class="btn-ghost text-sm flex items-center gap-1.5"
            :disabled="executing || currentTask.dataSources?.length === 0 || sortedSteps.length === 0"
            @click="execute"
          >
            <Play :size="14" />
            {{ executing ? '执行中...' : '执行分析' }}
          </button>
          <button class="btn-ghost text-sm flex items-center gap-1.5" @click="openSaveDialog">
            <Save :size="14" />
            保存为指标
          </button>
          <button
            class="p-2 rounded-md hover:bg-danger/10 text-gray-400 hover:text-danger transition-colors"
            title="删除任务"
            @click="deleteCurrentTask"
          >
            <Trash2 :size="14" />
          </button>
        </div>
        <div class="mt-3 flex items-center gap-2 text-xs text-gray-500">
          <Clock :size="12" />
          创建于 {{ formatDate(currentTask.createdAt) }}
          <span class="mx-1">·</span>
          最后执行 {{ formatDate(currentTask.executedAt) }}
          <span v-if="currentTask.errorMessage" class="text-red-400 ml-2">
            错误: {{ currentTask.errorMessage }}
          </span>
        </div>
      </div>

      <!-- 三栏工作台 -->
      <div class="flex-1 grid grid-cols-[240px_1fr_400px] gap-4 min-h-0">
        <!-- 左栏：数据源 -->
        <div class="glass-card p-3 flex flex-col min-h-0">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xs font-medium text-gray-400 uppercase">数据源</h3>
            <button
              class="p-1 rounded hover:bg-primary/10 text-primary-light transition-colors"
              title="关联数据源"
              @click="openDsDialog"
            >
              <Plus :size="14" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto space-y-1">
            <div v-if="!currentTask.dataSources || currentTask.dataSources.length === 0" class="text-xs text-gray-600 text-center py-4">
              点击 + 关联数据源
            </div>
            <div
              v-for="dsRef in currentTask.dataSources"
              :key="dsRef.id"
              class="p-2 rounded-lg cursor-pointer transition-colors"
              :class="selectedDsRefId === dsRef.id ? 'bg-primary/15 text-primary-light' : 'hover:bg-primary/5 text-gray-300'"
              @click="selectDsRef(dsRef)"
            >
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium truncate">{{ dsRef.alias }}</span>
                <button
                  class="p-0.5 rounded hover:bg-danger/10 text-gray-500 hover:text-danger"
                  @click.stop="removeDsRef(dsRef)"
                >
                  <X :size="12" />
                </button>
              </div>
              <div class="text-xs text-gray-500 truncate mt-0.5">
                {{ dsRef.dataSourceName || `数据源#${dsRef.dataSourceId}` }}
              </div>
              <div v-if="dsRef.tableName" class="text-xs text-gray-600 truncate">
                表: {{ dsRef.tableName }}
              </div>
              <div v-else-if="dsRef.querySql" class="text-xs text-gray-600 truncate">
                SQL: {{ truncate(dsRef.querySql, 30) }}
              </div>
            </div>
          </div>

          <!-- 字段列表 -->
          <div class="border-t border-primary/10 mt-2 pt-2">
            <div class="text-xs font-medium text-gray-400 uppercase mb-1.5">字段</div>
            <div v-if="loadingFields" class="text-xs text-gray-500">加载中...</div>
            <div v-else-if="fields.length === 0" class="text-xs text-gray-600">选择表类型数据源查看字段</div>
            <div v-else class="space-y-0.5 max-h-40 overflow-y-auto">
              <div
                v-for="f in fields"
                :key="f.name"
                class="flex items-center justify-between text-xs px-2 py-1 rounded hover:bg-primary/5"
              >
                <span class="text-gray-300 truncate">{{ f.name }}</span>
                <span class="text-gray-600 ml-2 shrink-0">{{ f.type }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 中栏：分析步骤 -->
        <div class="glass-card p-4 flex flex-col min-h-0">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-medium text-gray-300 flex items-center gap-2">
              <Sparkles :size="14" class="text-primary-light" />
              分析步骤
            </h3>
            <button
              class="btn-primary text-xs flex items-center gap-1 px-3 py-1.5"
              :disabled="!currentTask.dataSources || currentTask.dataSources.length === 0"
              @click="openStepPicker"
            >
              <Plus :size="12" />
              添加步骤
            </button>
          </div>

          <div class="flex-1 overflow-y-auto space-y-2">
            <div v-if="sortedSteps.length === 0" class="text-center text-gray-600 py-8 text-sm">
              暂无步骤，点击"添加步骤"开始构建分析流程
            </div>

            <div
              v-for="step in sortedSteps"
              :key="step.id"
              class="border border-primary/10 rounded-lg overflow-hidden"
            >
              <!-- 步骤头 -->
              <div
                class="flex items-center gap-2 p-2.5 cursor-pointer hover:bg-primary/5 transition-colors"
                @click="toggleStep(step)"
              >
                <component
                  :is="stepTypeMeta[step.stepType].icon"
                  :size="14"
                  :class="stepTypeMeta[step.stepType].color"
                />
                <span class="text-xs text-gray-500">#{{ step.stepOrder }}</span>
                <span class="text-sm text-white flex-1 truncate">{{ step.stepName }}</span>
                <span v-if="stepResultSummary(step)" class="text-xs text-gray-500">{{ stepResultSummary(step) }}</span>
                <span class="px-1.5 py-0.5 rounded text-xs" :class="stepStatusMeta[step.status].class">
                  {{ stepStatusMeta[step.status].label }}
                </span>
                <button
                  class="p-1 rounded hover:bg-danger/10 text-gray-500 hover:text-danger"
                  @click.stop="deleteStep(step)"
                >
                  <Trash2 :size="12" />
                </button>
                <ChevronDown v-if="expandedStepId === step.id" :size="14" class="text-gray-500" />
                <ChevronRight v-else :size="14" class="text-gray-500" />
              </div>

              <!-- 步骤配置面板 -->
              <div v-if="expandedStepId === step.id" class="border-t border-primary/10 p-3 bg-dark-surface/30">
                <div class="flex items-center justify-between mb-2">
                  <input
                    :value="step.stepName"
                    type="text"
                    class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-white focus:border-primary/40 focus:outline-none w-48"
                    @input="step.stepName = ($event.target as HTMLInputElement).value"
                    @blur="saveStep(step)"
                  />
                  <span v-if="savingStepId === step.id" class="text-xs text-gray-500">保存中...</span>
                </div>

                <!-- CLEAN 配置 -->
                <div v-if="step.stepType === 'CLEAN'" class="space-y-2 text-sm">
                  <label class="flex items-center gap-2 text-gray-300">
                    <input
                      type="checkbox"
                      :checked="parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).removeNull"
                      class="accent-primary"
                      @change="updateConfigField(step, { removeNull: ($event.target as HTMLInputElement).checked })"
                    />
                    移除空值
                  </label>
                  <div v-if="parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).removeNull" class="ml-6">
                    <label class="block text-xs text-gray-400 mb-1">空值策略</label>
                    <select
                      :value="parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).nullStrategy"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300"
                      @change="updateConfigField(step, { nullStrategy: ($event.target as HTMLSelectElement).value })"
                    >
                      <option value="DROP_ROW">删除行</option>
                      <option value="FILL_DEFAULT">填充默认值</option>
                    </select>
                  </div>
                  <label class="flex items-center gap-2 text-gray-300">
                    <input
                      type="checkbox"
                      :checked="parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).dedup"
                      class="accent-primary"
                      @change="updateConfigField(step, { dedup: ($event.target as HTMLInputElement).checked })"
                    />
                    去重
                  </label>
                  <div v-if="parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).dedup" class="ml-6">
                    <label class="block text-xs text-gray-400 mb-1">去重字段（逗号分隔）</label>
                    <input
                      type="text"
                      :value="(parseConfig<CleanConfig>(step, { removeNull: false, nullStrategy: 'DROP_ROW', dedup: false }).dedupFields || []).join(',')"
                      placeholder="如: id,name"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-white w-full"
                      @blur="updateConfigField(step, { dedupFields: ($event.target as HTMLInputElement).value.split(',').map(s => s.trim()).filter(Boolean) })"
                    />
                  </div>
                </div>

                <!-- FILTER 配置 -->
                <div v-else-if="step.stepType === 'FILTER'" class="space-y-2 text-sm">
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-gray-400">逻辑:</span>
                    <select
                      :value="parseConfig<FilterConfig>(step, { conditions: [], logic: 'AND' }).logic"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300"
                      @change="updateConfigField(step, { logic: ($event.target as HTMLSelectElement).value })"
                    >
                      <option value="AND">AND (全部满足)</option>
                      <option value="OR">OR (任一满足)</option>
                    </select>
                  </div>
                  <div class="space-y-1">
                    <div
                      v-for="(cond, idx) in parseConfig<FilterConfig>(step, { conditions: [], logic: 'AND' }).conditions"
                      :key="idx"
                      class="flex items-center gap-1"
                    >
                      <select
                        v-model="cond.field"
                        class="bg-dark-surface/50 border border-primary/15 rounded px-1.5 py-1 text-xs text-gray-300 flex-1"
                      >
                        <option value="">字段</option>
                        <option v-for="f in allFields" :key="f" :value="f">{{ f }}</option>
                      </select>
                      <select
                        v-model="cond.operator"
                        class="bg-dark-surface/50 border border-primary/15 rounded px-1.5 py-1 text-xs text-gray-300"
                      >
                        <option v-for="op in filterOperators" :key="op" :value="op">{{ op }}</option>
                      </select>
                      <input
                        v-model="cond.value as string"
                        type="text"
                        placeholder="值"
                        class="bg-dark-surface/50 border border-primary/15 rounded px-1.5 py-1 text-xs text-white w-24"
                      />
                      <button
                        class="p-1 rounded hover:bg-danger/10 text-gray-500 hover:text-danger"
                        @click="() => {
                          const cfg = parseConfig<FilterConfig>(step, { conditions: [], logic: 'AND' })
                          cfg.conditions.splice(idx, 1)
                          updateConfigFull(step, cfg)
                        }"
                      >
                        <X :size="12" />
                      </button>
                    </div>
                  </div>
                  <button
                    class="text-xs text-primary-light hover:underline flex items-center gap-1"
                    @click="() => {
                      const cfg = parseConfig<FilterConfig>(step, { conditions: [], logic: 'AND' })
                      cfg.conditions.push({ field: '', operator: '=', value: '' })
                      updateConfigFull(step, cfg)
                    }"
                  >
                    <Plus :size="10" />
                    添加条件
                  </button>
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-gray-400">Limit:</span>
                    <input
                      type="number"
                      :value="parseConfig<FilterConfig>(step, { conditions: [], logic: 'AND' }).limit"
                      placeholder="不限"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-white w-24"
                      @blur="updateConfigField(step, { limit: Number(($event.target as HTMLInputElement).value) || undefined })"
                    />
                  </div>
                </div>

                <!-- STATS 配置 -->
                <div v-else-if="step.stepType === 'STATS'" class="space-y-2 text-sm">
                  <div>
                    <label class="block text-xs text-gray-400 mb-1">统计字段</label>
                    <div class="flex flex-wrap gap-1">
                      <label
                        v-for="f in allFields"
                        :key="f"
                        class="flex items-center gap-1 text-xs text-gray-300 px-1.5 py-0.5 rounded bg-dark-surface/40"
                      >
                        <input
                          type="checkbox"
                          :checked="(parseConfig<StatsConfig>(step, { fields: [], methods: [] }).fields || []).includes(f)"
                          class="accent-primary"
                          @change="() => {
                            const cfg = parseConfig<StatsConfig>(step, { fields: [], methods: [] })
                            const set = new Set(cfg.fields || [])
                            if (($event.target as HTMLInputElement).checked) set.add(f)
                            else set.delete(f)
                            cfg.fields = Array.from(set)
                            updateConfigFull(step, cfg)
                          }"
                        />
                        {{ f }}
                      </label>
                    </div>
                    <div v-if="allFields.length === 0" class="text-xs text-gray-600">请先在左栏选择数据源</div>
                  </div>
                  <div>
                    <label class="block text-xs text-gray-400 mb-1">统计方法</label>
                    <div class="flex flex-wrap gap-1">
                      <label
                        v-for="m in statsMethods"
                        :key="m"
                        class="flex items-center gap-1 text-xs text-gray-300 px-1.5 py-0.5 rounded bg-dark-surface/40"
                      >
                        <input
                          type="checkbox"
                          :checked="(parseConfig<StatsConfig>(step, { fields: [], methods: [] }).methods || []).includes(m)"
                          class="accent-primary"
                          @change="() => {
                            const cfg = parseConfig<StatsConfig>(step, { fields: [], methods: [] })
                            const set = new Set(cfg.methods || [])
                            if (($event.target as HTMLInputElement).checked) set.add(m)
                            else set.delete(m)
                            cfg.methods = Array.from(set)
                            updateConfigFull(step, cfg)
                          }"
                        />
                        {{ m }}
                      </label>
                    </div>
                  </div>
                </div>

                <!-- ADVANCED_STATS 配置 -->
                <div v-else-if="step.stepType === 'ADVANCED_STATS'" class="space-y-2 text-sm">
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-gray-400">类型:</span>
                    <select
                      :value="parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).type"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300"
                      @change="updateConfigField(step, { type: ($event.target as HTMLSelectElement).value })"
                    >
                      <option value="CORRELATION">相关性分析</option>
                      <option value="GROUP_BY">分组聚合</option>
                      <option value="CROSS_TAB">交叉表</option>
                    </select>
                  </div>

                  <div v-if="parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).type === 'CORRELATION'">
                    <label class="block text-xs text-gray-400 mb-1">分析字段</label>
                    <div class="flex flex-wrap gap-1">
                      <label
                        v-for="f in allFields"
                        :key="f"
                        class="flex items-center gap-1 text-xs text-gray-300 px-1.5 py-0.5 rounded bg-dark-surface/40"
                      >
                        <input
                          type="checkbox"
                          :checked="(parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).fields || []).includes(f)"
                          class="accent-primary"
                          @change="() => {
                            const cfg = parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] })
                            const set = new Set(cfg.fields || [])
                            if (($event.target as HTMLInputElement).checked) set.add(f)
                            else set.delete(f)
                            cfg.fields = Array.from(set)
                            updateConfigFull(step, cfg)
                          }"
                        />
                        {{ f }}
                      </label>
                    </div>
                  </div>

                  <div v-else-if="parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).type === 'CROSS_TAB'">
                    <div class="grid grid-cols-2 gap-2">
                      <div>
                        <label class="block text-xs text-gray-400 mb-1">行字段</label>
                        <select
                          :value="parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).rowField"
                          class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300 w-full"
                          @change="updateConfigField(step, { rowField: ($event.target as HTMLSelectElement).value })"
                        >
                          <option value="">选择</option>
                          <option v-for="f in allFields" :key="f" :value="f">{{ f }}</option>
                        </select>
                      </div>
                      <div>
                        <label class="block text-xs text-gray-400 mb-1">列字段</label>
                        <select
                          :value="parseConfig<AdvancedStatsConfig>(step, { type: 'CORRELATION', fields: [] }).colField"
                          class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300 w-full"
                          @change="updateConfigField(step, { colField: ($event.target as HTMLSelectElement).value })"
                        >
                          <option value="">选择</option>
                          <option v-for="f in allFields" :key="f" :value="f">{{ f }}</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- ML 配置 -->
                <div v-else-if="step.stepType === 'ML'" class="space-y-2 text-sm">
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-gray-400">算法:</span>
                    <select
                      :value="parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 }).algorithm"
                      class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-gray-300"
                      @change="updateConfigField(step, { algorithm: ($event.target as HTMLSelectElement).value })"
                    >
                      <option v-for="alg in mlAlgorithms" :key="alg.value" :value="alg.value">{{ alg.label }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-xs text-gray-400 mb-1">特征字段</label>
                    <div class="flex flex-wrap gap-1">
                      <label
                        v-for="f in allFields"
                        :key="f"
                        class="flex items-center gap-1 text-xs text-gray-300 px-1.5 py-0.5 rounded bg-dark-surface/40"
                      >
                        <input
                          type="checkbox"
                          :checked="(parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 }).fields || []).includes(f)"
                          class="accent-primary"
                          @change="() => {
                            const cfg = parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 })
                            const set = new Set(cfg.fields || [])
                            if (($event.target as HTMLInputElement).checked) set.add(f)
                            else set.delete(f)
                            cfg.fields = Array.from(set)
                            updateConfigFull(step, cfg)
                          }"
                        />
                        {{ f }}
                      </label>
                    </div>
                  </div>
                  <div v-if="parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 }).algorithm === 'KMEANS'" class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="block text-xs text-gray-400 mb-1">K 值</label>
                      <input
                        type="number"
                        :value="parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 }).k"
                        class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-white w-full"
                        @blur="updateConfigField(step, { k: Number(($event.target as HTMLInputElement).value) || 3 })"
                      />
                    </div>
                    <div>
                      <label class="block text-xs text-gray-400 mb-1">最大迭代</label>
                      <input
                        type="number"
                        :value="parseConfig<MLConfig>(step, { algorithm: 'KMEANS', fields: [], k: 3, maxIterations: 100 }).maxIterations"
                        class="bg-dark-surface/50 border border-primary/15 rounded px-2 py-1 text-xs text-white w-full"
                        @blur="updateConfigField(step, { maxIterations: Number(($event.target as HTMLInputElement).value) || 100 })"
                      />
                    </div>
                  </div>
                </div>

                <!-- JSON 高级编辑 -->
                <details class="mt-3">
                  <summary class="text-xs text-gray-500 cursor-pointer hover:text-primary-light">JSON 配置</summary>
                  <textarea
                    :value="getStepDraft(step)"
                    rows="6"
                    class="mt-1 w-full bg-dark-surface/70 border border-primary/15 rounded p-2 text-xs text-gray-300 font-mono focus:border-primary/40 focus:outline-none"
                    @input="setStepDraft(step, ($event.target as HTMLTextAreaElement).value)"
                    @blur="saveStep(step)"
                  />
                </details>
              </div>
            </div>
          </div>
        </div>

        <!-- 右栏：结果预览 -->
        <div class="glass-card p-3 flex flex-col min-h-0">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xs font-medium text-gray-400 uppercase flex items-center gap-1">
              <Table2 :size="12" />
              结果预览
            </h3>
            <div v-if="result" class="flex items-center gap-1">
              <button
                class="text-xs px-2 py-0.5 rounded transition-colors"
                :class="resultViewMode === 'TABLE' ? 'bg-primary/20 text-primary-light' : 'text-gray-500 hover:bg-primary/5'"
                @click="resultViewMode = 'TABLE'"
              >
                表格
              </button>
              <button
                class="text-xs px-2 py-0.5 rounded transition-colors"
                :class="resultViewMode === 'JSON' ? 'bg-primary/20 text-primary-light' : 'text-gray-500 hover:bg-primary/5'"
                @click="resultViewMode = 'JSON'"
              >
                JSON
              </button>
              <button
                class="p-1 rounded hover:bg-primary/10 text-gray-400 hover:text-primary-light"
                title="刷新"
                @click="currentTask && loadResult(currentTask.id)"
              >
                <RefreshCw :size="12" />
              </button>
            </div>
          </div>

          <div class="flex-1 overflow-auto">
            <div v-if="loadingResult" class="text-center py-8">
              <div class="w-6 h-6 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto" />
            </div>
            <div v-else-if="!result" class="text-center text-gray-600 py-8 text-sm">
              <FileJson :size="40" class="mx-auto mb-2 text-gray-700" />
              <p>执行任务后查看结果</p>
            </div>
            <template v-else>
              <!-- 表格视图 -->
              <div v-if="resultViewMode === 'TABLE' && result.columns.length > 0">
                <table class="w-full text-xs">
                  <thead class="sticky top-0 bg-dark-card/90 backdrop-blur">
                    <tr class="border-b border-primary/10">
                      <th class="text-left px-2 py-1.5 text-gray-500">#</th>
                      <th
                        v-for="col in result.columns"
                        :key="col"
                        class="text-left px-2 py-1.5 text-gray-400 whitespace-nowrap"
                      >
                        {{ col }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(row, idx) in result.rows.slice(0, 200)"
                      :key="idx"
                      class="border-b border-primary/5 hover:bg-primary/5"
                    >
                      <td class="px-2 py-1 text-gray-600">{{ idx + 1 }}</td>
                      <td
                        v-for="col in result.columns"
                        :key="col"
                        class="px-2 py-1 text-gray-300 whitespace-nowrap max-w-[120px] truncate"
                      >
                        {{ previewJsonValue(row[col]) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="result.rows.length > 200" class="text-center text-xs text-gray-500 py-2">
                  仅显示前 200 行，共 {{ result.rows.length }} 行
                </div>
              </div>

              <!-- JSON 视图 -->
              <pre v-else class="text-xs text-gray-300 font-mono whitespace-pre-wrap break-all p-2">{{ JSON.stringify(result.summary ?? result, null, 2) }}</pre>
            </template>
          </div>

          <!-- 步骤执行日志 -->
          <div v-if="result && result.stepResults && result.stepResults.length > 0" class="border-t border-primary/10 mt-2 pt-2">
            <div class="text-xs font-medium text-gray-400 uppercase mb-1.5">执行日志</div>
            <div class="space-y-0.5 max-h-32 overflow-y-auto">
              <div
                v-for="sr in result.stepResults"
                :key="sr.stepId"
                class="flex items-center gap-2 text-xs"
              >
                <CheckCircle2 :size="12" class="text-green-400 shrink-0" />
                <span class="text-gray-300 truncate">{{ sr.stepName }}</span>
                <span class="text-gray-600 ml-auto truncate">{{ previewJsonValue(sr.result) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据源关联弹窗 -->
    <Teleport to="body">
      <div v-if="showDsDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showDsDialog = false">
        <div class="glass-card w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Database :size="18" class="text-primary-light" />
              关联数据源
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showDsDialog = false"><X :size="18" /></button>
          </div>

          <div v-if="loadingDsCandidates" class="text-center py-6">
            <div class="w-6 h-6 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto" />
          </div>
          <div v-else>
            <div class="mb-4">
              <label class="block text-sm text-gray-400 mb-2">选择数据源</label>
              <div class="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto">
                <button
                  v-for="ds in dsCandidates"
                  :key="ds.id"
                  class="text-left p-2 rounded-lg border border-primary/10 hover:border-primary/30 hover:bg-primary/5 transition-all"
                  @click="onPickCandidate(ds)"
                >
                  <div class="text-sm text-white truncate">{{ ds.name }}</div>
                  <div class="text-xs text-gray-500">{{ ds.type }} · {{ ds.host }}</div>
                </button>
              </div>
            </div>

            <div class="space-y-3">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">别名</label>
                <input
                  v-model="newDsAlias"
                  type="text"
                  placeholder="给数据源起个别名"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none"
                />
              </div>

              <div class="flex gap-2">
                <button
                  class="text-xs px-3 py-1.5 rounded-lg transition-colors"
                  :class="newDsMode === 'TABLE' ? 'bg-primary/20 text-primary-light' : 'bg-dark-surface/50 text-gray-400'"
                  @click="newDsMode = 'TABLE'"
                >
                  选择表
                </button>
                <button
                  class="text-xs px-3 py-1.5 rounded-lg transition-colors"
                  :class="newDsMode === 'SQL' ? 'bg-primary/20 text-primary-light' : 'bg-dark-surface/50 text-gray-400'"
                  @click="newDsMode = 'SQL'"
                >
                  自定义 SQL
                </button>
              </div>

              <div v-if="newDsMode === 'TABLE'">
                <label class="block text-sm text-gray-400 mb-1.5">选择表</label>
                <select
                  v-model="newDsTable"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
                >
                  <option value="">请选择</option>
                  <option v-for="t in dsTables" :key="t" :value="t">{{ t }}</option>
                </select>
                <p v-if="dsTables.length === 0" class="text-xs text-gray-600 mt-1">请先点击上方数据源加载表列表</p>
              </div>

              <div v-else>
                <label class="block text-sm text-gray-400 mb-1.5">SQL 查询</label>
                <textarea
                  v-model="newDsSql"
                  rows="4"
                  placeholder="SELECT * FROM ..."
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white font-mano focus:border-primary/40 focus:outline-none"
                />
              </div>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showDsDialog = false">取消</button>
            <button class="flex-1 btn-primary text-sm" @click="confirmAddDs">关联</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 步骤类型选择弹窗 -->
    <Teleport to="body">
      <div v-if="showStepTypePicker" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showStepTypePicker = false">
        <div class="glass-card w-full max-w-md mx-4 p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">选择步骤类型</h3>
            <button class="text-gray-500 hover:text-white" @click="showStepTypePicker = false"><X :size="18" /></button>
          </div>
          <div class="space-y-2">
            <button
              v-for="(meta, type) in stepTypeMeta"
              :key="type"
              class="w-full flex items-center gap-3 p-3 rounded-lg border border-primary/10 hover:border-primary/30 hover:bg-primary/5 transition-all text-left"
              @click="addStep(type as AnalysisStepType)"
            >
              <component :is="meta.icon" :size="20" :class="meta.color" />
              <div>
                <div class="text-sm text-white">{{ meta.label }}</div>
              </div>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 保存为指标弹窗 -->
    <Teleport to="body">
      <div v-if="showSaveDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showSaveDialog = false">
        <div class="glass-card w-full max-w-md mx-4 p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white flex items-center gap-2">
              <Save :size="18" class="text-primary-light" />
              保存为指标
            </h3>
            <button class="text-gray-500 hover:text-white" @click="showSaveDialog = false"><X :size="18" /></button>
          </div>
          <div class="space-y-3">
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">名称</label>
              <input
                v-model="saveForm.name"
                type="text"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white focus:border-primary/40 focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">描述</label>
              <textarea
                v-model="saveForm.description"
                rows="2"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white focus:border-primary/40 focus:outline-none"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">类型</label>
                <select
                  v-model="saveForm.type"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
                >
                  <option value="ATOMIC">原子指标</option>
                  <option value="DERIVED">派生指标</option>
                  <option value="COMPOSITE">复合指标</option>
                </select>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">值类型</label>
                <select
                  v-model="saveForm.valueType"
                  class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-gray-300 focus:border-primary/40 focus:outline-none"
                >
                  <option value="NUMBER">数值</option>
                  <option value="STRING">字符串</option>
                  <option value="JSON">JSON</option>
                  <option value="TABLE">表格</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1.5">标签（逗号分隔）</label>
              <input
                v-model="saveForm.tags"
                type="text"
                placeholder="如: 销售,月度"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2 text-sm text-white focus:border-primary/40 focus:outline-none"
              />
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showSaveDialog = false">取消</button>
            <button class="flex-1 btn-primary text-sm" @click="confirmSaveIndicator">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
