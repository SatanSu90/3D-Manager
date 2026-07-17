import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DataSource, DataSourceCreateRequest, DataSourceTestRequest, ConnectionTestResult, TableInfo, ColumnInfo, DataPreviewResult } from '@/types/datasource'
import type { PageData } from '@/types/api'
import * as dsApi from '@/api/datasource'

export const useDataSourceStore = defineStore('datasource', () => {
  const dataSources = ref<DataSource[]>([])
  const total = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const loading = ref(false)
  const keyword = ref('')
  const typeFilter = ref<string>('')

  async function loadDataSources() {
    loading.value = true
    try {
      const res = await dsApi.getDataSources({
        keyword: keyword.value || undefined,
        type: typeFilter.value || undefined,
        page: currentPage.value,
        size: pageSize.value,
      })
      const pageData = res.data.data as PageData<DataSource>
      dataSources.value = pageData.content
      total.value = pageData.totalElements
    } finally {
      loading.value = false
    }
  }

  async function createDataSource(data: DataSourceCreateRequest) {
    const res = await dsApi.createDataSource(data)
    await loadDataSources()
    return res.data.data
  }

  async function updateDataSource(id: number, data: Partial<DataSourceCreateRequest>) {
    const res = await dsApi.updateDataSource(id, data)
    await loadDataSources()
    return res.data.data
  }

  async function deleteDataSource(id: number) {
    await dsApi.deleteDataSource(id)
    await loadDataSources()
  }

  async function testConnectionDirect(data: DataSourceTestRequest): Promise<ConnectionTestResult> {
    const res = await dsApi.testConnectionDirect(data)
    return res.data.data
  }

  async function testConnection(id: number): Promise<ConnectionTestResult> {
    const res = await dsApi.testConnection(id)
    await loadDataSources()
    return res.data.data
  }

  async function getTables(id: number): Promise<TableInfo[]> {
    const res = await dsApi.getTables(id)
    return res.data.data
  }

  async function getColumns(id: number, tableName: string): Promise<ColumnInfo[]> {
    const res = await dsApi.getColumns(id, tableName)
    return res.data.data
  }

  async function previewData(id: number, tableName: string, limit = 100): Promise<DataPreviewResult> {
    const res = await dsApi.previewData(id, tableName, limit)
    return res.data.data
  }

  async function executeQuery(id: number, sql: string): Promise<DataPreviewResult> {
    const res = await dsApi.executeQuery(id, sql)
    return res.data.data
  }

  function setPage(page: number) {
    currentPage.value = page
    loadDataSources()
  }

  function setKeyword(kw: string) {
    keyword.value = kw
    currentPage.value = 0
    loadDataSources()
  }

  function setTypeFilter(type: string) {
    typeFilter.value = type
    currentPage.value = 0
    loadDataSources()
  }

  return {
    dataSources,
    total,
    currentPage,
    pageSize,
    loading,
    keyword,
    typeFilter,
    loadDataSources,
    createDataSource,
    updateDataSource,
    deleteDataSource,
    testConnectionDirect,
    testConnection,
    getTables,
    getColumns,
    previewData,
    executeQuery,
    setPage,
    setKeyword,
    setTypeFilter,
  }
})
