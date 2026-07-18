import * as THREE from 'three'
import { TransformControls } from 'three/addons/controls/TransformControls.js'
import { SceneRenderer } from './SceneRenderer'
import type { SceneObject, AnimationConfig } from '@/types/scene'
import { modelLoader } from './ModelLoader'

export class EditorEngine {
  private renderer: SceneRenderer
  private transformControls: TransformControls
  private selectedObject: THREE.Object3D | null = null
  private objectMap = new Map<string, THREE.Object3D>()
  private onObjectSelected: ((id: string | null) => void) | null = null
  private onTransformChanged: ((id: string, transform: SceneObject['transform']) => void) | null = null
  private onGetAnimation: ((id: string) => AnimationConfig | null | undefined) | null = null
  private raycaster = new THREE.Raycaster()
  private mouse = new THREE.Vector2()
  private isDragging = false
  private wasDragging = false
  private animationClock = new THREE.Clock()
  private originalTransforms = new Map<string, { position: THREE.Vector3, scale: THREE.Vector3, color: THREE.Color }>()

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
      } else {
        // 拖拽结束：刷新选中对象的原始变换缓存，使动画基于新位置继续
        if (this.selectedObject) {
          const id = this.selectedObject.userData.sceneId
          if (id) this.refreshOriginalTransform(id)
        }
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
    // 注册每帧动画更新回调
    this.renderer.setAnimationCallback(() => this.updateAnimations())
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

  /** 添加基础几何体 */
  addPrimitive(id: string, primitiveType: 'cube' | 'sphere' | 'cylinder' | 'plane' | 'cone' | 'torus'): THREE.Object3D {
    let geometry: THREE.BufferGeometry
    switch (primitiveType) {
      case 'cube':
        geometry = new THREE.BoxGeometry(1, 1, 1)
        break
      case 'sphere':
        geometry = new THREE.SphereGeometry(0.5, 32, 32)
        break
      case 'cylinder':
        geometry = new THREE.CylinderGeometry(0.5, 0.5, 1, 32)
        break
      case 'plane':
        geometry = new THREE.PlaneGeometry(2, 2)
        break
      case 'cone':
        geometry = new THREE.ConeGeometry(0.5, 1, 32)
        break
      case 'torus':
        geometry = new THREE.TorusGeometry(0.5, 0.2, 16, 32)
        break
    }
    const material = new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0, roughness: 0.5 })
    const mesh = new THREE.Mesh(geometry, material)
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.userData.sceneId = id
    this.objectMap.set(id, mesh)
    this.renderer.scene.add(mesh)
    return mesh
  }

  /** 复制对象 */
  duplicateObject(sourceId: string, newId: string): THREE.Object3D | null {
    const source = this.objectMap.get(sourceId)
    if (!source) return null
    const clone = source.clone()
    clone.userData.sceneId = newId
    // 偏移一点位置避免完全重叠
    clone.position.x += 1
    clone.position.z += 1
    // 克隆材质避免共享引用
    clone.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        if (mesh.material) {
          mesh.material = (mesh.material as THREE.Material).clone()
        }
      }
    })
    this.objectMap.set(newId, clone)
    this.renderer.scene.add(clone)
    return clone
  }

  /** 直接设置对象变换 */
  setTransform(id: string, position: [number, number, number], rotation: [number, number, number], scale: [number, number, number]): void {
    const obj = this.objectMap.get(id)
    if (!obj) return
    obj.position.set(position[0], position[1], position[2])
    obj.rotation.set(
      THREE.MathUtils.degToRad(rotation[0]),
      THREE.MathUtils.degToRad(rotation[1]),
      THREE.MathUtils.degToRad(rotation[2])
    )
    obj.scale.set(scale[0], scale[1], scale[2])
    // 同步刷新动画的原始变换缓存，避免动画将对象重置回旧位置
    this.refreshOriginalTransform(id)
  }

  /** 设置背景色 */
  setBackgroundColor(color: string): void {
    this.renderer.setBackgroundColor(color)
  }

  /** 设置雾效 */
  setFog(color: string, density: number): void {
    this.renderer.setFog(color, density)
  }

  /** 清除雾效 */
  clearFog(): void {
    this.renderer.clearFog()
  }

  /** 获取场景统计信息 */
  getSceneStats(): { triangles: number; vertices: number; objects: number } {
    let triangles = 0
    let vertices = 0
    this.objectMap.forEach((obj) => {
      obj.traverse((child) => {
        if ((child as THREE.Mesh).isMesh) {
          const mesh = child as THREE.Mesh
          const geo = mesh.geometry
          if (geo) {
            if (geo.index) {
              triangles += geo.index.count / 3
            } else if (geo.attributes.position) {
              triangles += geo.attributes.position.count / 3
            }
            vertices += geo.attributes.position?.count || 0
          }
        }
      })
    })
    return { triangles: Math.floor(triangles), vertices, objects: this.objectMap.size }
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
      this.originalTransforms.delete(id)
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

  /** 设置动画配置查询回调：传入对象ID，返回该对象的动画配置（或 null/undefined 表示无动画） */
  setAnimationCallback(callback: (id: string) => AnimationConfig | null | undefined): void {
    this.onGetAnimation = callback
  }

  /** 刷新某个对象的原始变换缓存（用户手动移动/缩放后调用） */
  private refreshOriginalTransform(id: string): void {
    const obj = this.objectMap.get(id)
    if (!obj) return
    const cached = this.originalTransforms.get(id)
    if (cached) {
      cached.position.copy(obj.position)
      cached.scale.copy(obj.scale)
    }
  }

  /** 更新所有对象的动画（每帧调用） */
  private updateAnimations(): void {
    if (!this.onGetAnimation) return
    const delta = this.animationClock.getDelta()
    const elapsed = this.animationClock.getElapsedTime()

    this.objectMap.forEach((obj, id) => {
      const animConfig = this.onGetAnimation ? this.onGetAnimation(id) : null
      if (!animConfig || !animConfig.enabled) {
        // 关闭动画时恢复原始变换
        if (this.originalTransforms.has(id)) {
          const original = this.originalTransforms.get(id)!
          obj.position.copy(original.position)
          obj.scale.copy(original.scale)
          this.setObjectColor(obj, original.color)
          this.originalTransforms.delete(id)
        }
        return
      }

      // 拖拽中：跳过当前选中对象的动画，避免与用户操作冲突
      if (this.isDragging && this.selectedObject === obj) return

      // 首次启用动画时保存原始变换
      if (!this.originalTransforms.has(id)) {
        this.originalTransforms.set(id, {
          position: obj.position.clone(),
          scale: obj.scale.clone(),
          color: this.getObjectColor(obj),
        })
      }
      const original = this.originalTransforms.get(id)!

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
          this.setObjectColor(obj, currentColor)
          break
        }
      }
    })
  }

  /** 获取对象的主颜色（取第一个 MeshStandardMaterial） */
  private getObjectColor(obj: THREE.Object3D): THREE.Color {
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
  private setObjectColor(obj: THREE.Object3D, color: THREE.Color): void {
    obj.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        const mat = (child as THREE.Mesh).material as THREE.MeshStandardMaterial
        if (mat?.isMeshStandardMaterial) {
          mat.color.copy(color)
        }
      }
    })
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
