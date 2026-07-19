<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import * as THREE from 'three'
import { useRoute, useRouter } from 'vue-router'
import { SceneRenderer } from '@/three/SceneRenderer'
import { previewScene } from '@/api/scene'
import { getModelUrl } from '@/api/model'
import { modelLoader } from '@/three/ModelLoader'
import { getIndicatorsByIds } from '@/api/indicator'
import { RotateCcw, Maximize, PanelRight, PanelRightClose, MousePointerClick, X } from 'lucide-vue-next'
import DataOverlay, { type IndicatorValue } from '@/components/scene/DataOverlay.vue'
import ChartRenderer from '@/components/scene/ChartRenderer.vue'
import type { SceneData, SceneObject, DataBinding, InteractionEvent, AnimationConfig } from '@/types/scene'

const route = useRoute()
const router = useRouter()
const containerRef = ref<HTMLDivElement>()
const loading = ref(true)
const error = ref('')
const previewRequired = ref(false)
const previewPassword = ref('')
const previewError = ref('')
const previewVerifying = ref(false)
const sceneName = ref('')
let renderer: SceneRenderer | null = null

// 数据绑定相关
const sceneObjects = ref<SceneObject[]>([])
const dataRefreshing = ref(false)
const indicatorValues = ref<Map<number, IndicatorValue>>(new Map())
const showDataPanel = ref(true)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 交互事件相关
/** sceneId -> THREE.Object3D 映射，用于 raycaster 查找与动作执行 */
const objectMap = new Map<string, THREE.Object3D>()
/** 当前是否有可交互对象被悬停（用于显示提示） */
const hoverInteractive = ref(false)
/** 交互数据弹窗 */
const showInteractionPanel = ref(false)
const interactionData = ref<{ title: string; content: string }>({ title: '', content: '' })
const highlightedChartId = ref<string | null>(null)
/** 当前点击的对象ID（用于 'self' 目标解析） */
let currentClickedId = ''

const raycaster = new THREE.Raycaster()
const mouseNDC = new THREE.Vector2()

// 动画播放相关
const animationClock = new THREE.Clock()
const originalTransforms = new Map<string, { position: THREE.Vector3, scale: THREE.Vector3, color: THREE.Color }>()

/** 收集场景中所有数据绑定（带上来源对象名） */
const allBindings = computed<Array<DataBinding & { objectName?: string }>>(() => {
  const list: Array<DataBinding & { objectName?: string }> = []
  for (const obj of sceneObjects.value) {
    if (obj.visible === false) continue
    if (!obj.dataBindings?.length) continue
    for (const b of obj.dataBindings) {
      list.push({ ...b, objectName: obj.name })
    }
  }
  return list
})

const hasBindings = computed(() => allBindings.value.length > 0)

