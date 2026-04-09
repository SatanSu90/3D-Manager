<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ref } from 'vue'
import { Search, User, LogOut, Settings, Box } from 'lucide-vue-next'

const authStore = useAuthStore()
const router = useRouter()
const searchKeyword = ref('')
const showUserMenu = ref(false)

function handleSearch() {
  // will be handled by model store
}

function goToModels() {
  router.push('/')
}

function goToAdmin() {
  router.push('/admin')
}



function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="fixed top-0 left-0 right-0 h-16 z-50 glass-card border-b border-primary/10 rounded-none flex items-center px-6 justify-between">
    <div class="flex items-center gap-4">
      <div class="flex items-center gap-2 cursor-pointer hover-glow rounded-lg px-3 py-1.5" @click="goToModels">
        <Box :size="28" class="text-primary-light" />
        <span class="text-xl font-semibold gradient-text">3D Manager</span>
      </div>
    </div>

    <div class="flex-1 max-w-xl mx-8">
      <div class="relative">
        <Search :size="18" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索模型..."
          class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-gray-500 focus:border-primary/40 focus:outline-none transition-all"
          @keyup.enter="handleSearch"
        />
      </div>
    </div>

    <div class="flex items-center gap-3">
      <div class="relative">
        <button
          class="flex items-center gap-2 hover-glow rounded-lg px-3 py-1.5 transition-all"
          @click="showUserMenu = !showUserMenu"
        >
          <div class="w-8 h-8 rounded-full bg-gradient-to-br from-primary to-primary-accent flex items-center justify-center">
            <User :size="16" class="text-white" />
          </div>
          <span class="text-sm text-gray-300">{{ authStore.user?.username }}</span>
        </button>

        <div
          v-if="showUserMenu"
          class="absolute right-0 top-12 glass-panel p-2 min-w-[160px] animate-fade-in"
          @mouseleave="showUserMenu = false"
        >
          <button
            v-if="authStore.isAdmin"
            class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-300 hover:bg-primary/10 hover:text-primary-light rounded-lg transition-all"
            @click="goToAdmin"
          >
            <Settings :size="16" />
            管理后台
          </button>
          <button
            class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-300 hover:bg-danger/10 hover:text-danger rounded-lg transition-all"
            @click="handleLogout"
          >
            <LogOut :size="16" />
            退出登录
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
