import * as THREE from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js'

export interface LoadProgress {
  loaded: number
  total: number
  percent: number
}

export class ModelLoader {
  private gltfLoader: GLTFLoader
  private dracoLoader: DRACOLoader

  constructor() {
    this.gltfLoader = new GLTFLoader()
    this.dracoLoader = new DRACOLoader()
    this.dracoLoader.setDecoderPath('https://www.gstatic.com/draco/versioned/decoders/1.5.6/')
    this.gltfLoader.setDRACOLoader(this.dracoLoader)
  }

  async loadModel(
    url: string,
    onProgress?: (progress: LoadProgress) => void
  ): Promise<THREE.Group> {
    return new Promise((resolve, reject) => {
      this.gltfLoader.load(
        url,
        (gltf) => {
          const model = gltf.scene
          model.traverse((child) => {
            if ((child as THREE.Mesh).isMesh) {
              child.castShadow = true
              child.receiveShadow = true
            }
          })
          resolve(model)
        },
        (progress) => {
          if (onProgress && progress.total > 0) {
            onProgress({
              loaded: progress.loaded,
              total: progress.total,
              percent: Math.round((progress.loaded / progress.total) * 100),
            })
          }
        },
        (error) => {
          console.error('模型加载失败:', error)
          reject(error)
        }
      )
    })
  }

  autoCenterAndScale(model: THREE.Object3D, targetSize = 5): void {
    const box = new THREE.Box3().setFromObject(model)
    const center = box.getCenter(new THREE.Vector3())
    const size = box.getSize(new THREE.Vector3())

    model.position.sub(center)

    const maxDim = Math.max(size.x, size.y, size.z)
    if (maxDim > 0) {
      const scale = targetSize / maxDim
      model.scale.multiplyScalar(scale)
    }

    const newBox = new THREE.Box3().setFromObject(model)
    const newCenter = newBox.getCenter(new THREE.Vector3())
    model.position.y -= newCenter.y - newBox.min.y
  }

  dispose(model: THREE.Object3D): void {
    model.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        const mesh = child as THREE.Mesh
        mesh.geometry?.dispose()
        if (Array.isArray(mesh.material)) {
          mesh.material.forEach((m) => this.disposeMaterial(m))
        } else {
          this.disposeMaterial(mesh.material)
        }
      }
    })
  }

  private disposeMaterial(material: THREE.Material): void {
    material.dispose()
    const mat = material as Record<string, unknown>
    for (const key of Object.keys(mat)) {
      const value = mat[key]
      if (value instanceof THREE.Texture) {
        value.dispose()
      }
    }
  }
}

export const modelLoader = new ModelLoader()