onMounted(async () => {
  const id = Number(route.params.sceneId)
  if (!id) {
    error.value = '无效的场景 ID'
    loading.value = false
    return
  }

  try {
    const sessionKey = `scene-preview-password-${id}`
    const storedPassword = sessionStorage.getItem(sessionKey) || ''
    const res = await previewScene(id, storedPassword)
    sessionStorage.removeItem(sessionKey)
    const scene = res.data.data
    sceneName.value = scene.name

    if (!containerRef.value) return
    renderer = new SceneRenderer(containerRef.value)
    renderer.setGradientBackground('#0a0a1a', '#1a1a3e')
    // 注册每帧动画更新回调（在 startRender 之前设置，确保首帧就生效）
    renderer.setAnimationCallback(() => updateAnimations())
    renderer.startRender()

    let sceneData: SceneData | null = null
    if (scene.sceneData) {
      try {
        sceneData = typeof scene.sceneData === 'string' ? JSON.parse(scene.sceneData) : scene.sceneData
      } catch {
        console.warn('场景数据解析失败')
      }
    }

    if (!sceneData?.objects) {
      loading.value = false
      return
    }

    sceneObjects.value = sceneData.objects

    // 加载场景对象
    const sceneGroup = new THREE.Group()
    for (const obj of sceneData.objects) {
      // 跳过隐藏的对象
      if (obj.visible === false) continue

      if (obj.type === 'model' && obj.modelRef) {
        try {
          const dlRes = await getModelUrl(obj.modelRef)
          const url = dlRes.data.data?.downloadUrl
          if (url) {
            const model = await modelLoader.loadModel(url)
            model.userData.sceneId = obj.id
            // 应用保存的 transform，不做 autoCenter
            if (obj.transform) {
              model.position.fromArray(obj.transform.position)
              model.rotation.set(
                THREE.MathUtils.degToRad(obj.transform.rotation[0]),
                THREE.MathUtils.degToRad(obj.transform.rotation[1]),
                THREE.MathUtils.degToRad(obj.transform.rotation[2])
              )
              model.scale.fromArray(obj.transform.scale)
            }
            renderer.getScene().add(model)
            objectMap.set(obj.id, model)
            sceneGroup.add(model.clone())
          }
        } catch (e) {
          console.warn('加载模型失败', obj.modelRef, e)
        }
      } else if (obj.type === 'primitive' && obj.primitiveType) {
        // 预览也支持基础几何体（动画常用在几何体上）
        let geometry: THREE.BufferGeometry
        switch (obj.primitiveType) {
          case 'cube': geometry = new THREE.BoxGeometry(1, 1, 1); break
          case 'sphere': geometry = new THREE.SphereGeometry(0.5, 32, 32); break
          case 'cylinder': geometry = new THREE.CylinderGeometry(0.5, 0.5, 1, 32); break
          case 'plane': geometry = new THREE.PlaneGeometry(2, 2); break
          case 'cone': geometry = new THREE.ConeGeometry(0.5, 1, 32); break
          case 'torus': geometry = new THREE.TorusGeometry(0.5, 0.2, 16, 32); break
        }
        const mat = new THREE.MeshStandardMaterial({
          color: obj.material?.color || '#cccccc',
          metalness: obj.material?.metalness ?? 0,
          roughness: obj.material?.roughness ?? 0.5,
        })
        const mesh = new THREE.Mesh(geometry, mat)
        mesh.castShadow = true
        mesh.receiveShadow = true
        mesh.userData.sceneId = obj.id
        if (obj.transform) {
          mesh.position.fromArray(obj.transform.position)
          mesh.rotation.set(
            THREE.MathUtils.degToRad(obj.transform.rotation[0]),
            THREE.MathUtils.degToRad(obj.transform.rotation[1]),
            THREE.MathUtils.degToRad(obj.transform.rotation[2])
          )
          mesh.scale.fromArray(obj.transform.scale)
        }
        renderer.getScene().add(mesh)
        objectMap.set(obj.id, mesh)
        sceneGroup.add(mesh.clone())
      } else if (obj.type === 'light' && obj.light) {
        let light: THREE.Light
        switch (obj.light.type) {
          case 'directional':
            light = new THREE.DirectionalLight(obj.light.color, obj.light.intensity)
            light.position.set(5, 8, 5)
            break
          case 'point':
            light = new THREE.PointLight(obj.light.color, obj.light.intensity, obj.light.distance || 20)
            light.position.set(0, 5, 0)
            break
          case 'spot':
            light = new THREE.SpotLight(obj.light.color, obj.light.intensity, obj.light.distance || 20, obj.light.angle || 0.5)
            light.position.set(0, 8, 0)
            break
          default:
            light = new THREE.AmbientLight(obj.light?.color || 0xffffff, obj.light?.intensity || 0.5)
        }
        light.userData.sceneId = obj.id
        if (obj.transform) {
          light.position.fromArray(obj.transform.position)
        }
        renderer.getScene().add(light)
        objectMap.set(obj.id, light)
      }
    }

    // 自动调整相机适配所有模型
    if (sceneGroup.children.length > 0) {
      renderer.fitCameraToObject(sceneGroup, 2)
      sceneGroup.traverse((child) => {
        if ((child as THREE.Mesh).isMesh) {
          ;(child as THREE.Mesh).geometry?.dispose()
        }
      })
    }

    // 加载数据绑定指标值
    await refreshIndicatorValues()

    // 启动定时刷新（30s）
    refreshTimer = setInterval(refreshIndicatorValues, 30_000)

    // 注册交互事件监听
    const canvas = renderer.renderer.domElement
    canvas.addEventListener('click', handleSceneClick)
    canvas.addEventListener('mousemove', handleSceneMouseMove)
  } catch (e) {
    if (e instanceof Error && e.message.includes('预览密码')) {
      previewRequired.value = true
      return
    }
    console.error('加载场景失败:', e)
    error.value = '加载场景失败，请返回重试'
  } finally {
    loading.value = false
  }
})

