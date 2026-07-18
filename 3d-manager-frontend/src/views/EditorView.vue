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
import { getIndicators } from '@/api/indicator'
import type { Indicator } from '@/types/indicator'
import type { DataBinding, AnimationConfig, ChartConfig, InteractionEvent } from '@/types/scene'
import {
  Move, RotateCw, Maximize2, Trash2, Save, Download, Undo2, Redo2, ArrowLeft,
  Sun, Lightbulb, Camera, Package, Plus, Box, ExternalLink, Pencil, Layers,
  Search, ChevronDown, ChevronRight, Eye, EyeOff, GripVertical, Check, X,
  Copy, Settings, Link2, AlertCircle, CheckCircle2, PlayCircle,
  BarChart3, ChartLine, ChartPie, Gauge, MousePointerClick
} from 'lucide-vue-next'
import type { Model, ModelQuery } from '@/types/model'
import ChartRenderer from '@/components/scene/ChartRenderer.vue'

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

// 通知弹窗
interface Notice {
  type: 'success' | 'error' | 'info'
  title: string
  message?: string
}
const notice = ref<Notice | null>(null)
function showSuccess(title: string, message?: string) {
  notice.value = { type: 'success', title, message }
}
function showError(title: string, message?: string) {
  notice.value = { type: 'error', title, message }
}
function showInfo(title: string, message?: string) {
  notice.value = { type: 'info', title, message }
}
function closeNotice() {
  notice.value = null
}

// 返回场景管理
function goBack() {
  if (editorStore.objects.length > 0 && !saving.value) {
    if (!confirm('当前场景有未保存的修改，确定要离开吗？')) return
  }
  router.push('/scenes')
}
const sceneStats = ref({ triangles: 0, vertices: 0 })

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
      showError('加载失败', '获取模型下载地址失败')
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
    updateSceneStats()
    // 自动选中新添加的模型，方便直接操作
    engine.selectObjectById(id)
    editorStore.selectObject(id)
  } catch (e) {
    console.error('添加模型失败:', e)
    showError('模型加载失败', '请检查模型文件是否可用')
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

  // 设置动画配置查询回调：编辑器中实时播放选中对象的动画
  engine.setAnimationCallback((id) => {
    const obj = editorStore.objects.find((o) => o.id === id)
    return obj?.animation
  })

  // 初始化背景色
  engine.setBackgroundColor(editorStore.environment.backgroundColor)

  // 如果有 sceneId，加载已有场景
  if (sceneId.value) {
    await loadScene(sceneId.value)
  }

  startFPSCounter()
  objectCount.value = editorStore.objects.length
  updateSceneStats()

  // 加载模型库数据
  loadModelLibCategories()
  loadModelLibModels()

  // 键盘快捷键
  window.addEventListener('keydown', handleKeyDown)
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
      } else if (obj.type === 'primitive' && obj.primitiveType) {
        const primObj = engine!.addPrimitive(obj.id, obj.primitiveType)
        if (obj.transform) {
          primObj.position.fromArray(obj.transform.position)
          primObj.rotation.set(
            THREE.MathUtils.degToRad(obj.transform.rotation[0]),
            THREE.MathUtils.degToRad(obj.transform.rotation[1]),
            THREE.MathUtils.degToRad(obj.transform.rotation[2])
          )
          primObj.scale.fromArray(obj.transform.scale)
        }
        if (obj.material) {
          engine!.updateObjectMaterial(obj.id, obj.material)
        }
      } else if (obj.type === 'chart') {
        // chart 类型不需要3D对象，直接添加到store
      }
      // 恢复可见性状态（chart 类型无3D对象，跳过）
      if (obj.visible === false && obj.type !== 'chart') {
        engine!.setObjectVisible(obj.id, false)
      }
      editorStore.addObject(obj)
    }
    objectCount.value = editorStore.objects.length
    updateSceneStats()
  } catch (e) {
    console.error('加载场景失败:', e)
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
  engine?.dispose()
  engine = null
  if (fpsInterval) clearInterval(fpsInterval)
})

/** 键盘快捷键处理 */
function handleKeyDown(e: KeyboardEvent) {
  // 如果焦点在输入框/文本框中，不处理快捷键
  const target = e.target as HTMLElement
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) return

  switch (e.key) {
    case 'Delete':
    case 'Backspace':
      if (editorStore.selectedObjectId) {
        e.preventDefault()
        deleteSelected()
      }
      break
    case 'w':
    case 'W':
      setTransformMode('translate')
      break
    case 'e':
    case 'E':
      setTransformMode('rotate')
      break
    case 'r':
    case 'R':
      setTransformMode('scale')
      break
    case 'z':
    case 'Z':
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        if (e.shiftKey) handleRedo()
        else handleUndo()
      }
      break
    case 'y':
    case 'Y':
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        handleRedo()
      }
      break
    case 'd':
    case 'D':
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        duplicateSelected()
      }
      break
  }
}

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
  updateSceneStats()
}

