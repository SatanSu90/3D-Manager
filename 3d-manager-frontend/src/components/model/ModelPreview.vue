<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'
import { SceneRenderer } from '@/three/SceneRenderer'
import { modelLoader } from '@/three/ModelLoader'
import { RotateCcw, Maximize, Camera } from 'lucide-vue-next'

const props = defineProps<{
  modelUrl: string
  hdrUrl?: string
}>()

const containerRef = ref<HTMLDivElement>()
let renderer: SceneRenderer | null = null
const loading = ref(true)
const progress = ref(0)

onMounted(() => {
  if (!containerRef.value) return
  renderer = new SceneRenderer(containerRef.value)
  
  if (props.hdrUrl) {
    renderer.loadHDR(props.hdrUrl)
  } else {
    renderer.setGradientBackground('#0a0a1a', '#1a1a3e')
  }
  
  renderer.startRender()
  loadModel()
})

onBeforeUnmount(() => {
  renderer?.dispose()
  renderer = null
})

watch(() => props.modelUrl, () => {
  loadModel()
})

async function loadModel() {
  if (!renderer || !props.modelUrl) return
  loading.value = true
  progress.value = 0
  
  // 清除旧模型
  const toRemove: THREE.Object3D[] = []
  renderer.scene.children.forEach((child) => {
    if (child.userData.isModel) toRemove.push(child)
  })
  toRemove.forEach((child) => {
    renderer!.scene.remove(child)
    modelLoader.dispose(child)
  })

  try {
    const model = await modelLoader.loadModel(props.modelUrl, (p) => {
      progress.value = p.percent
    })
    model.userData.isModel = true
    modelLoader.autoCenterAndScale(model, 4)
    renderer.scene.add(model)
    renderer.fitCameraToObject(model, 1.5)
  } catch (e) {
    console.error('模型加载失败:', e)
  } finally {
    loading.value = false
  }
}

function resetCamera() {
  renderer?.fitCameraToObject(
    renderer.scene.children.find((c) => c.userData.isModel) || renderer.scene,
    1.5
  )
}

function toggleFullscreen() {
  if (!containerRef.value) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    containerRef.value.requestFullscreen()
  }
}

function takeScreenshot() {
  if (!renderer) return
  const url = renderer.takeScreenshot()
  const link = document.createElement('a')
  link.href = url
  link.download = 'screenshot.png'
  link.click()
}
</script>

<template>
  <div class="relative w-full h-full bg-dark rounded-xl overflow-hidden">
    <div ref="containerRef" class="w-full h-full" />
    
    <div
      v-if="loading"
      class="absolute inset-0 flex flex-col items-center justify-center bg-dark/80 backdrop-blur-sm"
    >
      <div class="w-12 h-12 border-2 border-primary/30 border-t-primary-light rounded-full animate-spin mb-3" />
      <p class="text-sm text-gray-400">加载中... {{ progress }}%</p>
    </div>

    <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 glass-panel p-1.5">
      <button
        class="p-2 rounded-lg text-gray-400 hover:text-primary-light hover:bg-primary/10 transition-all"
        title="重置视角"
        @click="resetCamera"
      >
        <RotateCcw :size="18" />
      </button>
      <button
        class="p-2 rounded-lg text-gray-400 hover:text-primary-light hover:bg-primary/10 transition-all"
        title="截图"
        @click="takeScreenshot"
      >
        <Camera :size="18" />
      </button>
      <button
        class="p-2 rounded-lg text-gray-400 hover:text-primary-light hover:bg-primary/10 transition-all"
        title="全屏"
        @click="toggleFullscreen"
      >
        <Maximize :size="18" />
      </button>
    </div>
  </div>
</template>
