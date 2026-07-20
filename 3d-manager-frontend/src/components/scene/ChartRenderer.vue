<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, shallowRef, computed } from 'vue'
import * as echarts from 'echarts/core'
import {
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  ScatterChart,
  RadarChart,
  FunnelChart,
  TreemapChart,
  HeatmapChart,
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  RadarComponent,
  VisualMapComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ChartConfig, ChartDataPoint } from '@/types/scene'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  ScatterChart,
  RadarChart,
  FunnelChart,
  TreemapChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  RadarComponent,
  VisualMapComponent,
  CanvasRenderer,
])

interface Props {
  chartConfig: ChartConfig
  /** 绑定的指标值(JSON字符串，解析后取数值) */
  indicatorValue?: string
  width?: number
  height?: number
  highlighted?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  indicatorValue: undefined,
  width: 0,
  height: 0,
  highlighted: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
  pointerdown: [event: PointerEvent]
}>()

const chartContainerRef = ref<HTMLDivElement>()
const chartInstance = shallowRef<echarts.ECharts | null>(null)

/** 默认暗色配色（与系统主题一致） */
const DEFAULT_COLOR_SCHEME = ['#38bdf8', '#6366f1', '#34d399', '#fbbf24', '#f472b6', '#a78bfa']

/** 解析指标值并转换为 {name,value} 数组 */
function parseIndicatorData(raw: string | undefined): ChartDataPoint[] {
  if (!raw) return []
  try {
    // 优先尝试 JSON 解析
    const parsed = JSON.parse(raw)
    // 数组形式：直接当作 {name,value}[]
    if (Array.isArray(parsed)) {
      return parsed
        .map((item, idx) => {
          if (typeof item === 'number') {
            return { name: `项${idx + 1}`, value: item }
          }
          if (item && typeof item === 'object') {
            const v = typeof item.value === 'number'
              ? item.value
              : typeof item.value === 'string' ? parseFloat(item.value) : NaN
            return {
              name: typeof item.name === 'string' ? item.name : `项${idx + 1}`,
              value: isNaN(v) ? 0 : v,
            }
          }
          return null
        })
        .filter((x): x is { name: string; value: number } => x !== null)
    }
    // 对象形式：{ key: number }
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed).map(([k, v]) => ({
        name: k,
        value: typeof v === 'number' ? v : parseFloat(String(v)) || 0,
      }))
    }
    // 单个数值
    if (typeof parsed === 'number') {
      return [{ name: '值', value: parsed }]
    }
    return []
  } catch {
    // 非 JSON：尝试当纯数字解析
    const num = parseFloat(raw)
    if (!isNaN(num)) {
      return [{ name: '值', value: num }]
    }
    return []
  }
}

/** 取当前应渲染的数据 */
function getChartData(): ChartDataPoint[] {
  if (props.chartConfig.dataSource === 'indicator') {
    return parseIndicatorData(props.indicatorValue)
  }
  return props.chartConfig.staticData ?? []
}

