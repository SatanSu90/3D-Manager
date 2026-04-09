import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { RGBELoader } from 'three/addons/loaders/RGBELoader.js'
import { modelLoader } from './ModelLoader'
import type { SceneData, SceneObject } from '@/types/scene'

export interface RendererOptions {
  antialias?: boolean
  alpha?: boolean
  shadowMap?: boolean
  toneMapping?: THREE.ToneMapping
  toneMappingExposure?: number
}

export class SceneRenderer {
  renderer: THREE.WebGLRenderer
  scene: THREE.Scene
  camera: THREE.PerspectiveCamera
  controls: OrbitControls
  private animationId: number | null = null
  private resizeObserver: ResizeObserver | null = null

  constructor(container: HTMLElement, options: RendererOptions = {}) {
    const {
      antialias = true,
      alpha = true,
      shadowMap = true,
      toneMapping = THREE.ACESFilmicToneMapping,
      toneMappingExposure = 1.2,
    } = options

    this.renderer = new THREE.WebGLRenderer({ antialias, alpha })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setSize(container.clientWidth, container.clientHeight)
    this.renderer.toneMapping = toneMapping
    this.renderer.toneMappingExposure = toneMappingExposure
    this.renderer.outputColorSpace = THREE.SRGBColorSpace

    if (shadowMap) {
      this.renderer.shadowMap.enabled = true
      this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
    }

    container.appendChild(this.renderer.domElement)

    this.scene = new THREE.Scene()
    this.scene.background = new THREE.Color('#1a1a2e')

    this.camera = new THREE.PerspectiveCamera(
      45,
      container.clientWidth / container.clientHeight,
      0.1,
      1000
    )
    this.camera.position.set(5, 5, 5)

    this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.05
    this.controls.minDistance = 0.5
    this.controls.maxDistance = 100
    this.controls.target.set(0, 0, 0)

    this.addDefaultLights()
    this.setupResize(container)
  }

  private addDefaultLights(): void {
    const ambient = new THREE.AmbientLight(0xffffff, 0.6)
    this.scene.add(ambient)

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.0)
    dirLight.position.set(5, 10, 7)
    dirLight.castShadow = true
    dirLight.shadow.mapSize.width = 2048
    dirLight.shadow.mapSize.height = 2048
    dirLight.shadow.camera.near = 0.5
    dirLight.shadow.camera.far = 50
    dirLight.shadow.camera.left = -10
    dirLight.shadow.camera.right = 10
    dirLight.shadow.camera.top = 10
    dirLight.shadow.camera.bottom = -10
    this.scene.add(dirLight)

    const fillLight = new THREE.DirectionalLight(0x6C5CE7, 0.3)
    fillLight.position.set(-5, 3, -5)
    this.scene.add(fillLight)
  }

  async loadHDR(url: string): Promise<void> {
    const rgbeLoader = new RGBELoader()
    const texture = await new Promise<THREE.Texture>((resolve, reject) => {
      rgbeLoader.load(url, resolve, undefined, reject)
    })
    texture.mapping = THREE.EquirectangularReflectionMapping
    this.scene.environment = texture
    this.scene.background = texture
    this.scene.backgroundBlurriness = 0.05
  }

  setGradientBackground(topColor: string, bottomColor: string): void {
    const canvas = document.createElement('canvas')
    canvas.width = 2
    canvas.height = 256
    const ctx = canvas.getContext('2d')!
    const gradient = ctx.createLinearGradient(0, 0, 0, 256)
    gradient.addColorStop(0, topColor)
    gradient.addColorStop(1, bottomColor)
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 2, 256)
    const texture = new THREE.CanvasTexture(canvas)
    texture.mapping = THREE.EquirectangularReflectionMapping
    this.scene.background = texture
  }

  async loadSceneData(data: SceneData): Promise<void> {
    for (const obj of data.objects) {
      if (obj.type === 'model') {
        // 预览模式下通过 /api/models/{id}/download 获取 URL
        // 由于预览组件不直接支持，需要在调用前预先解析好 URL
        // 此方法主要用于非模型对象，模型对象的加载由外部处理
      }
      if (obj.type === 'light') {
        let light: THREE.Light
        switch (obj.light?.type) {
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
          light.position.set(...obj.transform.position)
        }
        this.scene.add(light)
      }
    }
  }

  getScene(): THREE.Scene {
    return this.scene
  }

  addGrid(): THREE.GridHelper {
    const grid = new THREE.GridHelper(20, 20, 0x6C5CE7, 0x2a2a4a)
    grid.material.opacity = 0.3
    grid.material.transparent = true
    this.scene.add(grid)
    return grid
  }

  fitCameraToObject(object: THREE.Object3D, offset = 1.5): void {
    const box = new THREE.Box3().setFromObject(object)
    const center = box.getCenter(new THREE.Vector3())
    const size = box.getSize(new THREE.Vector3())

    const maxDim = Math.max(size.x, size.y, size.z)
    const fov = this.camera.fov * (Math.PI / 180)
    let cameraZ = maxDim / (2 * Math.tan(fov / 2))
    cameraZ *= offset

    this.camera.position.set(center.x + cameraZ * 0.5, center.y + cameraZ * 0.5, center.z + cameraZ)
    this.controls.target.copy(center)
    this.controls.update()
  }

  startRender(): void {
    const animate = () => {
      this.animationId = requestAnimationFrame(animate)
      this.controls.update()
      this.renderer.render(this.scene, this.camera)
    }
    animate()
  }

  stopRender(): void {
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId)
      this.animationId = null
    }
  }

  private setupResize(container: HTMLElement): void {
    this.resizeObserver = new ResizeObserver(() => {
      const width = container.clientWidth
      const height = container.clientHeight
      this.camera.aspect = width / height
      this.camera.updateProjectionMatrix()
      this.renderer.setSize(width, height)
    })
    this.resizeObserver.observe(container)
  }

  takeScreenshot(width = 1920, height = 1080): string {
    const originalSize = new THREE.Vector2()
    this.renderer.getSize(originalSize)
    this.renderer.setSize(width, height)
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
    this.renderer.render(this.scene, this.camera)
    const dataUrl = this.renderer.domElement.toDataURL('image/png')
    this.renderer.setSize(originalSize.x, originalSize.y)
    this.camera.aspect = originalSize.x / originalSize.y
    this.camera.updateProjectionMatrix()
    return dataUrl
  }

  dispose(): void {
    this.stopRender()
    this.resizeObserver?.disconnect()
    this.controls.dispose()
    this.scene.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        mesh.geometry?.dispose()
        if (Array.isArray(mesh.material)) {
          mesh.material.forEach((m) => m.dispose())
        } else {
          mesh.material?.dispose()
        }
      }
    })
    this.renderer.dispose()
    this.renderer.domElement.parentElement?.removeChild(this.renderer.domElement)
  }
}
