<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import * as THREE from 'three'
import { useRoute, useRouter } from 'vue-router'
import { EditorEngine } from '@/three/EditorEngine'
import { useEditorStore } from '@/stores/editor'
import { SceneExporter } from '@/three/SceneExporter'
import { modelLoader } from '@/three/ModelLoader'
import { saveScene, updateScene, getScene } from '@/api/scene'
import { getModels, getModelUrl } from '@/api/model'
import { getCategoryTree, type CategoryTreeNode } from '@/api/category'
import {
  Move, RotateCw, Maximize2, Trash2, Save, Download, Undo2, Redo2,
  Sun, Lightbulb, Camera, Package, Plus, Box, ExternalLink, Pencil, Layers,
  Search, ChevronDown, ChevronRight, Eye, EyeOff, GripVertical, Check, X
} from 'lucide-vue-next'
import type { Model } from '@/types/model'

const props = defineProps<{
  sceneId?: number
}>()

const route = useRoute()
const router = useRouter()
const editorStore = useEditorStore()

const sceneId = ref<number | null>(props.sceneId || null)
const sceneName = ref('未命名场景')
const showRename = ref(false)
const renameInput = ref('')
const saving = ref(false)
const viewportRef = ref<HTMLDivElement>()
let engine: EditorEngine | null = null
const fps = ref(0)
const objectCount = ref(0)

// 模型库面板状态
const modelLibCategories = ref<CategoryTreeNode[]>([])
const modelLibActiveCategoryId = ref<number | undefined>(undefined)
const modelLibModels = ref<Model[]>([])
const modelLibLoading = ref(false)
const modelLibSearchKeyword = ref('')
const lightPanelExpanded = ref(true) // 灯光折叠状态
const modelLibExpanded = ref(true) // 模型库折叠状态
let modelLibSearchTimer: ReturnType<typeof setTimeout> | null = null

// 场景对象拖拽排序
const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

// 场景对象重命名
const renamingId = ref<string | null>(null)
const renamingInput = ref('')

const modelLibTopCategories = computed(() =>
  modelLibCategories.value.filter((c) => c.parentId === null || c.parentId === undefined)
)

const modelLibTotalCount = ref(0)

const modelLibFilteredModels = computed(() => modelLibModels.value)

