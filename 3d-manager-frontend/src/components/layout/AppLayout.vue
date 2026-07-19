<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isEditor = computed(() => route.path === '/editor' || route.path.startsWith('/editor/'))
</script>

<template>
  <div class="min-h-screen bg-dark">
    <AppHeader v-if="!isEditor" />
    <AppSidebar v-if="!isEditor" />
    <main :class="isEditor ? 'min-h-screen' : 'ml-64 pt-16 min-h-screen'">
      <router-view />
    </main>
  </div>
</template>