async function verifyPreview() {
  const id = Number(route.params.sceneId)
  if (!id || !previewPassword.value.trim()) {
    previewError.value = '请输入预览密码'
    return
  }
  previewVerifying.value = true
  previewError.value = ''
  try {
    await previewScene(id, previewPassword.value)
    sessionStorage.setItem(`scene-preview-password-${id}`, previewPassword.value)
    window.location.reload()
  } catch (e) {
    previewError.value = e instanceof Error ? e.message : '密码验证失败'
  } finally {
    previewVerifying.value = false
  }
}

/** 拉取所有绑定指标的最新值 */
async function refreshIndicatorValues() {
  const ids = Array.from(new Set(allBindings.value.map((b) => b.indicatorId)))
  if (ids.length === 0) return
  dataRefreshing.value = true
  try {
    const list = await getIndicatorsByIds(ids)
    const map = new Map<number, IndicatorValue>()
    for (const ind of list) {
      if (!ind) continue
      map.set(ind.id, {
        name: ind.name,
        value: ind.value,
        valueType: ind.valueType,
      })
    }
    indicatorValues.value = map
  } catch (e) {
    console.warn('刷新指标值失败:', e)
  } finally {
    dataRefreshing.value = false
  }
}

function resetCamera() {
  if (!renderer) return
  const objects: THREE.Object3D[] = []
  renderer.getScene().traverse((child) => {
    if (child.userData.sceneId) objects.push(child)
  })
  if (objects.length > 0) {
    const group = new THREE.Group()
    objects.forEach((o) => group.add(o.clone()))
    renderer.fitCameraToObject(group, 2)
    group.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        ;(child as THREE.Mesh).geometry?.dispose()
      }
    })
  }
}

function toggleFullscreen() {
  if (!containerRef.value) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    containerRef.value.requestFullscreen()
  }
}

// ===== 动画播放 =====

/** 每帧更新所有对象的动画 */
function updateAnimations() {
  if (!renderer) return
  const delta = animationClock.getDelta()
  const elapsed = animationClock.getElapsedTime()

  objectMap.forEach((obj, id) => {
    const sceneObj = sceneObjects.value.find((o) => o.id === id)
    const animConfig = sceneObj?.animation as AnimationConfig | undefined
    if (!animConfig || !animConfig.enabled) return

    // 首次启用动画时保存原始变换
    if (!originalTransforms.has(id)) {
      originalTransforms.set(id, {
        position: obj.position.clone(),
        scale: obj.scale.clone(),
        color: getObjectColor(obj),
      })
    }
    const original = originalTransforms.get(id)!

    switch (animConfig.type) {
      case 'rotate': {
        const speed = THREE.MathUtils.degToRad(animConfig.rotateSpeed ?? 30)
        const axis = animConfig.rotateAxis || 'y'
        obj.rotation[axis] += speed * delta
        break
      }
      case 'float': {
        const amplitude = animConfig.floatAmplitude ?? 0.5
        const floatSpeed = animConfig.floatSpeed ?? 1
        obj.position.y = original.position.y + Math.sin(elapsed * floatSpeed) * amplitude
        break
      }
      case 'pulse': {
        const pulseScale = animConfig.pulseScale ?? 0.1
        const pulseSpeed = animConfig.pulseSpeed ?? 2
        const scaleFactor = 1 + Math.sin(elapsed * pulseSpeed) * pulseScale
        obj.scale.set(
          original.scale.x * scaleFactor,
          original.scale.y * scaleFactor,
          original.scale.z * scaleFactor
        )
        break
      }
      case 'colorShift': {
        const fromColor = new THREE.Color(animConfig.colorFrom || '#ff0000')
        const toColor = new THREE.Color(animConfig.colorTo || '#00ff00')
        const colorSpeed = animConfig.colorSpeed ?? 1
        const t = (Math.sin(elapsed * colorSpeed) + 1) / 2
        const currentColor = fromColor.clone().lerp(toColor, t)
        setObjectColor(obj, currentColor)
        break
      }
    }
  })
}