function deleteSelected() {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  // chart 类型无3D对象，仅从 store 移除
  if (obj?.type !== 'chart') {
    engine?.removeObject(editorStore.selectedObjectId)
  }
  editorStore.removeObject(editorStore.selectedObjectId)
  objectCount.value = editorStore.objects.length
  if (obj?.type !== 'chart') updateSceneStats()
}

/** 通过ID删除对象（从场景树删除） */
function deleteObjectById(id: string) {
  const obj = editorStore.objects.find((o) => o.id === id)
  if (obj?.type !== 'chart') {
    engine?.removeObject(id)
  }
  editorStore.removeObject(id)
  objectCount.value = editorStore.objects.length
  if (obj?.type !== 'chart') updateSceneStats()
}

/** 更新场景统计信息 */
function updateSceneStats() {
  if (engine) {
    sceneStats.value = engine.getSceneStats()
  }
}

/** 添加基础几何体到场景 */
function addPrimitiveToScene(primitiveType: 'cube' | 'sphere' | 'cylinder' | 'plane' | 'cone' | 'torus') {
  if (!engine) return
  const id = `primitive_${Date.now()}`
  const obj = engine.addPrimitive(id, primitiveType)
  const names: Record<string, string> = {
    cube: '立方体', sphere: '球体', cylinder: '圆柱体', plane: '平面', cone: '圆锥体', torus: '圆环'
  }
  editorStore.addObject({
    id,
    name: `${names[primitiveType]}_${Date.now().toString().slice(-3)}`,
    type: 'primitive',
    primitiveType,
    transform: {
      position: obj.position.toArray() as [number, number, number],
      rotation: [0, 0, 0],
      scale: [1, 1, 1],
    },
    material: { color: '#cccccc', metalness: 0, roughness: 0.5 },
  })
  objectCount.value = editorStore.objects.length
  updateSceneStats()
  engine.selectObjectById(id)
  editorStore.selectObject(id)
}

/** 添加图表到场景（在3D视口中不渲染，仅作为2D覆盖层在预览中显示） */
function addChartToScene(type: 'bar' | 'line' | 'pie' | 'gauge') {
  const id = `chart_${Date.now()}`
  const names: Record<string, string> = { bar: '柱状图', line: '折线图', pie: '饼图', gauge: '仪表盘' }
  editorStore.addObject({
    id,
    name: `${names[type]}_${Date.now().toString().slice(-3)}`,
    type: 'chart',
    transform: { position: [0, 2, 0], rotation: [0, 0, 0], scale: [1, 1, 1] },
    chartConfig: {
      type,
      title: names[type],
      dataSource: 'static',
      staticData: [
        { name: '分类A', value: Math.floor(Math.random() * 100) },
        { name: '分类B', value: Math.floor(Math.random() * 100) },
        { name: '分类C', value: Math.floor(Math.random() * 100) },
      ],
      width: 300,
      height: 200,
    },
  })
  objectCount.value = editorStore.objects.length
  editorStore.selectObject(id)
}

/** 删除图表对象（无3D对象，只从 store 中移除） */
function deleteChartObject(id: string) {
  editorStore.removeObject(id)
  objectCount.value = editorStore.objects.length
}

/** 更新当前选中图表对象的 chartConfig */
function updateSelectedChartConfig(updates: Partial<ChartConfig>) {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  if (!obj || obj.type !== 'chart' || !obj.chartConfig) return
  const newConfig: ChartConfig = { ...obj.chartConfig, ...updates }
  editorStore.updateObject(editorStore.selectedObjectId, { chartConfig: newConfig })
}

/** 添加一行静态数据 */
function addStaticDataRow() {
  const obj = editorStore.selectedObject
  if (!obj || obj.type !== 'chart' || !obj.chartConfig) return
  const next = [...(obj.chartConfig.staticData ?? []), { name: `分类${(obj.chartConfig.staticData?.length ?? 0) + 1}`, value: 0 }]
  updateSelectedChartConfig({ staticData: next })
}

/** 删除一行静态数据 */
function removeStaticDataRow(index: number) {
  const obj = editorStore.selectedObject
  if (!obj || obj.type !== 'chart' || !obj.chartConfig) return
  const next = [...(obj.chartConfig.staticData ?? [])]
  next.splice(index, 1)
  updateSelectedChartConfig({ staticData: next })
}

