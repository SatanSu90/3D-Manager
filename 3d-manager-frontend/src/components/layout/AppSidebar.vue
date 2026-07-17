<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { menuConfig } from '@/config/menu'
import type { MenuItem } from '@/config/menu'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

/** Filter out admin-only groups when the current user is not an admin */
const visibleGroups = computed(() =>
  menuConfig.filter((group) => !group.permission || group.permission !== 'admin' || authStore.isAdmin)
)

/** Determine whether a menu item should be highlighted as active */
function isActive(item: MenuItem): boolean {
  const currentPath = route.path
  // Exact match
  if (currentPath === item.route) return true
  // Prefix match for /models to cover model detail pages like /models/123
  if (item.route === '/models' && currentPath.startsWith('/models/')) return true
  return false
}

/** Navigate to the menu item's route (only for active items) */
function navigate(item: MenuItem): void {
  if (item.status !== 'active') return
  router.push(item.route)
}
</script>

<template>
  <aside class="fixed left-0 top-16 bottom-0 w-64 glass-card border-r border-primary/10 rounded-none overflow-y-auto p-4">
    <nav class="space-y-6">
      <div v-for="group in visibleGroups" :key="group.key">
        <!-- 分组标题 -->
        <div class="flex items-center gap-2 px-3 mb-2">
          <component :is="group.icon" :size="14" class="text-gray-600" />
          <span class="text-xs font-medium text-gray-600 uppercase tracking-wide">{{ group.label }}</span>
        </div>
        <!-- 菜单项 -->
        <div class="space-y-1">
          <template v-for="item in group.children" :key="item.key">
            <!-- 可点击的活跃菜单项 -->
            <button
              v-if="item.status === 'active'"
              class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all flex items-center gap-2"
              :class="isActive(item)
                ? 'bg-primary/15 text-primary-light'
                : 'text-gray-400 hover:bg-primary/5 hover:text-gray-300'"
              @click="navigate(item)"
            >
              <component :is="item.icon" :size="14" class="shrink-0" />
              <span>{{ item.label }}</span>
            </button>
            <!-- 即将上线：灰色不可点击 -->
            <button
              v-else
              disabled
              title="即将上线"
              class="w-full text-left px-3 py-2 rounded-lg text-sm cursor-not-allowed flex items-center gap-2 text-gray-600"
            >
              <component :is="item.icon" :size="14" class="shrink-0 opacity-50" />
              <span class="opacity-60">{{ item.label }}</span>
            </button>
          </template>
        </div>
      </div>
    </nav>
  </aside>
</template>
