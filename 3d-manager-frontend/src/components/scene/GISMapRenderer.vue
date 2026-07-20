<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import type { MapConfig } from '@/types/scene'

const props = withDefaults(defineProps<{
  mapConfig: MapConfig
  highlighted?: boolean
}>(), {
  highlighted: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
  pointerdown: [event: PointerEvent]
  'update-config': [updates: Partial<MapConfig>]
}>()

const panOffset = ref({ x: 0, y: 0 })
const viewDrag = ref<{ startX: number; startY: number; offsetX: number; offsetY: number } | null>(null)

const mapStyle = computed(() => ({
  width: `${props.mapConfig.width ?? 520}px`,
  height: `${props.mapConfig.height ?? 320}px`,
  background: props.mapConfig.backgroundColor || '#0b1d2a',
}))

const mapLayerStyle = computed(() => ({
  transform: `translate(${panOffset.value.x}px, ${panOffset.value.y}px) rotate(${props.mapConfig.bearing ?? 0}deg) rotateX(${props.mapConfig.mode === '3d' ? 48 : 0}deg) scale(${props.mapConfig.zoom ?? 1})`,
}))

function changeZoom(delta: number) {
  const zoom = Math.min(4, Math.max(0.5, (props.mapConfig.zoom ?? 1) + delta))
  emit('update-config', { zoom: Number(zoom.toFixed(2)) })
}

function changeBearing(delta: number) {
  const bearing = ((props.mapConfig.bearing ?? 0) + delta + 360) % 360
  emit('update-config', { bearing })
}

function handleWheel(event: WheelEvent) {
  changeZoom(event.deltaY > 0 ? -0.1 : 0.1)
}

function handlePointerDown(event: PointerEvent) {
  if (event.altKey || event.button === 1) {
    viewDrag.value = {
      startX: event.clientX,
      startY: event.clientY,
      offsetX: panOffset.value.x,
      offsetY: panOffset.value.y,
    }
    window.addEventListener('pointermove', moveView)
    window.addEventListener('pointerup', endView, { once: true })
    return
  }
  emit('pointerdown', event)
}

function moveView(event: PointerEvent) {
  if (!viewDrag.value) return
  panOffset.value = {
    x: viewDrag.value.offsetX + event.clientX - viewDrag.value.startX,
    y: viewDrag.value.offsetY + event.clientY - viewDrag.value.startY,
  }
}

function endView() {
  viewDrag.value = null
  window.removeEventListener('pointermove', moveView)
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', moveView)
})
</script>

<template>
  <div
    class="gis-map-renderer"
    :class="[
      { 'gis-map-highlighted': highlighted },
      `gis-map-mode-${mapConfig.mode || 'gis'}`,
    ]"
    :style="mapStyle"
    @click="emit('click', $event)"
    @pointerdown="handlePointerDown"
    @wheel.prevent="handleWheel"
  >
    <div class="gis-map-layer" :style="mapLayerStyle">
      <div v-if="mapConfig.showGrid !== false" class="gis-map-grid" />
      <div v-if="mapConfig.showRoads !== false" class="gis-map-road gis-map-road-a" />
      <div v-if="mapConfig.showRoads !== false" class="gis-map-road gis-map-road-b" />
    </div>
    <div class="gis-map-header">
      <span>{{ mapConfig.title || (mapConfig.mode === '3d' ? '三维地图' : 'GIS 地图') }}</span>
      <span class="gis-map-coord">{{ mapConfig.center?.[0] ?? 31.2304 }}, {{ mapConfig.center?.[1] ?? 121.4737 }}</span>
    </div>
    <div v-if="mapConfig.showObjects !== false" class="gis-map-legend">场景对象图层</div>
    <div class="gis-map-controls">
      <button title="放大" @click.stop="changeZoom(0.25)">+</button>
      <button title="缩小" @click.stop="changeZoom(-0.25)">−</button>
      <button title="逆时针旋转" @click.stop="changeBearing(-15)">↶</button>
      <button title="顺时针旋转" @click.stop="changeBearing(15)">↷</button>
    </div>
    <div class="gis-map-scale">比例尺 500 m · Alt+拖拽平移</div>
  </div>
</template>

<style scoped>
.gis-map-renderer { position: relative; overflow: hidden; box-sizing: border-box; border: 1px solid rgba(56, 189, 248, 0.35); border-radius: 12px; cursor: grab; box-shadow: 0 12px 30px rgba(2, 8, 23, 0.3); }
.gis-map-renderer:active { cursor: grabbing; }
.gis-map-highlighted { box-shadow: 0 0 0 2px #fbbf24, 0 0 28px rgba(251, 191, 36, 0.3); }
.gis-map-layer { position: absolute; inset: -25%; transform-origin: center; transition: transform 160ms ease; }
.gis-map-mode-3d { perspective: 900px; }
.gis-map-mode-3d .gis-map-layer { transform-origin: center 65%; }
.gis-map-mode-3d .gis-map-road { box-shadow: 0 10px 14px rgba(2, 8, 23, 0.35); }
.gis-map-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(56, 189, 248, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(56, 189, 248, 0.08) 1px, transparent 1px); background-size: 38px 38px; transform: rotate(-8deg) scale(1.15); }
.gis-map-road { position: absolute; height: 18px; background: rgba(148, 163, 184, 0.26); border-top: 1px solid rgba(226, 232, 240, 0.25); border-bottom: 1px solid rgba(226, 232, 240, 0.25); }
.gis-map-road-a { width: 120%; left: -10%; top: 42%; transform: rotate(-18deg); }
.gis-map-road-b { width: 110%; left: -5%; top: 65%; transform: rotate(24deg); }
.gis-map-header, .gis-map-legend, .gis-map-scale { position: absolute; z-index: 2; padding: 5px 8px; border: 1px solid rgba(56, 189, 248, 0.25); border-radius: 6px; background: rgba(15, 23, 42, 0.84); color: #bae6fd; font-size: 10px; backdrop-filter: blur(10px); }
.gis-map-header { top: 10px; left: 10px; right: 10px; display: flex; justify-content: space-between; }
.gis-map-coord { color: #94a3b8; }
.gis-map-legend { left: 10px; bottom: 10px; color: #cbd5e1; }
.gis-map-scale { right: 10px; bottom: 10px; color: #94a3b8; }
.gis-map-controls { position: absolute; z-index: 3; right: 10px; top: 42px; display: flex; gap: 3px; }
.gis-map-controls button { width: 24px; height: 24px; border: 1px solid rgba(56, 189, 248, 0.25); border-radius: 5px; background: rgba(15, 23, 42, 0.84); color: #bae6fd; font-size: 14px; }
.gis-map-controls button:hover { background: rgba(56, 189, 248, 0.2); }
</style>