/** 修改静态数据某行 */
function updateStaticDataRow(index: number, field: 'name' | 'value', value: string) {
  const obj = editorStore.selectedObject
  if (!obj || obj.type !== 'chart' || !obj.chartConfig) return
  const next = [...(obj.chartConfig.staticData ?? [])]
  if (!next[index]) return
  next[index] = {
    ...next[index],
    [field]: field === 'value' ? (parseFloat(value) || 0) : value,
  }
  updateSelectedChartConfig({ staticData: next })
}

/** 当图表数据源切换为 indicator 时，同步 indicatorId */
function onChartIndicatorPicked(id: number) {
  updateSelectedChartConfig({ indicatorId: id })
}

/** 复制当前选中对象 */
function duplicateSelected() {
  if (!editorStore.selectedObjectId) return
  const sourceId = editorStore.selectedObjectId
  const source = editorStore.objects.find((o) => o.id === sourceId)
  if (!source) return
  const newId = `obj_${Date.now()}`

  // chart 类型无3D对象，直接在 store 中复制
  if (source.type === 'chart') {
    editorStore.addObject({
      ...JSON.parse(JSON.stringify(source)),
      id: newId,
      name: `${source.name}_副本`,
      transform: {
        position: [source.transform.position[0] + 1, source.transform.position[1], source.transform.position[2] + 1],
        rotation: [...source.transform.rotation] as [number, number, number],
        scale: [...source.transform.scale] as [number, number, number],
      },
    })
    objectCount.value = editorStore.objects.length
    editorStore.selectObject(newId)
    return
  }

  if (!engine) return
  const clonedObj = engine.duplicateObject(sourceId, newId)
  if (!clonedObj) return

  editorStore.addObject({
    ...JSON.parse(JSON.stringify(source)),
    id: newId,
    name: `${source.name}_副本`,
    transform: {
      position: clonedObj.position.toArray() as [number, number, number],
      rotation: [
        THREE.MathUtils.radToDeg(clonedObj.rotation.x),
        THREE.MathUtils.radToDeg(clonedObj.rotation.y),
        THREE.MathUtils.radToDeg(clonedObj.rotation.z),
      ],
      scale: clonedObj.scale.toArray() as [number, number, number],
    },
  })
  objectCount.value = editorStore.objects.length
  updateSceneStats()
  engine.selectObjectById(newId)
  editorStore.selectObject(newId)
}

/** 通过输入框更新变换值 */
function updateTransformValue(axis: 'position' | 'rotation' | 'scale', index: number, value: number) {
  if (!editorStore.selectedObjectId || !engine) return
  const obj = editorStore.selectedObject
  if (!obj) return
  const newTransform = { ...obj.transform }
  const arr = [...newTransform[axis]] as [number, number, number]
  arr[index] = value
  newTransform[axis] = arr
  engine.setTransform(editorStore.selectedObjectId, newTransform.position, newTransform.rotation, newTransform.scale)
  editorStore.updateTransform(editorStore.selectedObjectId, newTransform)
}

/** 更新背景色 */
function updateBackgroundColor(color: string) {
  editorStore.updateEnvironment({ backgroundColor: color })
  engine?.setBackgroundColor(color)
}

/** 更新雾效颜色 */
function updateFogColor(color: string) {
  const density = editorStore.environment.fogDensity || 0
  editorStore.updateEnvironment({ fogColor: color })
  if (density > 0) {
    engine?.setFog(color, density)
  }
}

/** 更新雾效密度 */
function updateFogDensity(density: number) {
  const color = editorStore.environment.fogColor || '#1a1a2e'
  editorStore.updateEnvironment({ fogDensity: density })
  if (density > 0) {
    engine?.setFog(color, density)
  } else {
    engine?.clearFog()
  }
}

// 场景对象显示/隐藏
function toggleObjectVisible(id: string, currentVisible: boolean | undefined) {
  const visible = currentVisible === undefined ? true : currentVisible
  const newVisible = !visible
  editorStore.updateVisible(id, newVisible)
  // chart 类型无3D对象，跳过引擎调用
  const obj = editorStore.objects.find((o) => o.id === id)
  if (obj?.type !== 'chart') {
    engine?.setObjectVisible(id, newVisible)
  }
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
    showSuccess('保存成功', `场景「${sceneName.value}」已保存`)
  } catch (e: unknown) {
    console.error('保存失败:', e)
    const err = e as Error
    showError('保存失败', err.message || '请稍后重试')
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
    showInfo('提示', '请先保存场景后再预览')
  }
}

function updateSelectedMaterial(prop: string, value: unknown) {
  if (!editorStore.selectedObjectId || !engine) return
  engine.updateObjectMaterial(editorStore.selectedObjectId, { [prop]: value })
  editorStore.updateMaterial(editorStore.selectedObjectId, { [prop]: value })
}

