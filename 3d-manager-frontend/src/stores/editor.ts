import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SceneObject, SceneEnvironment, DataBinding, AnimationConfig, InteractionEvent } from '@/types/scene'

export interface EditorState {
  objects: SceneObject[]
  selectedObjectId: string | null
  environment: SceneEnvironment
  history: string[]
  historyIndex: number
}

export const useEditorStore = defineStore('editor', () => {
  const objects = ref<SceneObject[]>([])
  const selectedObjectId = ref<string | null>(null)
  const environment = ref<SceneEnvironment>({
    hdrFile: undefined,
    ambientIntensity: 0.5,
    backgroundColor: '#1a1a2e',
  })
  const history = ref<string[]>([])
  const historyIndex = ref(-1)
  const transformMode = ref<'translate' | 'rotate' | 'scale'>('translate')

  const selectedObject = ref<SceneObject | null>(null)

  function selectObject(id: string | null) {
    selectedObjectId.value = id
    selectedObject.value = objects.value.find((o) => o.id === id) || null
  }

  function addObject(obj: SceneObject) {
    objects.value.push(obj)
    pushHistory()
  }

  function removeObject(id: string) {
    objects.value = objects.value.filter((o) => o.id !== id)
    if (selectedObjectId.value === id) {
      selectedObjectId.value = null
      selectedObject.value = null
    }
    pushHistory()
  }

  function updateObject(id: string, updates: Partial<SceneObject>) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx] = { ...objects.value[idx], ...updates }
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  function updateTransform(id: string, transform: Partial<SceneObject['transform']>) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].transform = { ...objects.value[idx].transform, ...transform }
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  function updateMaterial(id: string, material: Partial<SceneObject['material']>) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0 && objects.value[idx].material) {
      objects.value[idx].material = { ...objects.value[idx].material!, ...material }
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  function updateVisible(id: string, visible: boolean) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].visible = visible
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  /** 批量更新某个对象的数据绑定配置 */
  function updateDataBindings(id: string, bindings: DataBinding[]) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].dataBindings = bindings
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  /** 更新某个对象的动画配置 */
  function updateAnimation(id: string, animation: AnimationConfig) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].animation = animation
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  /** 批量更新某个对象的交互事件配置 */
  function updateInteractions(id: string, interactions: InteractionEvent[]) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].interactions = interactions
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  function renameObject(id: string, name: string) {
    const idx = objects.value.findIndex((o) => o.id === id)
    if (idx >= 0) {
      objects.value[idx].name = name
      if (selectedObjectId.value === id) {
        selectedObject.value = objects.value[idx]
      }
      pushHistory()
    }
  }

  function moveObject(fromIndex: number, toIndex: number) {
    if (fromIndex === toIndex) return
    const obj = objects.value.splice(fromIndex, 1)[0]
    objects.value.splice(toIndex, 0, obj)
    pushHistory()
  }

  function duplicateObject(id: string): SceneObject | null {
    const source = objects.value.find((o) => o.id === id)
    if (!source) return null
    const newId = `${source.type}_${Date.now()}`
    const cloned: SceneObject = {
      ...JSON.parse(JSON.stringify(source)),
      id: newId,
      name: `${source.name}_副本`,
      transform: {
        position: [source.transform.position[0] + 1, source.transform.position[1], source.transform.position[2] + 1],
        rotation: [...source.transform.rotation] as [number, number, number],
        scale: [...source.transform.scale] as [number, number, number],
      },
    }
    objects.value.push(cloned)
    pushHistory()
    return cloned
  }

  function updateEnvironment(updates: Partial<SceneEnvironment>) {
    environment.value = { ...environment.value, ...updates }
  }

  function setTransformMode(mode: 'translate' | 'rotate' | 'scale') {
    transformMode.value = mode
  }

  function pushHistory() {
    const snapshot = JSON.stringify(objects.value)
    history.value = history.value.slice(0, historyIndex.value + 1)
    history.value.push(snapshot)
    historyIndex.value = history.value.length - 1
  }

  function undo() {
    if (historyIndex.value > 0) {
      historyIndex.value--
      objects.value = JSON.parse(history.value[historyIndex.value])
      syncSelectedObject()
    }
  }

  function updateChartPosition(id: string, position: { x: number; y: number; zIndex?: number }) {
    const idx = objects.value.findIndex((o) => o.id === id)
    const object = objects.value[idx]
    if (idx < 0 || object?.type !== 'chart' || !object.chartConfig) return
    object.chartConfig = { ...object.chartConfig, position }
    if (selectedObjectId.value === id) selectedObject.value = object
  }

  function updateMapPosition(id: string, position: { x: number; y: number; zIndex?: number }) {
    const idx = objects.value.findIndex((o) => o.id === id)
    const object = objects.value[idx]
    if (idx < 0 || object?.type !== 'gis' || !object.mapConfig) return
    object.mapConfig = { ...object.mapConfig, position }
    if (selectedObjectId.value === id) selectedObject.value = object
  }

  function updateChartSize(id: string, width: number, height: number) {
    const object = objects.value.find((item) => item.id === id)
    if (!object || object.type !== 'chart' || !object.chartConfig) return
    object.chartConfig = { ...object.chartConfig, width, height }
    if (selectedObjectId.value === id) selectedObject.value = object
  }

  function updateMapSize(id: string, width: number, height: number) {
    const object = objects.value.find((item) => item.id === id)
    if (!object || object.type !== 'gis' || !object.mapConfig) return
    object.mapConfig = { ...object.mapConfig, width, height }
    if (selectedObjectId.value === id) selectedObject.value = object
  }

  function redo() {
    if (historyIndex.value < history.value.length - 1) {
      historyIndex.value++
      objects.value = JSON.parse(history.value[historyIndex.value])
      syncSelectedObject()
    }
  }

  function syncSelectedObject() {
    if (!selectedObjectId.value) {
      selectedObject.value = null
      return
    }
    const next = objects.value.find((object) => object.id === selectedObjectId.value)
    if (next) {
      selectedObject.value = next
    } else {
      selectedObjectId.value = null
      selectedObject.value = null
    }
  }

  function clearScene() {
    objects.value = []
    selectedObjectId.value = null
    selectedObject.value = null
    history.value = []
    historyIndex.value = -1
  }

  function loadScene(sceneObjects: SceneObject[], env: SceneEnvironment) {
    objects.value = sceneObjects
    environment.value = env
    selectedObjectId.value = null
    selectedObject.value = null
    history.value = [JSON.stringify(sceneObjects)]
    historyIndex.value = 0
  }

  return {
    objects,
    selectedObjectId,
    selectedObject,
    environment,
    transformMode,
    history,
    historyIndex,
    selectObject,
    addObject,
    removeObject,
    updateObject,
    updateChartPosition,
    updateMapPosition,
    updateChartSize,
    updateMapSize,
    updateTransform,
    updateMaterial,
    updateVisible,
    updateDataBindings,
    updateAnimation,
    updateInteractions,
    renameObject,
    moveObject,
    duplicateObject,
    updateEnvironment,
    setTransformMode,
    pushHistory,
    undo,
    redo,
    clearScene,
    loadScene,
  }
})
