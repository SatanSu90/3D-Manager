<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Box, Eye, EyeOff } from 'lucide-vue-next'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await authStore.login(username.value, password.value)
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: unknown) {
    errorMsg.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-dark flex items-center justify-center relative overflow-hidden">
    <div class="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary-accent/5" />
    
    <div class="absolute top-1/4 left-1/4 w-64 h-64 bg-primary/10 rounded-full blur-[100px] animate-float" />
    <div class="absolute bottom-1/4 right-1/4 w-80 h-80 bg-primary-accent/8 rounded-full blur-[120px] animate-float" style="animation-delay: -3s" />
    
    <div class="relative z-10 w-full max-w-md mx-4">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-primary to-primary-accent mb-4 shadow-lg shadow-primary/30">
          <Box :size="32" class="text-white" />
        </div>
        <h1 class="text-3xl font-semibold gradient-text">3D Manager</h1>
        <p class="text-gray-500 mt-2 text-sm">三维模型资产管理平台</p>
      </div>

      <div class="glass-card p-8 animate-slide-up">
        <h2 class="text-xl font-medium text-white mb-6">登录</h2>
        
        <div v-if="errorMsg" class="mb-4 px-4 py-3 rounded-lg bg-danger/10 border border-danger/20 text-danger text-sm">
          {{ errorMsg }}
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1.5">用户名</label>
            <input
              v-model="username"
              type="text"
              placeholder="请输入用户名"
              class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-3 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
              @keyup.enter="handleLogin"
            />
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-1.5">密码</label>
            <div class="relative">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="w-full bg-dark-surface/50 border border-primary/15 rounded-xl px-4 py-3 pr-12 text-sm text-white placeholder-gray-600 focus:border-primary/40 focus:outline-none transition-all"
                @keyup.enter="handleLogin"
              />
              <button
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
          </div>

          <button
            class="w-full btn-primary py-3 text-sm"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="loading" class="inline-flex items-center gap-2">
              <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              登录中...
            </span>
            <span v-else>登录</span>
          </button>
        </div>
      </div>

      <p class="text-center text-gray-600 text-xs mt-6">
        © 2026 3D Manager. All rights reserved.
      </p>
    </div>
  </div>
</template>