// ===== 数据绑定相关 =====
const showBindingPicker = ref(false)
const indicatorList = ref<Indicator[]>([])
const indicatorListLoading = ref(false)
const bindingForm = ref<Partial<DataBinding>>({
  displayMode: 'text',
})

/** 加载指标列表（用于绑定选择器） */
async function loadIndicators() {
  if (indicatorList.value.length > 0) return // 已加载过则不重复
  indicatorListLoading.value = true
  try {
    const res = await getIndicators({ page: 0, size: 200 })
    indicatorList.value = res.data.data?.content ?? []
  } catch (e) {
    console.error('加载指标列表失败', e)
  } finally {
    indicatorListLoading.value = false
  }
}

/** 打开绑定选择器 */
function openBindingPicker() {
  bindingForm.value = { displayMode: 'text' }
  showBindingPicker.value = true
  loadIndicators()
}

/** 选择指标时同步 indicatorName */
function onIndicatorPicked(id: number) {
  const ind = indicatorList.value.find((i) => i.id === id)
  if (ind) {
    bindingForm.value.indicatorName = ind.name
    if (!bindingForm.value.label) {
      bindingForm.value.label = ind.name
    }
  }
}

/** 确认添加绑定 */
function confirmAddBinding() {
  if (!editorStore.selectedObjectId) return
  if (!bindingForm.value.indicatorId) {
    showError('提示', '请选择一个指标')
    return
  }
  const obj = editorStore.selectedObject
  if (!obj) return

  const newBinding: DataBinding = {
    indicatorId: bindingForm.value.indicatorId,
    indicatorName: bindingForm.value.indicatorName,
    label: bindingForm.value.label,
    displayMode: bindingForm.value.displayMode || 'text',
    position: bindingForm.value.position,
    unit: bindingForm.value.unit,
  }

  const bindings = [...(obj.dataBindings || []), newBinding]
  editorStore.updateDataBindings(editorStore.selectedObjectId, bindings)
  showBindingPicker.value = false
}

/** 删除某个绑定 */
function removeBinding(index: number) {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  if (!obj?.dataBindings) return
  const next = [...obj.dataBindings]
  next.splice(index, 1)
  editorStore.updateDataBindings(editorStore.selectedObjectId, next)
}

/** 显示模式中文标签 */
function displayModeLabel(mode: DataBinding['displayMode']): string {
  return mode === 'text' ? '文本' : mode === 'gauge' ? '仪表盘' : '颜色映射'
}

// ===== 动画配置相关 =====
const animTypeOptions: { value: AnimationConfig['type'], label: string }[] = [
  { value: 'rotate', label: '旋转' },
  { value: 'float', label: '浮动' },
  { value: 'pulse', label: '脉冲' },
  { value: 'colorShift', label: '颜色渐变' },
]

/** 切换动画启用状态 */
function toggleAnimation() {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  if (!obj) return
  const current = obj.animation
  const next: AnimationConfig = current?.enabled
    ? { ...current, enabled: false }
    : { enabled: true, type: 'rotate', rotateSpeed: 30, rotateAxis: 'y' }
  editorStore.updateAnimation(editorStore.selectedObjectId, next)
}

/** 切换动画类型时设置该类型默认参数 */
function onAnimTypeChange(newType: AnimationConfig['type']) {
  if (!editorStore.selectedObjectId || !editorStore.selectedObject) return
  const obj = editorStore.selectedObject
  const current = obj.animation
  if (!current) return
  const next: AnimationConfig = { ...current, type: newType }
  // 为新类型补充默认参数（如果未设置）
  switch (newType) {
    case 'rotate':
      if (next.rotateSpeed === undefined) next.rotateSpeed = 30
      if (next.rotateAxis === undefined) next.rotateAxis = 'y'
      break
    case 'float':
      if (next.floatAmplitude === undefined) next.floatAmplitude = 0.5
      if (next.floatSpeed === undefined) next.floatSpeed = 1
      break
    case 'pulse':
      if (next.pulseScale === undefined) next.pulseScale = 0.1
      if (next.pulseSpeed === undefined) next.pulseSpeed = 2
      break
    case 'colorShift':
      if (!next.colorFrom) next.colorFrom = '#ff0000'
      if (!next.colorTo) next.colorTo = '#00ff00'
      if (next.colorSpeed === undefined) next.colorSpeed = 1
      break
  }
  editorStore.updateAnimation(editorStore.selectedObjectId, next)
}