/** 获取对象主颜色 */
function getObjectColor(obj: THREE.Object3D): THREE.Color {
  let color = new THREE.Color(0xcccccc)
  obj.traverse((child) => {
    if ((child as THREE.Mesh).isMesh) {
      const mat = (child as THREE.Mesh).material as THREE.MeshStandardMaterial
      if (mat?.isMeshStandardMaterial) {
        color = mat.color.clone()
      }
    }
  })
  return color
}

/** 设置对象所有 Mesh 的颜色 */
function setObjectColor(obj: THREE.Object3D, color: THREE.Color): void {
  obj.traverse((child) => {
    if ((child as THREE.Mesh).isMesh) {
      const mat = (child as THREE.Mesh).material as THREE.MeshStandardMaterial
      if (mat?.isMeshStandardMaterial) {
        mat.color.copy(color)
      }
    }
  })
}

// ===== 交互事件处理 =====

/** 收集所有可交互（有 interactions 配置且可见）的对象的 mesh 子节点 */
function collectInteractiveMeshes(): THREE.Mesh[] {
  const meshes: THREE.Mesh[] = []
  sceneObjects.value.forEach((obj) => {
    if (obj.visible === false) return
    if (!obj.interactions?.length) return
    const threeObj = objectMap.get(obj.id)
    if (!threeObj) return
    threeObj.traverse((child) => {
      const mesh = child as THREE.Mesh
      if (mesh.isMesh) meshes.push(mesh)
    })
  })
  return meshes
}

/** 通过 raycast 找到点击的对象的 sceneId */
function pickSceneId(event: MouseEvent): string | null {
  if (!renderer) return null
  const canvas = renderer.renderer.domElement
  const rect = canvas.getBoundingClientRect()
  mouseNDC.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouseNDC.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouseNDC, renderer.camera)
  const meshes = collectInteractiveMeshes()
  if (meshes.length === 0) return null
  const intersects = raycaster.intersectObjects(meshes, false)
  if (intersects.length === 0) return null
  let obj: THREE.Object3D | null = intersects[0].object
  while (obj && !obj.userData.sceneId) obj = obj.parent
  return obj?.userData.sceneId || null
}

/** 场景点击处理 */
function handleSceneClick(event: MouseEvent) {
  const sceneId = pickSceneId(event)
  if (!sceneId) return
  currentClickedId = sceneId
  handleObjectClick(sceneId)
}

/** 鼠标移动：检测是否悬停在可交互对象上，切换 cursor */
function handleSceneMouseMove(event: MouseEvent) {
  const sceneId = pickSceneId(event)
  const interactive = !!sceneId
  if (interactive !== hoverInteractive.value) {
    hoverInteractive.value = interactive
    if (renderer) {
      renderer.renderer.domElement.style.cursor = interactive ? 'pointer' : 'default'
    }
  }
}

/** 处理对象点击：查找其 interactions 并依次执行 */
function handleObjectClick(objectId: string) {
  const obj = sceneObjects.value.find((o) => o.id === objectId)
  if (!obj?.interactions?.length) return
  obj.interactions.forEach((interaction) => executeInteraction(interaction))
}

function handleChartClick(objectId: string) {
  currentClickedId = objectId
  handleObjectClick(objectId)
}

/** 执行单个交互事件 */
function executeInteraction(interaction: InteractionEvent) {
  const targetId = interaction.targetId === 'self' ? currentClickedId : interaction.targetId
  const targetObj = targetId ? objectMap.get(targetId) : undefined
  const targetSceneObj = sceneObjects.value.find((object) => object.id === targetId)

  switch (interaction.action) {
    case 'highlight':
      if (targetObj) applyHighlight(targetObj, interaction)
      if (targetSceneObj?.type === 'chart') {
        highlightedChartId.value = targetId || null
        window.setTimeout(() => {
          if (highlightedChartId.value === targetId) highlightedChartId.value = null
        }, interaction.highlightDuration || 2000)
      }
      break
    case 'toggleVisible':
      if (targetObj) targetObj.visible = !targetObj.visible
      if (targetSceneObj) targetSceneObj.visible = targetSceneObj.visible === false
      break
    case 'showData':
      interactionData.value = {
        title: interaction.dataTitle || '数据详情',
        content: interaction.dataContent || '',
      }
      showInteractionPanel.value = true
      break
    case 'navigate':
      if (interaction.navigateUrl) window.open(interaction.navigateUrl, '_blank')
      break
  }
}

