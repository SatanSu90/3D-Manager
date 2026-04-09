<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { useRoute, useRouter } from 'vue-router'
import { SceneRenderer } from '@/three/SceneRenderer'
import { getScene } from '@/api/scene'
import { getModelUrl } from '@/api/model'
import { modelLoader } from '@/three/ModelLoader'
import { RotateCcw, Maximize } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const containerRef = ref<HTMLDivElement>()
const loading = ref(true)
const error = ref('')
const sceneName = ref('')
let renderer: SceneRenderer | null = null

onMounted(async () => {
  const id = Number(route.params.sceneId)
  if (!id) {
    error.value = '无效的场景 ID'
    loading.value = false
    return
  }

  try {
    const res = await getScene(id)
    const scene = res.data.data
    sceneName.value = scene.name

    if (!containerRef.value) return
    renderer = new SceneRenderer(containerRef.value)
    renderer.setGradientBackground('#0a0a1a', '#1a1a3e')
    renderer.startRender()

    let sceneData = null
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
            sceneGroup.add(model.clone())
          }
        } catch (e) {
          console.warn('加载模型失败', obj.modelRef, e)
        }
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
        if (obj.transform) {
          light.position.fromArray(obj.transform.position)
        }
        renderer.getScene().add(light)
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
  } catch (e) {
    console.error('加载场景失败:', e)
    error.value = '加载场景失败，请返回重试'
  } finally {
    loading.value = false
  }
})

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

onBeforeUnmount(() => {
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

    <!-- 渲染区域 -->
    <div ref="containerRef" class="flex-1 w-full" />

    <!-- 加载中 -->
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-dark/80 z-10">
      <div class="text-center">
        <div class="w-12 h-12 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mx-auto mb-3" />
        <p class="text-sm text-gray-400">加载场景中...</p>
      </div>
    </div>

    <!-- 错误 -->
    <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-dark/80 z-10">
      <div class="text-center">
        <p class="text-danger mb-3">{{ error }}</p>
        <button class="btn-primary text-sm" @click="router.back()">返回</button>
      </div>
    </div>
  </div>
</template>
