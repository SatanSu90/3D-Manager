import * as THREE from 'three'
import { TransformControls } from 'three/addons/controls/TransformControls.js'
import { SceneRenderer } from './SceneRenderer'
import type { SceneObject } from '@/types/scene'
import { modelLoader } from './ModelLoader'

export class EditorEngine {
  private renderer: SceneRenderer
  private transformControls: TransformControls
  private selectedObject: THREE.Object3D | null = null
  private objectMap = new Map<string, THREE.Object3D>()
  private onObjectSelected: ((id: string | null) => void) | null = null
  private onTransformChanged: ((id: string, transform: SceneObject['transform']) => void) | null = null
  private raycaster = new THREE.Raycaster()
  private mouse = new THREE.Vector2()
  private isDragging = false
  private wasDragging = false

  constructor(container: HTMLElement) {
    this.renderer = new SceneRenderer(container, { shadowMap: true })
    this.renderer.addGrid()

    this.transformControls = new TransformControls(
      this.renderer.camera,
      this.renderer.renderer.domElement
    )
    this.transformControls.addEventListener('dragging-changed', (event) => {
      this.isDragging = event.value
      if (event.value) {
        this.wasDragging = true
      }
      this.renderer.controls.enabled = !event.value
    })
    this.transformControls.addEventListener('objectChange', () => {
      if (this.selectedObject) {
        const id = this.selectedObject.userData.sceneId
        if (id && this.onTransformChanged) {
          this.onTransformChanged(id, {
            position: this.selectedObject.position.toArray() as [number, number, number],
            rotation: [
              THREE.MathUtils.radToDeg(this.selectedObject.rotation.x),
              THREE.MathUtils.radToDeg(this.selectedObject.rotation.y),
              THREE.MathUtils.radToDeg(this.selectedObject.rotation.z),
            ],
            scale: this.selectedObject.scale.toArray() as [number, number, number],
          })
        }
      }
    })
    this.renderer.scene.add(this.transformControls.getHelper())

    this.setupClickSelection()
    this.renderer.startRender()
  }

  private setupClickSelection(): void {
    this.renderer.renderer.domElement.addEventListener('click', (event) => {
      // 拖拽 TransformControls 结束后触发的 click 需要忽略
      if (this.wasDragging) {
        this.wasDragging = false
        return
      }

      const rect = this.renderer.renderer.domElement.getBoundingClientRect()
      this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
      this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

      this.raycaster.setFromCamera(this.mouse, this.renderer.camera)

      const meshes: THREE.Mesh[] = []
      this.objectMap.forEach((obj) => {
        // 跳过隐藏的对象
        if (!obj.visible) return
        obj.traverse((child) => {
          if ((child as THREE.Mesh).isMesh) meshes.push(child as THREE.Mesh)
        })
      })

      const intersects = this.raycaster.intersectObjects(meshes, false)
      if (intersects.length > 0) {
        let obj = intersects[0].object
        while (obj.parent && !obj.userData.sceneId) {
          obj = obj.parent
        }
        if (obj.userData.sceneId) {
          this.selectObjectById(obj.userData.sceneId)
        }
      } else {
        this.deselectAll()
      }
    })
  }

  async addModel(id: string, url: string, autoCenter = false): Promise<THREE.Object3D> {
    const model = await modelLoader.loadModel(url)
    model.userData.sceneId = id
    this.objectMap.set(id, model)
    this.renderer.scene.add(model)
    if (autoCenter) {
      modelLoader.autoCenterAndScale(model)
    }
    return model
  }

  addLight(id: string, type: SceneObject['light']): THREE.Light {
    let light: THREE.Light
    switch (type?.type) {
      case 'directional':
        light = new THREE.DirectionalLight(type.color, type.intensity)
        light.position.set(5, 8, 5)
        break
      case 'point':
        light = new THREE.PointLight(type.color, type.intensity, type.distance || 20)
        light.position.set(0, 5, 0)
        break
      case 'spot':
        light = new THREE.SpotLight(type.color, type.intensity, type.distance || 20, type.angle || 0.5)
        light.position.set(0, 8, 0)
        break
      default:
        light = new THREE.AmbientLight(type?.color || 0xffffff, type?.intensity || 0.5)
    }
    light.userData.sceneId = id
    this.objectMap.set(id, light)
    this.renderer.scene.add(light)
    return light
  }

  selectObjectById(id: string): void {
    const obj = this.objectMap.get(id)
    if (obj) {
      // 隐藏的对象不选中
      if (!obj.visible) return
      this.selectedObject = obj
      this.transformControls.attach(obj)
      this.onObjectSelected?.(id)
    }
  }

  deselectAll(): void {
    this.selectedObject = null
    this.transformControls.detach()
    this.onObjectSelected?.(null)
  }

  removeObject(id: string): void {
    const obj = this.objectMap.get(id)
    if (obj) {
      if (this.selectedObject === obj) {
        this.transformControls.detach()
        this.selectedObject = null
      }
      this.renderer.scene.remove(obj)
      modelLoader.dispose(obj)
      this.objectMap.delete(id)
    }
  }

  setTransformMode(mode: 'translate' | 'rotate' | 'scale'): void {
    this.transformControls.setMode(mode)
  }

  updateObjectMaterial(id: string, props: { color?: string; metalness?: number; roughness?: number }): void {
    const obj = this.objectMap.get(id)
    if (!obj) return
    obj.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        const material = mesh.material as THREE.MeshStandardMaterial
        if (material?.isMeshStandardMaterial) {
          if (props.color) material.color.set(props.color)
          if (props.metalness !== undefined) material.metalness = props.metalness
          if (props.roughness !== undefined) material.roughness = props.roughness
        }
      }
    })
  }

  setObjectVisible(id: string, visible: boolean): void {
    const obj = this.objectMap.get(id)
    if (obj) {
      obj.visible = visible
    }
  }

  fitCameraToScene(): void {
    const objects = Array.from(this.objectMap.values())
    if (objects.length > 0) {
      const group = new THREE.Group()
      objects.forEach((o) => group.add(o.clone()))
      this.renderer.fitCameraToObject(group, 2)
      group.traverse((child) => {
        if ((child as THREE.Mesh).isMesh) {
          ;(child as THREE.Mesh).geometry?.dispose()
        }
      })
    }
  }

  setCallbacks(
    onSelected: (id: string | null) => void,
    onTransform: (id: string, transform: SceneObject['transform']) => void
  ): void {
    this.onObjectSelected = onSelected
    this.onTransformChanged = onTransform
  }

  takeScreenshot(): string {
    return this.renderer.takeScreenshot()
  }

  getScene(): THREE.Scene {
    return this.renderer.scene
  }

  getCamera(): THREE.PerspectiveCamera {
    return this.renderer.camera
  }

  getRenderer(): THREE.WebGLRenderer {
    return this.renderer.renderer
  }

  getControls(): import('three').OrbitControls {
    return this.renderer.controls
  }

  dispose(): void {
    this.objectMap.forEach((obj) => modelLoader.dispose(obj))
    this.objectMap.clear()
    this.transformControls.dispose()
    this.renderer.dispose()
  }
}