/** 构建 ECharts option */
function buildOption(): echarts.EChartsCoreOption {
  const cfg = props.chartConfig
  const data = getChartData()
  const colors = cfg.colorScheme?.length ? cfg.colorScheme : DEFAULT_COLOR_SCHEME

  const baseOption: echarts.EChartsCoreOption = {
    backgroundColor: 'transparent',
    color: colors,
    textStyle: { color: '#9ca3af', fontFamily: 'inherit' },
    animation: cfg.animation?.enabled !== false,
    animationDuration: cfg.animation?.duration ?? 800,
    animationEasing: 'cubicOut',
    animationLoop: cfg.animation?.loop === true,
    title: cfg.title
      ? {
          text: cfg.title,
          left: 'center',
          textStyle: { color: '#e5e7eb', fontSize: 13, fontWeight: 500 },
        }
      : undefined,
    tooltip: {
      show: cfg.showTooltip !== false,
      trigger: ['pie', 'scatter', 'funnel', 'treemap', 'heatmap'].includes(cfg.type) ? 'item' : 'axis',
      backgroundColor: 'rgba(17, 24, 39, 0.95)',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      textStyle: { color: '#e5e7eb', fontSize: 12 },
    },
    grid: {
      left: 36,
      right: 16,
      top: cfg.title ? 36 : 20,
      bottom: 28,
      containLabel: true,
    },
  }

  if (cfg.type === 'bar') {
    (baseOption as Record<string, unknown>).xAxis = {
      type: cfg.variant === 'horizontal' ? 'value' : 'category',
      data: data.map((d) => d.name),
      axisLine: { lineStyle: { color: '#374151' } },
      axisLabel: { color: '#6b7280', fontSize: 10 },
      axisTick: { show: false },
    }
    ;(baseOption as Record<string, unknown>).yAxis = {
      type: cfg.variant === 'horizontal' ? 'category' : 'value',
      data: cfg.variant === 'horizontal' ? data.map((d) => d.name) : undefined,
      axisLine: { lineStyle: { color: '#374151' } },
      axisLabel: { color: '#6b7280', fontSize: 10 },
      splitLine: { lineStyle: { color: '#1f2937' } },
    }
    ;(baseOption as Record<string, unknown>).series = [
      {
        type: 'bar',
        data: data.map((d) => cfg.variant === 'horizontal' ? d.value : d.value),
        stack: cfg.variant === 'stacked' ? 'total' : undefined,
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        barWidth: '60%',
      },
    ]
  } else if (cfg.type === 'line') {
    (baseOption as Record<string, unknown>).xAxis = {
      type: 'category',
      data: data.map((d) => d.name),
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#374151' } },
      axisLabel: { color: '#6b7280', fontSize: 10 },
      axisTick: { show: false },
    }
    ;(baseOption as Record<string, unknown>).yAxis = {
      type: 'value',
      axisLine: { lineStyle: { color: '#374151' } },
      axisLabel: { color: '#6b7280', fontSize: 10 },
      splitLine: { lineStyle: { color: '#1f2937' } },
    }
    ;(baseOption as Record<string, unknown>).series = [
      {
        type: 'line',
        data: data.map((d) => d.value),
        smooth: cfg.variant !== 'step' && cfg.smooth !== false,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2 },
        step: cfg.variant === 'step' ? 'middle' : undefined,
        areaStyle: cfg.variant === 'area' || cfg.areaStyle ? { opacity: 0.2 } : undefined,
      },
    ]
  } else if (cfg.type === 'pie') {
    (baseOption as Record<string, unknown>).legend = {
      show: cfg.showLegend !== false,
      orient: 'vertical',
      right: 8,
      top: 'center',
      textStyle: { color: '#9ca3af', fontSize: 10 },
      itemWidth: 8,
      itemHeight: 8,
    }
    ;(baseOption as Record<string, unknown>).series = [
      {
        type: 'pie',
        radius: cfg.variant === 'donut' || cfg.variant === 'rose' ? ['35%', '60%'] : '60%',
        center: ['40%', '55%'],
        data: data.map((d) => ({ name: d.name, value: d.value })),
        label: { show: cfg.showLabel !== false, color: '#9ca3af', fontSize: 10 },
        labelLine: { lineStyle: { color: '#374151' } },
        roseType: cfg.variant === 'rose' ? 'radius' : undefined,
        itemStyle: {
          borderColor: '#0a0a1a',
          borderWidth: 1,
        },
      },
    ]
  } else if (cfg.type === 'scatter') {
    ;(baseOption as Record<string, unknown>).xAxis = { type: 'value', axisLabel: { color: '#6b7280', fontSize: 10 }, splitLine: { lineStyle: { color: '#1f2937' } } }
    ;(baseOption as Record<string, unknown>).yAxis = { type: 'value', axisLabel: { color: '#6b7280', fontSize: 10 }, splitLine: { lineStyle: { color: '#1f2937' } } }
    ;(baseOption as Record<string, unknown>).series = [{
      type: 'scatter',
      symbolSize: (value: unknown[]) => Math.max(8, Math.min(28, Number(value[2] ?? 12))),
      data: data.map((point, index) => [point.x ?? index, point.y ?? point.value, point.value]),
    }]
  } else if (cfg.type === 'radar') {
    ;(baseOption as Record<string, unknown>).radar = {
      indicator: data.map((point) => ({ name: point.name, max: Math.max(100, point.value * 1.25) })),
      axisName: { color: '#9ca3af', fontSize: 10 },
      splitLine: { lineStyle: { color: '#374151' } },
      splitArea: { areaStyle: { color: ['rgba(56, 189, 248, 0.05)', 'rgba(99, 102, 241, 0.08)'] } },
    }
    ;(baseOption as Record<string, unknown>).series = [{ type: 'radar', data: [{ value: data.map((point) => point.value), name: cfg.title || '指标' }] }]
  } else if (cfg.type === 'funnel') {
    ;(baseOption as Record<string, unknown>).series = [{
      type: 'funnel',
      left: '8%',
      width: '84%',
      top: cfg.title ? 40 : 20,
      bottom: 20,
      min: 0,
      max: Math.max(...data.map((point) => point.value), 100),
      sort: 'descending',
      gap: 3,
      label: { color: '#e5e7eb', fontSize: 10 },
      data: data.map((point) => ({ name: point.name, value: point.value })),
    }]
  } else if (cfg.type === 'treemap') {
    ;(baseOption as Record<string, unknown>).series = [{
      type: 'treemap',
      roam: false,
      breadcrumb: { show: false },
      label: { show: cfg.showLabel !== false, color: '#e5e7eb', fontSize: 10 },
      data: data.map((point) => ({ name: point.name, value: point.value })),
    }]
  } else if (cfg.type === 'heatmap') {
    ;(baseOption as Record<string, unknown>).xAxis = { type: 'category', data: data.map((point) => point.name), axisLabel: { color: '#6b7280', fontSize: 10 } }
    ;(baseOption as Record<string, unknown>).yAxis = { type: 'category', data: ['当前值'], axisLabel: { color: '#6b7280', fontSize: 10 } }
    ;(baseOption as Record<string, unknown>).visualMap = { min: 0, max: Math.max(...data.map((point) => point.value), 100), calculable: false, show: false, inRange: { color: ['#172554', '#0ea5e9', '#fbbf24'] } }
    ;(baseOption as Record<string, unknown>).series = [{ type: 'heatmap', data: data.map((point, index) => [index, 0, point.value]), label: { show: cfg.showLabel !== false, color: '#fff', fontSize: 10 } }]
  } else if (cfg.type === 'gauge') {
    const first = data[0]?.value ?? 0
    ;(baseOption as Record<string, unknown>).series = [
      {
        type: 'gauge',
        min: 0,
        max: 100,
        progress: {
          show: true,
          width: 10,
          itemStyle: { color: '#6366f1' },
        },
        axisLine: {
          lineStyle: { width: 10, color: [[1, '#1f2937']] },
        },
        pointer: {
          itemStyle: { color: '#38bdf8' },
        },
        axisTick: { show: false },
        splitLine: {
          length: 8,
          lineStyle: { color: '#374151' },
        },
        axisLabel: { color: '#6b7280', fontSize: 9, distance: 12 },
        detail: {
          valueAnimation: true,
          formatter: '{value}',
          color: '#e5e7eb',
          fontSize: 16,
          offsetCenter: [0, '40%'],
        },
        data: [{ value: first, name: data[0]?.name ?? '' }],
      },
    ]
  }

  return baseOption
}