/** 临时高亮目标对象：保存原色 -> 改为高亮色 -> 定时恢复 */
function applyHighlight(target: THREE.Object3D, interaction: InteractionEvent) {
  const color = interaction.highlightColor || '#ff0000'
  const duration = interaction.highlightDuration || 2000
  const saved: Array<{ mesh: THREE.Mesh; color: THREE.Color; emissive: THREE.Color; intensity: number }> = []
  target.traverse((child) => {
    const mesh = child as THREE.Mesh
    if (!mesh.isMesh) return
    const mat = mesh.material as THREE.MeshStandardMaterial | THREE.MeshStandardMaterial[]
    if (Array.isArray(mat)) {
      mat.forEach((m) => saveAndHighlight(m, color, saved, mesh))
    } else if (mat) {
      saveAndHighlight(mat, color, saved, mesh)
    }
  })
  setTimeout(() => {
    saved.forEach(({ mesh, color: origColor, emissive, intensity }) => {
      const mat = mesh.material as THREE.MeshStandardMaterial | THREE.MeshStandardMaterial[]
      if (Array.isArray(mat)) {
        mat.forEach((m) => {
          m.color.copy(origColor)
          m.emissive.copy(emissive)
          m.emissiveIntensity = intensity
        })
      } else if (mat) {
        mat.color.copy(origColor)
        mat.emissive.copy(emissive)
        mat.emissiveIntensity = intensity
      }
    })
  }, duration)
}

function saveAndHighlight(
  mat: THREE.MeshStandardMaterial,
  color: string,
  saved: Array<{ mesh: THREE.Mesh; color: THREE.Color; emissive: THREE.Color; intensity: number }>,
  mesh: THREE.Mesh
) {
  saved.push({
    mesh,
    color: mat.color.clone(),
    emissive: mat.emissive.clone(),
    intensity: mat.emissiveIntensity,
  })
  mat.color.set(color)
  mat.emissive.set(color)
  mat.emissiveIntensity = 0.5
}

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (renderer) {
    const canvas = renderer.renderer.domElement
    canvas.removeEventListener('click', handleSceneClick)
    canvas.removeEventListener('mousemove', handleSceneMouseMove)
  }
  objectMap.clear()
  originalTransforms.clear()
  renderer?.dispose()
  renderer = null
})
</script>

