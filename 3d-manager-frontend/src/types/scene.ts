export type SceneVisibility = 'PRIVATE' | 'DEPARTMENT_SHARED' | 'PUBLIC'
export type SceneStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface Scene {
  id: number
  name: string
  sceneData: string
  thumbnailKey: string
  thumbnailUrl: string
  description: string | null
  creatorId: number
  creatorName: string
  categoryId: number | null
  categoryName: string | null
  ownerId: number
  ownerName: string
  visibility: SceneVisibility
  status: SceneStatus
  resolution: string | null
  previewPassword: string | null
  createdAt: string
  updatedAt: string
}

export interface SceneData {
  objects: SceneObject[]
  environment: SceneEnvironment
}

export interface SceneObject {
  id: string
  name: string
  type: 'model' | 'light' | 'camera' | 'primitive' | 'chart'
  visible?: boolean
  modelRef?: number
  primitiveType?: 'cube' | 'sphere' | 'cylinder' | 'plane' | 'cone' | 'torus'
  transform: {
    position: [number, number, number]
    rotation: [number, number, number]
    scale: [number, number, number]
  }
  material?: {
    color: string
    metalness: number
    roughness: number
    map?: string
  }
  light?: {
    type: 'ambient' | 'directional' | 'point' | 'spot'
    color: string
    intensity: number
    distance?: number
    angle?: number
  }
  /** 数据绑定配置列表 */
  dataBindings?: DataBinding[]
  /** 动画配置 */
  animation?: AnimationConfig
  /** 图表配置（type='chart' 时使用） */
  chartConfig?: ChartConfig
  /** 交互事件配置列表 */
  interactions?: InteractionEvent[]
}

/** 交互事件配置：点击对象时触发指定动作 */
export interface InteractionEvent {
  id: string
  /** 触发方式（目前只支持 click） */
  trigger: 'click'
  /** 目标对象ID（'self' 表示自身） */
  targetId: string
  /** 动作类型：高亮 / 切换可见 / 弹窗数据 / 跳转链接 */
  action: 'highlight' | 'toggleVisible' | 'showData' | 'navigate'
  // ===== highlight 参数 =====
  /** 高亮颜色，默认 #ff0000 */
  highlightColor?: string
  /** 高亮持续时间（毫秒），默认 2000 */
  highlightDuration?: number
  // ===== showData 参数 =====
  /** 数据弹窗标题 */
  dataTitle?: string
  /** 数据弹窗内容（文本或指标ID） */
  dataContent?: string
  // ===== navigate 参数 =====
  /** 跳转 URL */
  navigateUrl?: string
}

/** 图表组件配置 */
export interface ChartConfig {
  type: 'bar' | 'line' | 'pie' | 'gauge'
  title?: string
  // 数据来源：从绑定的指标取值，或手动配置静态数据
  dataSource: 'indicator' | 'static'
  indicatorId?: number  // 当dataSource='indicator'时
  // 静态数据（dataSource='static'时使用）
  staticData?: { name: string; value: number }[]
  // 图表样式
  width?: number  // 默认 300
  height?: number // 默认 200
  colorScheme?: string[] // 自定义颜色序列
}

/** 动画配置 */
export interface AnimationConfig {
  enabled: boolean
  type: 'rotate' | 'float' | 'pulse' | 'colorShift'
  // 旋转动画参数
  rotateSpeed?: number        // 旋转速度(度/秒)，默认30
  rotateAxis?: 'x' | 'y' | 'z' // 旋转轴，默认y
  // 浮动动画参数
  floatAmplitude?: number     // 浮动幅度，默认0.5
  floatSpeed?: number         // 浮动速度，默认1
  // 脉冲动画参数
  pulseScale?: number         // 脉冲缩放比例，默认0.1(±10%)
  pulseSpeed?: number         // 脉冲速度，默认2
  // 颜色渐变参数
  colorFrom?: string          // 起始颜色
  colorTo?: string            // 结束颜色
  colorSpeed?: number         // 颜色变化速度，默认1
}

/** 数据绑定配置：将指标(Indicator)绑定到场景对象上 */
export interface DataBinding {
  indicatorId: number       // 绑定的指标ID
  indicatorName?: string    // 指标名称（展示用）
  label?: string            // 显示标签（如"温度"、"压力"）
  displayMode: 'text' | 'gauge' | 'color'  // 显示模式：文本/仪表盘/颜色映射
  position?: 'top' | 'bottom' | 'left' | 'right'  // 标签位置（相对3D对象）
  unit?: string             // 单位（如°C、MPa、rpm）
  // 颜色映射（displayMode='color'时使用）
  colorMapping?: {
    field: string           // 值字段
    ranges: { min: number; max: number; color: string }[]  // 值范围→颜色
  }
}

export interface SceneEnvironment {
  hdrFile?: string
  ambientIntensity: number
  backgroundColor: string
  fogColor?: string
  fogDensity?: number
}