async function loadModelLibCategories() {
  try {
    const res = await getCategoryTree()
    modelLibCategories.value = res.data.data ?? []
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

async function loadModelLibModels() {
  modelLibLoading.value = true
  try {
    const params: ModelQuery = {
      keyword: modelLibSearchKeyword.value || undefined,
      categoryId: modelLibActiveCategoryId.value,
      page: 0,
      size: 50,
    }
    const res = await getModels(params)
    modelLibModels.value = res.data.data?.content ?? []
    // 不带分类过滤时更新总模型数
    if (!modelLibActiveCategoryId.value && !modelLibSearchKeyword.value) {
      modelLibTotalCount.value = res.data.data?.totalElements ?? 0
    }
  } catch (e) {
    console.error('加载模型失败', e)
  } finally {
    modelLibLoading.value = false
  }
}

function selectModelLibCategory(categoryId: number | undefined) {
  modelLibActiveCategoryId.value = categoryId
  loadModelLibModels()
}

function onModelLibSearchInput() {
  if (modelLibSearchTimer) clearTimeout(modelLibSearchTimer)
  modelLibSearchTimer = setTimeout(() => {
    loadModelLibModels()
  }, 300)
}

async function addModelFromLib(model: Model) {
  if (!engine) return
  const id = `model_${Date.now()}`
  try {
    const dlRes = await getModelUrl(model.id)
    const url = dlRes.data.data?.downloadUrl
    if (!url) {
      alert('获取模型下载地址失败')
      return
    }
    const threeObj = await engine.addModel(id, url, true)
    editorStore.addObject({
      id,
      name: model.name,
      type: 'model',
      modelRef: model.id,
      transform: {
        position: threeObj.position.toArray() as [number, number, number],
        rotation: [
          THREE.MathUtils.radToDeg(threeObj.rotation.x),
          THREE.MathUtils.radToDeg(threeObj.rotation.y),
          THREE.MathUtils.radToDeg(threeObj.rotation.z),
        ],
        scale: threeObj.scale.toArray() as [number, number, number],
      },
      material: { color: '#ffffff', metalness: 0, roughness: 0.5 },
    })
    objectCount.value = editorStore.objects.length
    // 自动选中新添加的模型，方便直接操作
    engine.selectObjectById(id)
    editorStore.selectObject(id)
  } catch (e) {
    console.error('添加模型失败:', e)
    alert('模型加载失败，请检查模型文件是否可用')
  }
}

onMounted(async () => {
  if (!viewportRef.value) return

  // 每次进入编辑器，先清空之前的状态，避免残留旧场景数据
  editorStore.clearScene()
  sceneName.value = '未命名场景'
  sceneId.value = props.sceneId || (route.params.sceneId ? Number(route.params.sceneId) : null)
  objectCount.value = 0

  engine = new EditorEngine(viewportRef.value)

  engine.setCallbacks(
    (id) => editorStore.selectObject(id),
    (id, transform) => editorStore.updateTransform(id, transform)
  )

  // 如果有 sceneId，加载已有场景
  if (sceneId.value) {
    await loadScene(sceneId.value)
  }

  startFPSCounter()
  objectCount.value = editorStore.objects.length

  // 加载模型库数据
  loadModelLibCategories()
  loadModelLibModels()
})

async function loadScene(id: number) {
  try {
    const res = await getScene(id)
    const scene = res.data.data
    sceneName.value = scene.name

    let sceneData = null
    if (scene.sceneData) {
      try {
        sceneData = JSON.parse(scene.sceneData)
      } catch {
        console.warn('场景数据解析失败')
      }
    }

    if (!sceneData?.objects) return

    for (const obj of sceneData.objects) {
      if (obj.type === 'model' && obj.modelRef) {
        try {
          const dlRes = await getModelUrl(obj.modelRef)
          const url = dlRes.data.data?.downloadUrl
          if (url) {
            const threeObj = await engine!.addModel(obj.id, url, false)
            if (obj.transform) {
              threeObj.position.fromArray(obj.transform.position)
              threeObj.rotation.set(
                THREE.MathUtils.degToRad(obj.transform.rotation[0]),
                THREE.MathUtils.degToRad(obj.transform.rotation[1]),
                THREE.MathUtils.degToRad(obj.transform.rotation[2])
              )
              threeObj.scale.fromArray(obj.transform.scale)
            }
          }
        } catch (e) {
          console.warn('加载模型失败', obj.modelRef, e)
        }
      } else if (obj.type === 'light' && obj.light) {
        engine!.addLight(obj.id, obj.light)
      }
      // 恢复可见性状态
      if (obj.visible === false) {
        engine!.setObjectVisible(obj.id, false)
      }
      editorStore.addObject(obj)
    }
    objectCount.value = editorStore.objects.length
  } catch (e) {
    console.error('加载场景失败:', e)
  }
}

onBeforeUnmount(() => {
  engine?.dispose()
  engine = null
  if (fpsInterval) clearInterval(fpsInterval)
})

let fpsFrameCount = 0
let fpsInterval: ReturnType<typeof setInterval> | null = null

function startFPSCounter() {
  fpsInterval = setInterval(() => {
    fps.value = fpsFrameCount
    fpsFrameCount = 0
  }, 1000)
  const countFrame = () => { fpsFrameCount++; requestAnimationFrame(countFrame) }
  countFrame()
}

function setTransformMode(mode: 'translate' | 'rotate' | 'scale') {
  engine?.setTransformMode(mode)
  editorStore.setTransformMode(mode)
}

function addLightToScene(type: 'ambient' | 'directional' | 'point' | 'spot') {
  if (!engine) return
  const id = `light_${Date.now()}`
  engine.addLight(id, {
    type,
    color: '#ffffff',
    intensity: type === 'ambient' ? 0.5 : 1.0,
  })
  const names: Record<string, string> = { ambient: '环境光', directional: '平行光', point: '点光源', spot: '聚光灯' }
  editorStore.addObject({
    id,
    name: `${names[type]}_${Date.now().toString().slice(-3)}`,
    type: 'light',
    transform: { position: [0, 5, 0], rotation: [0, 0, 0], scale: [1, 1, 1] },
    light: { type, color: '#ffffff', intensity: type === 'ambient' ? 0.5 : 1.0 },
  })
  objectCount.value = editorStore.objects.length
}

function deleteSelected() {
  if (!engine || !editorStore.selectedObjectId) return
  engine.removeObject(editorStore.selectedObjectId)
  editorStore.removeObject(editorStore.selectedObjectId)
  objectCount.value = editorStore.objects.length
}

// 场景对象显示/隐藏
function toggleObjectVisible(id: string, currentVisible: boolean | undefined) {
  const visible = currentVisible === undefined ? true : currentVisible
  const newVisible = !visible
  editorStore.updateVisible(id, newVisible)
  engine?.setObjectVisible(id, newVisible)
}

// 场景对象拖拽排序
function onDragStart(index: number) {
  dragIndex.value = index
}

function onDragOver(event: DragEvent, index: number) {
  event.preventDefault()
  dragOverIndex.value = index
}

function onDragLeave() {
  dragOverIndex.value = null
}

function onDrop(index: number) {
  if (dragIndex.value !== null && dragIndex.value !== index) {
    editorStore.moveObject(dragIndex.value, index)
  }
  dragIndex.value = null
  dragOverIndex.value = null
}

function onDragEnd() {
  dragIndex.value = null
  dragOverIndex.value = null
}

// 场景对象重命名
function startRename(id: string, name: string) {
  renamingId.value = id
  renamingInput.value = name
}

function confirmRename() {
  if (renamingId.value && renamingInput.value.trim()) {
    editorStore.renameObject(renamingId.value, renamingInput.value.trim())
  }
  renamingId.value = null
  renamingInput.value = ''
}

function cancelRename() {
  renamingId.value = null
  renamingInput.value = ''
}

function handleUndo() { editorStore.undo() }
function handleRedo() { editorStore.redo() }

async function handleExport() {
  if (!engine) return
  try {
    const blob = await SceneExporter.exportToGLB(engine.getScene())
    SceneExporter.downloadBlob(blob, `${sceneName.value}.glb`)
  } catch (e) {
    console.error('导出失败:', e)
  }
}

async function handleSave() {
  saving.value = true
  try {
    const sceneData = {
      objects: editorStore.objects.map((obj) => ({
        ...obj,
        // 不发送 Three.js 特有属性
      })),
      environment: editorStore.environment,
    }

    // 截图作为缩略图
    const thumbnailBase64 = engine ? engine.takeScreenshot().split(',')[1] : undefined

    if (sceneId.value) {
      await updateScene(sceneId.value, {
        name: sceneName.value,
        sceneData,
        thumbnailBase64,
      })
    } else {
      const res = await saveScene({
        name: sceneName.value,
        sceneData,
        thumbnailBase64,
      })
      sceneId.value = res.data.data.id
    }
    alert('保存成功')
  } catch (e: unknown) {
    console.error('保存失败:', e)
    const err = e as Error & { response?: { data?: { message?: string } } }
    alert('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

function openRename() {
  renameInput.value = sceneName.value
  showRename.value = true
}

function handleRename() {
  if (renameInput.value.trim()) {
    sceneName.value = renameInput.value.trim()
  }
  showRename.value = false
}

function handlePreview() {
  if (sceneId.value) {
    window.open(`/preview/${sceneId.value}`, '_blank')
  } else {
    alert('请先保存场景后再预览')
  }
}

function updateSelectedMaterial(prop: string, value: unknown) {
  if (!editorStore.selectedObjectId || !engine) return
  engine.updateObjectMaterial(editorStore.selectedObjectId, { [prop]: value })
  editorStore.updateMaterial(editorStore.selectedObjectId, { [prop]: value })
}
</script>

<template>
  <div class="h-[calc(100vh-4rem)] flex flex-col">
    <!-- 顶部工具栏 -->
    <div class="glass-card rounded-none border-b border-primary/10 p-2 flex items-center gap-2 shrink-0">
      <!-- 场景名称 -->
      <div class="flex items-center gap-2 min-w-0 mr-2">
        <Layers :size="14" class="text-primary-light shrink-0" />
        <span class="text-sm text-white truncate max-w-[200px]">{{ sceneName }}</span>
        <button class="p-1 rounded hover:bg-primary/10 text-gray-400 hover:text-white transition-colors shrink-0" title="重命名" @click="openRename">
          <Pencil :size="12" />
        </button>
      </div>

      <div class="w-px h-6 bg-primary/15 mx-1" />

      <button class="btn-ghost text-xs px-2 py-1.5" :disabled="editorStore.historyIndex <= 0" @click="handleUndo">
        <Undo2 :size="14" />
      </button>
      <button class="btn-ghost text-xs px-2 py-1.5" :disabled="editorStore.historyIndex >= (editorStore.history?.length ?? 0) - 1" @click="handleRedo">
        <Redo2 :size="14" />
      </button>

      <div class="w-px h-6 bg-primary/15 mx-1" />

      <button
        class="px-2 py-1.5 rounded-md text-xs transition-all"
        :class="editorStore.transformMode === 'translate' ? 'bg-primary/20 text-primary-light' : 'text-gray-400 hover:text-gray-300'"
        @click="setTransformMode('translate')"
      >
        <Move :size="14" />
      </button>
      <button
        class="px-2 py-1.5 rounded-md text-xs transition-all"
        :class="editorStore.transformMode === 'rotate' ? 'bg-primary/20 text-primary-light' : 'text-gray-400 hover:text-gray-300'"
        @click="setTransformMode('rotate')"
      >
        <RotateCw :size="14" />
      </button>
      <button
        class="px-2 py-1.5 rounded-md text-xs transition-all"
        :class="editorStore.transformMode === 'scale' ? 'bg-primary/20 text-primary-light' : 'text-gray-400 hover:text-gray-300'"
        @click="setTransformMode('scale')"
      >
        <Maximize2 :size="14" />
      </button>

      <div class="w-px h-6 bg-primary/15 mx-1" />

      <button
        class="btn-ghost text-xs px-2 py-1.5 text-danger hover:bg-danger/10"
        :disabled="!editorStore.selectedObjectId"
        @click="deleteSelected"
      >
        <Trash2 :size="14" />
      </button>

      <!-- 弹性空间，把保存/导出推到最右边 -->
      <div class="flex-1" />

      <button
        v-if="sceneId"
        class="btn-ghost text-xs px-3 py-1.5 flex items-center gap-1.5"
        @click="handlePreview"
      >
        <ExternalLink :size="14" />
        预览
      </button>

      <button class="btn-ghost text-xs px-3 py-1.5 flex items-center gap-1.5" @click="handleExport">
        <Download :size="14" />
        导出 GLB
      </button>

      <button
        class="btn-primary text-xs px-3 py-1.5 flex items-center gap-1.5"
        :disabled="saving"
        @click="handleSave"
      >
        <span v-if="saving" class="w-3 h-3 border border-white/50 border-t-white rounded-full animate-spin" />
        <Save v-else :size="14" />
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div class="flex-1 flex overflow-hidden">
      <!-- 左侧面板 -->
      <div class="w-64 glass-card rounded-none border-r border-primary/10 overflow-y-auto shrink-0">
        <!-- 组件库 -->
        <div class="border-b border-primary/10">
          <!-- 灯光快捷按钮 -->
          <div class="border-b border-primary/10">
            <button
              class="w-full px-3 py-2.5 flex items-center justify-between hover:bg-primary/5 transition-colors"
              @click="lightPanelExpanded = !lightPanelExpanded"
            >
              <h3 class="text-xs font-medium text-primary-light flex items-center gap-1.5">
                <Sun :size="12" />
                灯光
              </h3>
              <ChevronDown v-if="lightPanelExpanded" :size="12" class="text-gray-500" />
              <ChevronRight v-else :size="12" class="text-gray-500" />
            </button>
            <div v-show="lightPanelExpanded" class="px-3 pb-3">
              <div class="flex flex-wrap gap-1.5">
                <button
                  class="px-2.5 py-1.5 rounded-lg text-xs text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all flex items-center gap-1.5"
                  @click="addLightToScene('directional')"
                >
                  <Sun :size="10" />
                  平行光
                </button>
                <button
                  class="px-2.5 py-1.5 rounded-lg text-xs text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all flex items-center gap-1.5"
                  @click="addLightToScene('point')"
                >
                  <Lightbulb :size="10" />
                  点光源
                </button>
                <button
                  class="px-2.5 py-1.5 rounded-lg text-xs text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all flex items-center gap-1.5"
                  @click="addLightToScene('ambient')"
                >
                  <Camera :size="10" />
                  环境光
                </button>
              </div>
            </div>
          </div>

          <!-- 模型库 -->
          <div>
            <button
              class="w-full px-3 py-2.5 flex items-center justify-between hover:bg-primary/5 transition-colors"
              @click="modelLibExpanded = !modelLibExpanded"
            >
              <h3 class="text-xs font-medium text-primary-light flex items-center gap-1.5">
                <Package :size="12" />
                模型库
                <span class="text-gray-500 font-normal">({{ modelLibTotalCount }})</span>
              </h3>
              <ChevronDown v-if="modelLibExpanded" :size="12" class="text-gray-500" />
              <ChevronRight v-else :size="12" class="text-gray-500" />
            </button>

            <div v-show="modelLibExpanded" class="pb-2">
              <!-- 分类标签栏 -->
              <div class="px-3 pb-2">
                <div class="flex items-center gap-1 overflow-x-auto scrollbar-thin pb-1">
                  <button
                    class="shrink-0 px-2 py-1 rounded-full text-[10px] font-medium transition-all"
                    :class="modelLibActiveCategoryId === undefined
                      ? 'bg-primary/20 text-primary-light border border-primary/30'
                      : 'text-gray-500 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
                    @click="selectModelLibCategory(undefined)"
                  >
                    全部
                  </button>
                  <button
                    v-for="cat in modelLibTopCategories"
                    :key="cat.id"
                    class="shrink-0 px-2 py-1 rounded-full text-[10px] font-medium transition-all"
                    :class="modelLibActiveCategoryId === cat.id
                      ? 'bg-primary/20 text-primary-light border border-primary/30'
                      : 'text-gray-500 hover:text-gray-300 hover:bg-primary/5 border border-transparent'"
                    @click="selectModelLibCategory(cat.id)"
                  >
                    {{ cat.name }}
                  </button>
                </div>
              </div>

              <!-- 搜索框 -->
              <div class="px-3 pb-2">
                <div class="relative">
                  <Search :size="12" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500" />
                  <input
                    v-model="modelLibSearchKeyword"
                    type="text"
                    placeholder="搜索模型..."
                    class="w-full bg-dark-surface/50 border border-primary/15 rounded-lg pl-7 pr-3 py-1.5 text-[10px] text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
                    @input="onModelLibSearchInput"
                  />
                </div>
              </div>

              <!-- 模型列表 -->
              <div class="px-3 max-h-[40vh] overflow-y-auto scrollbar-thin">
                <div v-if="modelLibLoading" class="flex justify-center py-4">
                  <div class="w-5 h-5 border border-primary/30 border-t-primary-light rounded-full animate-spin" />
                </div>
                <div v-else class="space-y-1">
                  <button
                    v-for="model in modelLibFilteredModels"
                    :key="model.id"
                    class="w-full text-left flex items-center gap-2 p-1.5 rounded-lg transition-all hover:bg-primary/10 group"
                    @click="addModelFromLib(model)"
                  >
                    <img
                      v-if="model.thumbnailUrl"
                      :src="model.thumbnailUrl"
                      class="w-8 h-8 rounded-md object-cover shrink-0"
                    />
                    <div v-else class="w-8 h-8 rounded-md bg-dark-surface/50 flex items-center justify-center shrink-0">
                      <Box :size="14" class="text-gray-600" />
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="text-[10px] text-gray-300 group-hover:text-white truncate transition-colors">{{ model.name }}</p>
                      <p class="text-[9px] text-gray-600">.{{ model.format }}</p>
                    </div>
                    <Plus :size="12" class="text-gray-600 group-hover:text-primary-light shrink-0 transition-colors" />
                  </button>

                  <p v-if="modelLibFilteredModels.length === 0" class="text-[10px] text-gray-600 text-center py-4">
                    暂无模型
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 场景对象树 -->
        <div class="p-3">
          <h3 class="text-xs font-medium text-primary-light mb-2">场景对象</h3>
          <div class="space-y-0.5">
            <div
              v-for="(obj, index) in editorStore.objects"
              :key="obj.id"
              class="group flex items-center gap-1 px-2 py-1.5 rounded-md text-xs transition-all"
              :class="[
                editorStore.selectedObjectId === obj.id
                  ? 'bg-primary/15 text-primary-light'
                  : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300',
                dragOverIndex === index && dragIndex !== index ? 'border-t-2 border-primary/50' : ''
              ]"
              draggable="true"
              @dragstart="onDragStart(index)"
              @dragover="onDragOver($event, index)"
              @dragleave="onDragLeave"
              @drop="onDrop(index)"
              @dragend="onDragEnd"
            >
              <!-- 拖拽手柄 -->
              <GripVertical :size="10" class="text-gray-600 cursor-grab shrink-0 opacity-0 group-hover:opacity-100 transition-opacity" />

              <!-- 类型图标 -->
              <Box v-if="obj.type === 'model'" :size="10" class="shrink-0" />
              <Lightbulb v-else-if="obj.type === 'light'" :size="10" class="shrink-0" />

              <!-- 名称（可重命名） -->
              <template v-if="renamingId === obj.id">
                <div class="flex-1 flex items-center gap-1 min-w-0">
                  <input
                    v-model="renamingInput"
                    type="text"
                    class="flex-1 bg-dark-surface/50 border border-primary/30 rounded px-1.5 py-0.5 text-[10px] text-white focus:outline-none min-w-0"
                    @keyup.enter="confirmRename"
                    @keyup.escape="cancelRename"
                    @blur="confirmRename"
                  />
                  <button class="p-0.5 hover:text-primary-light" @mousedown.prevent="confirmRename">
                    <Check :size="10" />
                  </button>
                  <button class="p-0.5 hover:text-danger" @mousedown.prevent="cancelRename">
                    <X :size="10" />
                  </button>
                </div>
              </template>
              <template v-else>
                <span
                  class="flex-1 truncate cursor-pointer"
                  @click="engine?.selectObjectById(obj.id)"
                  @dblclick="startRename(obj.id, obj.name)"
                >
                  {{ obj.name }}
                </span>
              </template>

              <!-- 显示/隐藏按钮 -->
              <button
                class="p-0.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                :class="(obj.visible === undefined || obj.visible) ? 'text-gray-500 hover:text-gray-300' : 'text-gray-600 hover:text-gray-400'"
                @click.stop="toggleObjectVisible(obj.id, obj.visible)"
              >
                <Eye v-if="obj.visible === undefined || obj.visible" :size="10" />
                <EyeOff v-else :size="10" />
              </button>

              <!-- 重命名按钮 -->
              <button
                class="p-0.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity text-gray-500 hover:text-primary-light"
                @click.stop="startRename(obj.id, obj.name)"
              >
                <Pencil :size="9" />
              </button>
            </div>
            <p v-if="editorStore.objects.length === 0" class="text-xs text-gray-600 py-2 text-center">
              场景为空
            </p>
          </div>
        </div>
      </div>

      <!-- 中央视口 -->
      <div ref="viewportRef" class="flex-1 bg-dark" />

      <!-- 右侧属性面板 -->
      <div class="w-72 glass-card rounded-none border-l border-primary/10 overflow-y-auto p-4 shrink-0">
        <template v-if="editorStore.selectedObject">
          <!-- 名称（可重命名） -->
          <div class="flex items-center gap-2 mb-4">
            <h3 class="text-sm font-medium text-white flex-1 truncate">{{ editorStore.selectedObject.name }}</h3>
            <button
              class="p-1 rounded hover:bg-primary/10 text-gray-400 hover:text-primary-light transition-colors shrink-0"
              title="重命名"
              @click="startRename(editorStore.selectedObject!.id, editorStore.selectedObject!.name)"
            >
              <Pencil :size="12" />
            </button>
          </div>

          <!-- 变换属性（只读显示） -->
          <div class="mb-4">
            <h4 class="text-xs font-medium text-primary-light mb-2">变换</h4>
            <div class="space-y-2">
              <div v-for="(label, key) in ({ position: '位置', rotation: '旋转', scale: '缩放' } as const)" :key="key">
                <p class="text-xs text-gray-500 mb-1">{{ label }}</p>
                <div class="grid grid-cols-3 gap-1.5">
                  <div v-for="(axis, i) in ['X', 'Y', 'Z']" :key="axis">
                    <div class="flex items-center gap-1 bg-dark-surface/50 rounded-md px-2 py-1">
                      <span class="text-[10px]" :class="i === 0 ? 'text-red-400' : i === 1 ? 'text-green-400' : 'text-blue-400'">{{ axis }}</span>
                      <span class="text-xs text-gray-300">
                        {{ editorStore.selectedObject?.transform?.[key]?.[i]?.toFixed(1) || 0 }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 材质属性 -->
          <div v-if="editorStore.selectedObject.material" class="mb-4">
            <h4 class="text-xs font-medium text-primary-light mb-2">材质</h4>
            <div class="space-y-2">
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500 w-12">颜色</span>
                <input
                  type="color"
                  :value="editorStore.selectedObject.material?.color || '#ffffff'"
                  class="w-8 h-6 rounded cursor-pointer bg-transparent border-none"
                  @input="updateSelectedMaterial('color', ($event.target as HTMLInputElement).value)"
                />
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500 w-12">金属度</span>
                <input
                  type="range" min="0" max="1" step="0.01"
                  :value="editorStore.selectedObject.material?.metalness || 0"
                  class="flex-1 accent-primary"
                  @input="updateSelectedMaterial('metalness', parseFloat(($event.target as HTMLInputElement).value))"
                />
                <span class="text-xs text-gray-400 w-8 text-right">{{ (editorStore.selectedObject.material?.metalness || 0).toFixed(2) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500 w-12">粗糙度</span>
                <input
                  type="range" min="0" max="1" step="0.01"
                  :value="editorStore.selectedObject.material?.roughness || 0.5"
                  class="flex-1 accent-primary"
                  @input="updateSelectedMaterial('roughness', parseFloat(($event.target as HTMLInputElement).value))"
                />
                <span class="text-xs text-gray-400 w-8 text-right">{{ (editorStore.selectedObject.material?.roughness || 0.5).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="flex flex-col items-center justify-center h-full text-gray-600">
          <Package :size="32" class="mb-2" />
          <p class="text-xs">选择对象查看属性</p>
        </div>
      </div>
    </div>

    <!-- 底部状态栏 -->
    <div class="glass-card rounded-none border-t border-primary/10 px-4 py-1.5 flex items-center gap-6 text-xs text-gray-500 shrink-0">
      <span>FPS: <span class="text-primary-light">{{ fps }}</span></span>
      <span>对象: <span class="text-white">{{ objectCount }}</span></span>
    </div>

    <!-- 重命名弹窗 -->
    <Teleport to="body">
      <div v-if="showRename" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showRename = false">
        <div class="glass-card w-full max-w-sm mx-4 p-6 animate-slide-up">
          <h3 class="text-lg font-semibold text-white mb-4">重命名场景</h3>
          <input
            v-model="renameInput"
            type="text"
            placeholder="输入场景名称"
            class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
            @keyup.enter="handleRename"
          />
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showRename = false">取消</button>
            <button class="flex-1 btn-primary text-sm" :disabled="!renameInput.trim()" @click="handleRename">确认</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
