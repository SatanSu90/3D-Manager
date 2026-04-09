import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/preview/:sceneId',
      name: 'ScenePreview',
      component: () => import('@/views/ScenePreviewView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/',
      component: () => import('@/components/layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Models',
          component: () => import('@/views/ModelManageView.vue'),
        },
        {
          path: 'model/:id',
          name: 'ModelDetail',
          component: () => import('@/views/ModelDetailView.vue'),
          props: true,
        },
        {
          path: 'editor',
          name: 'Editor',
          component: () => import('@/views/EditorView.vue'),
        },
        {
          path: 'editor/:sceneId',
          name: 'EditorLoad',
          component: () => import('@/views/EditorView.vue'),
          props: true,
        },
        {
          path: 'scenes',
          name: 'Scenes',
          component: () => import('@/views/SceneManageView.vue'),
        },
        {
          path: 'categories',
          name: 'Categories',
          component: () => import('@/views/CategoryManageView.vue'),
        },
        {
          path: 'tags',
          name: 'Tags',
          component: () => import('@/views/TagManageView.vue'),
        },
        {
          path: 'admin',
          name: 'Admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { requiresAdmin: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'Models' }
  }

  if (to.path === '/login' && authStore.isLoggedIn) {
    return { name: 'Models' }
  }
})

export default router