<template>
  <div class="w-screen h-screen bg-dark overflow-hidden flex flex-col">
    <!-- 简洁顶部栏 -->
    <div class="h-10 bg-dark-surface/80 border-b border-primary/10 px-4 flex items-center justify-between shrink-0 backdrop-blur-sm">
      <div class="flex items-center gap-3">
        <span class="text-sm text-white/80 font-medium">{{ sceneName || '场景预览' }}</span>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-if="hasBindings"
          class="p-1.5 rounded-lg text-gray-500 hover:text-primary-light hover:bg-primary/10 transition-all"
          :title="showDataPanel ? '隐藏数据面板' : '显示数据面板'"
          @click="showDataPanel = !showDataPanel"
        >
          <PanelRightClose v-if="showDataPanel" :size="16" />
          <PanelRight v-else :size="16" />
        </button>
        <button
          class="p-1.5 rounded-lg text-gray-500 hover:text-primary-light hover:bg-primary/10 transition-all"
          title="重置视角"
          @click="resetCamera"
        >
          <RotateCcw :size="16" />
        </button>
        <button
          class="p-1.5 rounded-lg text-gray-500 hover:text-primary-light hover:bg-primary/10 transition-all"
          title="全屏"
          @click="toggleFullscreen"
        >
          <Maximize :size="16" />
        </button>
        <span class="text-[10px] text-gray-600 ml-2">只读预览</span>
      </div>
    </div>

    <!-- 主体：3D 渲染区 + 数据面板 -->
    <div class="flex-1 flex overflow-hidden relative">
      <!-- 渲染区域 -->
      <div ref="containerRef" class="relative flex-1 w-full overflow-hidden">
        <div class="absolute inset-0 z-10 pointer-events-none">
          <div
            v-for="obj in sceneObjects.filter((item) => item.type === 'chart' && item.visible !== false)"
            :key="obj.id"
            class="absolute pointer-events-auto"
            :style="{
              left: `${obj.chartConfig?.position?.x ?? 24}px`,
              top: `${obj.chartConfig?.position?.y ?? 24}px`,
              zIndex: obj.chartConfig?.position?.zIndex ?? 1,
            }"
          >
            <ChartRenderer
              v-if="obj.chartConfig"
              :chart-config="obj.chartConfig"
              :indicator-value="indicatorValues.get(obj.chartConfig.indicatorId || obj.dataBindings?.[0]?.indicatorId || 0)?.value"
              :highlighted="highlightedChartId === obj.id"
              @click="handleChartClick(obj.id)"
            />
          </div>
        </div>
      </div>

      <!-- 数据覆盖面板（右侧浮动） -->
      <div
        v-if="showDataPanel && hasBindings"
        class="absolute top-3 right-3 bottom-3 w-72 z-10 pointer-events-auto"
      >
        <DataOverlay
          :bindings="allBindings"
          :indicator-values="indicatorValues"
          :loading="dataRefreshing"
          @refresh="refreshIndicatorValues"
        />
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-dark/80 z-20">
      <div class="text-center">
        <div class="w-12 h-12 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto mb-3" />
        <p class="text-sm text-gray-400">加载场景中...</p>
      </div>
    </div>

    <!-- 错误 -->
    <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-dark/80 z-20">
      <div class="text-center">
        <p class="text-danger mb-3">{{ error }}</p>
        <button class="btn-primary text-sm" @click="router.back()">返回</button>
      </div>
    </div>

    <!-- 预览密码验证 -->
    <div v-if="previewRequired" class="absolute inset-0 flex items-center justify-center bg-dark/90 z-30">
      <div class="glass-card w-full max-w-sm mx-4 p-6">
        <h2 class="text-lg font-semibold text-white mb-2">需要预览密码</h2>
        <p class="text-sm text-gray-400 mb-4">该场景设置了预览密码，请验证后继续。</p>
        <input
          v-model="previewPassword"
          type="password"
          autofocus
          placeholder="请输入预览密码"
          class="w-full bg-dark-surface/60 border border-primary/20 rounded-lg px-3 py-2.5 text-sm text-white placeholder-gray-600 focus:border-primary/50 focus:outline-none"
          @keyup.enter="verifyPreview"
        />
        <p v-if="previewError" class="text-sm text-danger mt-2">{{ previewError }}</p>
        <div class="flex gap-2 mt-4">
          <button class="btn-ghost flex-1 text-sm" @click="router.back()">返回</button>
          <button class="btn-primary flex-1 text-sm" :disabled="previewVerifying" @click="verifyPreview">
            {{ previewVerifying ? '验证中...' : '验证并预览' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 交互提示标识 -->
    <div
      v-if="hoverInteractive"
      class="absolute top-14 left-1/2 -translate-x-1/2 z-10 px-3 py-1 rounded-full bg-primary/20 border border-primary/30 backdrop-blur-sm flex items-center gap-1.5 pointer-events-none"
    >
      <MousePointerClick :size="12" class="text-primary-light" />
      <span class="text-[10px] text-primary-light">点击触发交互</span>
    </div>

    <!-- 交互数据弹窗 -->
    <Teleport to="body">
      <div
        v-if="showInteractionPanel"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        @click.self="showInteractionPanel = false"
      >
        <div class="glass-card w-full max-w-md mx-4 p-6 animate-slide-up">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-lg font-semibold text-white">{{ interactionData.title }}</h3>
            <button
              class="p-1 rounded-md text-gray-500 hover:text-white hover:bg-primary/10 transition-colors"
              @click="showInteractionPanel = false"
            >
              <X :size="16" />
            </button>
          </div>
          <p class="text-sm text-gray-300 whitespace-pre-wrap break-words">{{ interactionData.content }}</p>
          <button class="mt-5 btn-primary text-sm w-full" @click="showInteractionPanel = false">关闭</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>
