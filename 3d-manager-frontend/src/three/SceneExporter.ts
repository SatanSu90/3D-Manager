import * as THREE from 'three'
import { GLTFExporter } from 'three/addons/exporters/GLTFExporter.js'

export class SceneExporter {
  static async exportToGLB(scene: THREE.Scene): Promise<Blob> {
    const exporter = new GLTFExporter()
    return new Promise((resolve, reject) => {
      exporter.parse(
        scene,
        (result) => {
          if (result instanceof ArrayBuffer) {
            resolve(new Blob([result], { type: 'application/octet-stream' }))
          } else {
            const json = JSON.stringify(result, null, 2)
            resolve(new Blob([json], { type: 'application/json' }))
          }
        },
        (error) => {
          console.error('导出失败:', error)
          reject(error)
        },
        { binary: true }
      )
    })
  }

  static downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
  }
}
