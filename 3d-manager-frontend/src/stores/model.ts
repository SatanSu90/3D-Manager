import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Model, ModelQuery, Category, Tag } from '@/types/model'
import type { PageData } from '@/types/api'
import { getModels } from '@/api/model'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'

export const useModelStore = defineStore('model', () => {
  const models = ref<Model[]>([])
  const totalElements = ref(0)
  const categories = ref<Category[]>([])
  const tags = ref<Tag[]>([])
  const loading = ref(false)

  const query = ref<ModelQuery>({
    keyword: '',
    categoryId: undefined,
    tagIds: [],
    page: 0,
    size: 20,
    sortBy: 'createdAt',
    sortDir: 'desc',
  })

  const totalPages = computed(() => Math.ceil(totalElements.value / query.value.size))

  async function loadModels() {
    loading.value = true
    try {
      const res = await getModels(query.value)
      const pageData = res.data.data as PageData<Model>
      models.value = pageData?.content ?? []
      totalElements.value = pageData?.totalElements ?? 0
    } catch (e) {
      console.error('加载模型列表失败', e)
    } finally {
      loading.value = false
    }
  }

  async function loadCategories() {
    try {
      const res = await getCategories()
      categories.value = res.data.data ?? []
    } catch (e) {
      console.error('加载分类失败', e)
    }
  }

  async function loadTags() {
    try {
      const res = await getTags()
      tags.value = res.data.data ?? []
    } catch (e) {
      console.error('加载标签失败', e)
    }
  }

  function setKeyword(keyword: string) {
    query.value.keyword = keyword
    query.value.page = 0
    loadModels()
  }

  function setCategory(categoryId: number | undefined) {
    query.value.categoryId = categoryId
    query.value.page = 0
    loadModels()
  }

  function toggleTag(tagId: number) {
    const idx = query.value.tagIds.indexOf(tagId)
    if (idx >= 0) {
      query.value.tagIds.splice(idx, 1)
    } else {
      query.value.tagIds.push(tagId)
    }
    query.value.page = 0
    loadModels()
  }

  function setPage(page: number) {
    query.value.page = page
    loadModels()
  }

  return {
    models,
    totalElements,
    totalPages,
    categories,
    tags,
    loading,
    query,
    loadModels,
    loadCategories,
    loadTags,
    setKeyword,
    setCategory,
    toggleTag,
    setPage,
  }
})