/** 通用动画字段更新 */
function updateAnimField<K extends keyof AnimationConfig>(key: K, value: AnimationConfig[K]) {
  if (!editorStore.selectedObjectId || !editorStore.selectedObject?.animation) return
  const next: AnimationConfig = { ...editorStore.selectedObject.animation, [key]: value }
  editorStore.updateAnimation(editorStore.selectedObjectId, next)
}

// ===== 交互事件相关 =====
const showInteractionPicker = ref(false)
const interactionForm = ref<Partial<InteractionEvent>>({})
const interactionActionOptions: { value: InteractionEvent['action'], label: string }[] = [
  { value: 'highlight', label: '高亮' },
  { value: 'toggleVisible', label: '显示/隐藏' },
  { value: 'showData', label: '弹窗数据' },
  { value: 'navigate', label: '跳转链接' },
]

/** 动作类型中文标签（用于列表展示） */
function actionLabel(action: InteractionEvent['action']): string {
  return interactionActionOptions.find((o) => o.value === action)?.label || action
}

/** 打开交互事件添加弹窗 */
function addInteraction() {
  interactionForm.value = {
    trigger: 'click',
    targetId: 'self',
    action: 'highlight',
    highlightColor: '#ff0000',
    highlightDuration: 2000,
  }
  showInteractionPicker.value = true
}

/** 确认添加交互事件 */
function confirmAddInteraction() {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  if (!obj) return
  const form = interactionForm.value
  if (!form.action) {
    showError('提示', '请选择动作类型')
    return
  }
  if (form.action === 'navigate' && !form.navigateUrl?.trim()) {
    showError('提示', '请填写跳转 URL')
    return
  }
  const interactions = [...(obj.interactions || []), {
    id: `inter_${Date.now()}`,
    trigger: 'click',
    targetId: form.targetId || 'self',
    action: form.action,
    highlightColor: form.highlightColor,
    highlightDuration: form.highlightDuration,
    dataTitle: form.dataTitle,
    dataContent: form.dataContent,
    navigateUrl: form.navigateUrl,
  } as InteractionEvent]
  editorStore.updateInteractions(editorStore.selectedObjectId, interactions)
  showInteractionPicker.value = false
}

/** 删除某个交互事件 */
function removeInteraction(index: number) {
  if (!editorStore.selectedObjectId) return
  const obj = editorStore.selectedObject
  if (!obj?.interactions) return
  const next = [...obj.interactions]
  next.splice(index, 1)
  editorStore.updateInteractions(editorStore.selectedObjectId, next)
}
</script>

