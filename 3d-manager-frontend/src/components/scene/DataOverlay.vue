<script setup lang="ts">
import { computed } from 'vue'
import { RefreshCw, Activity } from 'lucide-vue-next'
import type { DataBinding } from '@/types/scene'

/** 指标值的运行时数据 */
export interface IndicatorValue {
  name: string
  value: string
  valueType: string
}

interface Props {
  /** 当前场景中所有的数据绑定（已附带来源对象信息） */
  bindings: Array<DataBinding & { objectName?: string }>
  /** 指标值映射：indicatorId -> IndicatorValue */
  indicatorValues: Map<number, IndicatorValue>
  /** 是否正在刷新 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

/** 是否有任意绑定 */
const hasBindings = computed(() => props.bindings.length > 0)

/** 格式化显示值：优先取指标值，找不到则显示 -- */
function formatValue(binding: DataBinding): string {
  const v = props.indicatorValues.get(binding.indicatorId)
  if (!v) return '--'
  return v.value
}

/** 仪表盘样式：根据数值在 0~max 中占比填充（max 默认 100） */
function gaugeStyle(binding: DataBinding): Record<string, string> {
  const raw = props.indicatorValues.get(binding.indicatorId)?.value
  const num = Number(raw)
  if (!raw || isNaN(num)) {
    return { width: '0%' }
  }
  // 简单策略：默认量程 0~100，超过按 100% 显示
  const max = 100
  const pct = Math.max(0, Math.min(100, (num / max) * 100))
  return {
    width: `${pct}%`,
    background: 'linear-gradient(90deg, #38bdf8, #6366f1)',
  }
}

/** 显示模式中文标签 */
function displayModeLabel(mode: DataBinding['displayMode']): string {
  return mode === 'text' ? '文本' : mode === 'gauge' ? '仪表盘' : '颜色映射'
}
</script>

<template>
  <div class="data-overlay-panel glass-card rounded-xl border border-primary/15 overflow-hidden flex flex-col">
    <!-- 头部 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-primary/10 bg-dark-surface/30">
      <h3 class="text-sm font-medium text-white flex items-center gap-2">
        <Activity :size="14" class="text-primary-light" />
        实时数据
      </h3>
      <button
        class="p-1.5 rounded-lg text-gray-400 hover:text-primary-light hover:bg-primary/10 transition-all disabled:opacity-50"
        title="刷新数据"
        :disabled="loading"
        @click="emit('refresh')"
      >
        <RefreshCw :size="14" :class="loading ? 'animate-spin' : ''" />
      </button>
    </div>

    <!-- 数据项列表 -->
    <div class="overlay-items flex-1 overflow-y-auto scrollbar-thin p-3 space-y-2">
      <template v-if="hasBindings">
        <div
          v-for="(binding, i) in bindings"
          :key="`${binding.indicatorId}-${i}`"
          class="overlay-item rounded-lg bg-dark-surface/40 border border-primary/10 p-2.5 hover:border-primary/30 transition-colors"
        >
          <!-- 标签行 -->
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-xs text-gray-300 font-medium truncate">
              {{ binding.label || binding.indicatorName || '未命名' }}
            </span>
            <span
              v-if="binding.objectName"
              class="text-[10px] text-gray-600 ml-2 shrink-0"
              :title="`来源：${binding.objectName}`"
            >
              {{ binding.objectName }}
            </span>
          </div>

          <!-- 文本模式 -->
          <div v-if="binding.displayMode === 'text'" class="flex items-baseline gap-1">
            <span class="text-lg font-semibold text-white tabular-nums">
              {{ formatValue(binding) }}
            </span>
            <span v-if="binding.unit" class="text-xs text-gray-500">{{ binding.unit }}</span>
          </div>

          <!-- 仪表盘模式 -->
          <div v-else-if="binding.displayMode === 'gauge'" class="space-y-1">
            <div class="flex items-baseline justify-between">
              <span class="text-sm font-semibold text-white tabular-nums">
                {{ formatValue(binding) }}
                <span v-if="binding.unit" class="text-xs text-gray-500 ml-0.5">{{ binding.unit }}</span>
              </span>
              <span class="text-[10px] text-gray-600">{{ displayModeLabel(binding.displayMode) }}</span>
            </div>
            <div class="h-1.5 rounded-full bg-dark-surface/80 overflow-hidden">
              <div class="h-full rounded-full transition-all duration-500" :style="gaugeStyle(binding)" />
            </div>
          </div>

          <!-- 颜色映射模式：暂时按文本展示，颜色由调用方决定 -->
          <div v-else class="flex items-baseline gap-1">
            <span
              class="text-lg font-semibold tabular-nums"
              :style="{ color: formatValue(binding) === '--' ? '#9ca3af' : '#34d399' }"
            >
              {{ formatValue(binding) }}
            </span>
            <span v-if="binding.unit" class="text-xs text-gray-500">{{ binding.unit }}</span>
          </div>

          <!-- 指标名（次要信息） -->
          <div v-if="binding.indicatorName && binding.label" class="text-[10px] text-gray-600 mt-1 truncate">
            {{ binding.indicatorName }}
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-8 text-gray-600">
        <Activity :size="24" class="mb-2 opacity-50" />
        <p class="text-xs">该场景暂无数据绑定</p>
        <p class="text-[10px] mt-1">在编辑器中选中对象后可添加</p>
      </div>
    </div>
  </div>
</template>