function renderChart() {
  if (!chartInstance.value) return
  chartInstance.value.setOption(buildOption(), true)
}

function initChart() {
  if (!chartContainerRef.value) return
  chartInstance.value = echarts.init(chartContainerRef.value)
  renderChart()
}

function resizeChart() {
  chartInstance.value?.resize()
}

const containerStyle = computed(() => ({
  width: (props.width || props.chartConfig.width || 300) + 'px',
  height: (props.height || props.chartConfig.height || 200) + 'px',
}))

const chartStyle = computed(() => ({
  ...containerStyle.value,
  background: props.chartConfig.backgroundColor || 'rgba(10, 10, 26, 0.78)',
  border: `${props.chartConfig.borderWidth ?? 1}px solid ${props.chartConfig.borderColor || 'rgba(99, 102, 241, 0.35)'}`,
  borderRadius: `${props.chartConfig.borderRadius ?? 10}px`,
  opacity: props.chartConfig.opacity ?? 1,
}))

const animationClass = computed(() => {
  if (!props.chartConfig.animation?.enabled) return ''
  return `chart-entrance-${props.chartConfig.animation.entrance || 'fadeIn'}`
})

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
})

// 监听配置或数据变化，重新渲染
watch(
  () => [props.chartConfig, props.indicatorValue],
  () => renderChart(),
  { deep: true }
)

// 监听宽高变化，重新 resize
watch(containerStyle, () => {
  resizeChart()
})

</script>

<template>
  <div
    ref="chartContainerRef"
    class="chart-renderer-container"
    :class="[animationClass, { 'chart-highlighted': highlighted }]"
    :style="chartStyle"
    @click="emit('click', $event)"
    @pointerdown="emit('pointerdown', $event)"
  />
</template>

<style scoped>
.chart-renderer-container {
  background: transparent;
  overflow: hidden;
  box-sizing: border-box;
  cursor: pointer;
  transition: box-shadow 180ms ease, border-color 180ms ease;
}

.chart-highlighted {
  box-shadow: 0 0 0 2px #fbbf24, 0 0 28px rgba(251, 191, 36, 0.35);
}

.chart-entrance-fadeIn { animation: chart-fade-in 700ms ease both; }
.chart-entrance-scaleIn { animation: chart-scale-in 700ms ease both; }
.chart-entrance-slideUp { animation: chart-slide-up 700ms ease both; }

@keyframes chart-fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes chart-scale-in { from { opacity: 0; transform: scale(0.82); } to { opacity: 1; transform: scale(1); } }
@keyframes chart-slide-up { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }

@media (prefers-reduced-motion: reduce) {
  .chart-renderer-container { animation: none !important; transition: none; }
}
</style>