<template>
  <div class="h-[calc(100vh-4rem)] flex flex-col">
    <!-- 顶部工具栏 -->
    <div class="glass-card rounded-none border-b border-primary/10 p-2 flex items-center gap-2 shrink-0">
      <!-- 返回按钮 -->
      <button
        class="btn-ghost text-xs px-2 py-1.5 flex items-center gap-1.5"
        title="返回场景管理"
        @click="goBack"
      >
        <ArrowLeft :size="14" />
      </button>

      <div class="w-px h-6 bg-primary/15 mx-1" />

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
        class="btn-ghost text-xs px-2 py-1.5"
        :disabled="!editorStore.selectedObjectId"
        @click="duplicateSelected"
        title="复制 (Ctrl+D)"
      >
        <Copy :size="14" />
      </button>

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
          <!-- 几何体 -->
          <div class="border-b border-primary/10">
            <div class="px-3 py-2.5">
              <h3 class="text-xs font-medium text-primary-light flex items-center gap-1.5 mb-2">
                <Box :size="12" />
                几何体
              </h3>
              <div class="grid grid-cols-3 gap-1.5">
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('cube')">立方体</button>
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('sphere')">球体</button>
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('cylinder')">圆柱体</button>
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('plane')">平面</button>
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('cone')">圆锥体</button>
                <button class="px-2 py-1.5 rounded-lg text-[10px] text-gray-400 hover:bg-primary/10 hover:text-primary-light transition-all" @click="addPrimitiveToScene('torus')">圆环</button>
              </div>
            </div>
          </div>

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
              <Box v-else-if="obj.type === 'primitive'" :size="10" class="shrink-0 text-primary-light" />

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
                  @click="obj.type === 'chart' ? editorStore.selectObject(obj.id) : engine?.selectObjectById(obj.id)"
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

              <!-- 删除按钮 -->
              <button
                class="p-0.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity text-gray-500 hover:text-danger"
                @click.stop="deleteObjectById(obj.id)"
              >
                <Trash2 :size="9" />
              </button>
            </div>
            <p v-if="editorStore.objects.length === 0" class="text-xs text-gray-600 py-2 text-center">
              场景为空
            </p>
          </div>
        </div>

        <!-- 环境设置 -->
        <div class="border-t border-primary/10 p-3">
          <h3 class="text-xs font-medium text-primary-light mb-2 flex items-center gap-1.5">
            <Settings :size="12" />
            环境
          </h3>
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <span class="text-xs text-gray-500 w-12">背景色</span>
              <input
                type="color"
                :value="editorStore.environment.backgroundColor"
                class="w-8 h-6 rounded cursor-pointer bg-transparent border-none"
                @input="updateBackgroundColor(($event.target as HTMLInputElement).value)"
              />
            </div>
            <div class="flex items-center gap-2">
              <span class="text-xs text-gray-500 w-12">雾效</span>
              <input
                type="color"
                :value="editorStore.environment.fogColor || '#1a1a2e'"
                class="w-8 h-6 rounded cursor-pointer bg-transparent border-none"
                @input="updateFogColor(($event.target as HTMLInputElement).value)"
              />
              <input
                type="range" min="0" max="0.1" step="0.001"
                :value="editorStore.environment.fogDensity || 0"
                class="flex-1 accent-primary"
                @input="updateFogDensity(parseFloat(($event.target as HTMLInputElement).value))"
              />
            </div>
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

          <!-- 变换属性（可编辑） -->
          <div class="mb-4">
            <h4 class="text-xs font-medium text-primary-light mb-2">变换</h4>
            <div class="space-y-2">
              <div v-for="(label, key) in ({ position: '位置', rotation: '旋转', scale: '缩放' } as const)" :key="key">
                <p class="text-xs text-gray-500 mb-1">{{ label }}</p>
                <div class="grid grid-cols-3 gap-1.5">
                  <div v-for="(axis, i) in ['X', 'Y', 'Z']" :key="axis">
                    <div class="flex items-center gap-1 bg-dark-surface/50 rounded-md px-2 py-1">
                      <span class="text-[10px]" :class="i === 0 ? 'text-red-400' : i === 1 ? 'text-green-400' : 'text-blue-400'">{{ axis }}</span>
                      <input
                        type="number"
                        step="0.1"
                        :value="editorStore.selectedObject?.transform?.[key]?.[i]?.toFixed(1) || 0"
                        class="w-full bg-transparent text-xs text-gray-300 focus:outline-none"
                        @change="updateTransformValue(key, i, parseFloat(($event.target as HTMLInputElement).value))"
                      />
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

          <!-- 数据绑定 -->
          <div class="mb-4">
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-xs font-medium text-primary-light flex items-center gap-1.5">
                <Link2 :size="12" />
                数据绑定
              </h4>
              <button
                class="text-xs text-gray-500 hover:text-primary-light transition-colors"
                @click="openBindingPicker"
              >
                + 添加
              </button>
            </div>
            <div v-if="editorStore.selectedObject.dataBindings?.length" class="space-y-2">
              <div
                v-for="(binding, i) in editorStore.selectedObject.dataBindings"
                :key="i"
                class="bg-dark-surface/50 rounded-lg p-2 border border-primary/10 hover:border-primary/30 transition-colors"
              >
                <div class="flex items-center justify-between">
                  <span class="text-xs text-gray-300 truncate">{{ binding.label || binding.indicatorName }}</span>
                  <button
                    class="text-xs text-gray-600 hover:text-danger shrink-0 ml-2"
                    @click="removeBinding(i)"
                  >
                    <X :size="12" />
                  </button>
                </div>
                <div class="text-[10px] text-gray-500 mt-1">
                  {{ displayModeLabel(binding.displayMode) }}
                  <span v-if="binding.unit"> · {{ binding.unit }}</span>
                </div>
              </div>
            </div>
            <p v-else class="text-xs text-gray-600">暂无数据绑定</p>
          </div>

          <!-- 动画配置 -->
          <div class="mb-4">
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-xs font-medium text-primary-light flex items-center gap-1.5">
                <PlayCircle :size="12" />
                动画
              </h4>
              <button
                class="text-xs px-2 py-0.5 rounded transition-colors"
                :class="editorStore.selectedObject?.animation?.enabled
                  ? 'bg-primary/20 text-primary-light hover:bg-primary/30'
                  : 'text-gray-500 hover:text-primary-light hover:bg-primary/10'"
                @click="toggleAnimation"
              >
                {{ editorStore.selectedObject?.animation?.enabled ? '关闭' : '开启' }}
              </button>
            </div>
            <div v-if="editorStore.selectedObject?.animation?.enabled" class="space-y-2">
              <!-- 动画类型选择 -->
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500 w-12">类型</span>
                <div class="relative flex-1">
                  <select
                    :value="editorStore.selectedObject.animation.type"
                    class="w-full bg-dark-surface/50 border border-primary/15 rounded-md px-2 py-1 text-xs text-white focus:border-primary/40 focus:outline-none appearance-none"
                    @change="onAnimTypeChange(($event.target as HTMLSelectElement).value as AnimationConfig['type'])"
                  >
                    <option v-for="opt in animTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                  <ChevronDown :size="10" class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
                </div>
              </div>

              <!-- 旋转参数 -->
              <template v-if="editorStore.selectedObject.animation.type === 'rotate'">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">速度</span>
                  <input
                    type="range" min="10" max="180" step="5"
                    :value="editorStore.selectedObject.animation.rotateSpeed ?? 30"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('rotateSpeed', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">{{ editorStore.selectedObject.animation.rotateSpeed ?? 30 }}°/s</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">轴</span>
                  <div class="flex gap-1">
                    <button
                      v-for="ax in ['x', 'y', 'z'] as const"
                      :key="ax"
                      class="px-2 py-0.5 rounded text-[10px] uppercase transition-all"
                      :class="(editorStore.selectedObject.animation.rotateAxis || 'y') === ax
                        ? 'bg-primary/20 text-primary-light border border-primary/30'
                        : 'text-gray-500 hover:text-gray-300 border border-transparent hover:bg-primary/5'"
                      @click="updateAnimField('rotateAxis', ax)"
                    >
                      {{ ax }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- 浮动参数 -->
              <template v-if="editorStore.selectedObject.animation.type === 'float'">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">幅度</span>
                  <input
                    type="range" min="0.1" max="2" step="0.1"
                    :value="editorStore.selectedObject.animation.floatAmplitude ?? 0.5"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('floatAmplitude', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">{{ (editorStore.selectedObject.animation.floatAmplitude ?? 0.5).toFixed(1) }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">速度</span>
                  <input
                    type="range" min="0.5" max="3" step="0.1"
                    :value="editorStore.selectedObject.animation.floatSpeed ?? 1"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('floatSpeed', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">{{ (editorStore.selectedObject.animation.floatSpeed ?? 1).toFixed(1) }}</span>
                </div>
              </template>

              <!-- 脉冲参数 -->
              <template v-if="editorStore.selectedObject.animation.type === 'pulse'">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">幅度</span>
                  <input
                    type="range" min="0.02" max="0.5" step="0.02"
                    :value="editorStore.selectedObject.animation.pulseScale ?? 0.1"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('pulseScale', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">±{{ Math.round((editorStore.selectedObject.animation.pulseScale ?? 0.1) * 100) }}%</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">速度</span>
                  <input
                    type="range" min="0.5" max="5" step="0.1"
                    :value="editorStore.selectedObject.animation.pulseSpeed ?? 2"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('pulseSpeed', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">{{ (editorStore.selectedObject.animation.pulseSpeed ?? 2).toFixed(1) }}</span>
                </div>
              </template>

              <!-- 颜色渐变参数 -->
              <template v-if="editorStore.selectedObject.animation.type === 'colorShift'">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">起始</span>
                  <input
                    type="color"
                    :value="editorStore.selectedObject.animation.colorFrom || '#ff0000'"
                    class="w-8 h-6 rounded cursor-pointer bg-transparent border-none"
                    @input="updateAnimField('colorFrom', ($event.target as HTMLInputElement).value)"
                  />
                  <span class="text-xs text-gray-400 flex-1">{{ editorStore.selectedObject.animation.colorFrom || '#ff0000' }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">结束</span>
                  <input
                    type="color"
                    :value="editorStore.selectedObject.animation.colorTo || '#00ff00'"
                    class="w-8 h-6 rounded cursor-pointer bg-transparent border-none"
                    @input="updateAnimField('colorTo', ($event.target as HTMLInputElement).value)"
                  />
                  <span class="text-xs text-gray-400 flex-1">{{ editorStore.selectedObject.animation.colorTo || '#00ff00' }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-12">速度</span>
                  <input
                    type="range" min="0.2" max="4" step="0.1"
                    :value="editorStore.selectedObject.animation.colorSpeed ?? 1"
                    class="flex-1 accent-primary"
                    @input="updateAnimField('colorSpeed', parseFloat(($event.target as HTMLInputElement).value))"
                  />
                  <span class="text-xs text-gray-400 w-10 text-right">{{ (editorStore.selectedObject.animation.colorSpeed ?? 1).toFixed(1) }}</span>
                </div>
                <p v-if="!editorStore.selectedObject.material" class="text-[10px] text-yellow-500/80">
                  注意：颜色渐变仅在带材质的对象（模型/几何体）上有效
                </p>
              </template>
            </div>
          </div>

          <!-- 交互事件 -->
          <div class="mb-4">
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-xs font-medium text-primary-light flex items-center gap-1.5">
                <MousePointerClick :size="12" />
                交互事件
              </h4>
              <button
                class="text-xs text-gray-500 hover:text-primary-light transition-colors"
                @click="addInteraction"
              >
                + 添加
              </button>
            </div>
            <div v-if="editorStore.selectedObject.interactions?.length" class="space-y-2">
              <div
                v-for="(inter, i) in editorStore.selectedObject.interactions"
                :key="inter.id"
                class="bg-dark-surface/50 rounded-lg p-2 border border-primary/10 hover:border-primary/30 transition-colors"
              >
                <div class="flex items-center justify-between">
                  <span class="text-xs text-gray-300">{{ actionLabel(inter.action) }}</span>
                  <button
                    class="text-xs text-gray-600 hover:text-danger shrink-0 ml-2"
                    @click="removeInteraction(i)"
                  >
                    <X :size="12" />
                  </button>
                </div>
                <div class="text-[10px] text-gray-500 mt-1">
                  目标: {{ inter.targetId === 'self' ? '自身' : (editorStore.objects.find(o => o.id === inter.targetId)?.name || inter.targetId) }}
                </div>
              </div>
            </div>
            <p v-else class="text-xs text-gray-600">暂无交互事件</p>
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
      <span>三角面: <span class="text-white">{{ sceneStats.triangles }}</span></span>
      <span>顶点: <span class="text-white">{{ sceneStats.vertices }}</span></span>
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

    <!-- 通知弹窗 -->
    <Teleport to="body">
      <div v-if="notice" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="closeNotice">
        <div class="glass-card w-full max-w-md mx-4 p-6 animate-slide-up">
          <div class="flex items-start gap-3">
            <div class="shrink-0 mt-0.5">
              <CheckCircle2 v-if="notice.type === 'success'" :size="20" class="text-green-400" />
              <AlertCircle v-else-if="notice.type === 'error'" :size="20" class="text-danger" />
              <AlertCircle v-else :size="20" class="text-primary-light" />
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-base font-semibold text-white">{{ notice.title }}</h3>
              <p v-if="notice.message" class="text-sm text-gray-400 mt-1.5 break-words">{{ notice.message }}</p>
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="goBack">返回</button>
            <button class="flex-1 btn-primary text-sm" @click="closeNotice">确认</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 数据绑定选择器弹窗 -->
    <Teleport to="body">
      <div
        v-if="showBindingPicker"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        @click.self="showBindingPicker = false"
      >
        <div class="glass-card w-full max-w-md mx-4 p-6 animate-slide-up">
          <h3 class="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Link2 :size="18" class="text-primary-light" />
            添加数据绑定
          </h3>

          <!-- 指标选择 -->
          <label class="block text-sm text-gray-400 mb-1.5">选择指标</label>
          <div class="relative">
            <select
              v-model.number="bindingForm.indicatorId"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white mb-1 focus:border-primary/40 focus:outline-none transition-all appearance-none"
              @change="onIndicatorPicked(bindingForm.indicatorId!)"
            >
              <option :value="undefined" disabled>请选择指标...</option>
              <option v-for="ind in indicatorList" :key="ind.id" :value="ind.id">
                {{ ind.name }}
              </option>
            </select>
            <ChevronDown :size="14" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
          </div>
          <p v-if="indicatorListLoading" class="text-[10px] text-gray-500 mt-1">加载指标列表中...</p>

          <!-- 显示标签 -->
          <label class="block text-sm text-gray-400 mb-1.5 mt-3">显示标签</label>
          <input
            v-model="bindingForm.label"
            type="text"
            placeholder="如：温度、压力"
            class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          />

          <!-- 显示模式 -->
          <label class="block text-sm text-gray-400 mb-1.5 mt-3">显示模式</label>
          <div class="relative">
            <select
              v-model="bindingForm.displayMode"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white focus:border-primary/40 focus:outline-none transition-all appearance-none"
            >
              <option value="text">文本</option>
              <option value="gauge">仪表盘</option>
              <option value="color">颜色映射</option>
            </select>
            <ChevronDown :size="14" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
          </div>

          <!-- 单位 -->
          <label class="block text-sm text-gray-400 mb-1.5 mt-3">单位</label>
          <input
            v-model="bindingForm.unit"
            type="text"
            placeholder="如：°C、MPa、rpm"
            class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
          />

          <!-- 确认按钮 -->
          <div class="flex gap-3 mt-6">
            <button class="flex-1 btn-ghost text-sm" @click="showBindingPicker = false">取消</button>
            <button
              class="flex-1 btn-primary text-sm"
              :disabled="!bindingForm.indicatorId"
              @click="confirmAddBinding"
            >
              确认绑定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
