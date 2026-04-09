import * as THREE from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js'

export class ThumbnailGenerator {
  private renderer: THREE.WebGLRenderer
  private scene: THREE.Scene
  private camera: THREE.PerspectiveCamera
  private readonly width: number
  private readonly height: number

  constructor(width = 512, height = 512) {
    this.width = width
    this.height = height

    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    this.renderer.setSize(width, height)
    this.renderer.setPixelRatio(1)
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping
    this.renderer.toneMappingExposure = 1.0
    this.renderer.outputColorSpace = THREE.SRGBColorSpace

    this.scene = new THREE.Scene()
    this.scene.background = new THREE.Color('#1a1a2e')

    const ambient = new THREE.AmbientLight(0xffffff, 0.8)
    this.scene.add(ambient)

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.0)
    dirLight.position.set(5, 8, 5)
    this.scene.add(dirLight)

    const fillLight = new THREE.DirectionalLight(0x6C5CE7, 0.3)
    fillLight.position.set(-3, 2, -3)
    this.scene.add(fillLight)

    this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100)
  }

  async generateFromFile(file: File): Promise<Blob> {
    const url = URL.createObjectURL(file)
    try {
      const dracoLoader = new DRACOLoader()
      dracoLoader.setDecoderPath('https://www.gstatic.com/draco/versioned/decoders/1.5.6/')
      const loader = new GLTFLoader()
      loader.setDRACOLoader(dracoLoader)
      const gltf = await new Promise<{ scene: THREE.Group }>((resolve, reject) => {
        loader.load(url, resolve, undefined, reject)
      })

      const model = gltf.scene
      this.scene.add(model)

      this.fitModelToCamera(model)
      this.renderer.render(this.scene, this.camera)

      const blob = await new Promise<Blob>((resolve) => {
        this.renderer.domElement.toBlob(
          (b) => resolve(b!),
          'image/jpeg',
          0.85
        )
      })

      this.scene.remove(model)
      dracoLoader.dispose()
      model.traverse((child) => {
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

      return blob
    } finally {
      URL.revokeObjectURL(url)
    }
  }

  private fitModelToCamera(model: THREE.Object3D): void {
    const box = new THREE.Box3().setFromObject(model)
    const center = box.getCenter(new THREE.Vector3())
    const size = box.getSize(new THREE.Vector3())

    model.position.sub(center)

    const maxDim = Math.max(size.x, size.y, size.z)
    const fov = this.camera.fov * (Math.PI / 180)
    const cameraZ = maxDim / (2 * Math.tan(fov / 2)) * 1.8

    this.camera.position.set(cameraZ * 0.6, cameraZ * 0.4, cameraZ * 0.8)
    this.camera.lookAt(0, 0, 0)
  }

  dispose(): void {
    this.renderer.dispose()
  }
}

export const thumbnailGenerator = new ThumbnailGenerator()
