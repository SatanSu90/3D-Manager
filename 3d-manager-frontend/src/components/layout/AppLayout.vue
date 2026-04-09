<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
</script>

<template>
  <div class="min-h-screen bg-dark">
    <AppHeader />
    <AppSidebar />
    <main class="ml-64 pt-16 min-h-screen">
      <router-view />
    </main>
  </div>
</template>
