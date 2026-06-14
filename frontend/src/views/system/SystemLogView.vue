<template>
  <div class="log-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>系统日志</h2>
          <p>查看平台运行与操作日志，支撑审计和故障排查</p>
        </div>
      </div>

      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="日志级别">
          <a-select
            v-model:value="query.logLevel"
            allow-clear
            placeholder="全部级别"
            :options="levelOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item label="模块">
          <a-input v-model:value="query.module" allow-clear placeholder="请输入模块名称" />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="搜索操作或日志内容"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
            <a-button @click="resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" title="日志记录" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>

      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1320, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'logLevel'">
            <a-tag :color="levelColor(record.logLevel)">{{ levelLabel(record.logLevel) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'module'">
            {{ textOrDash(record.module) }}
          </template>
          <template v-else-if="column.key === 'operation'">
            {{ textOrDash(record.operation) }}
          </template>
          <template v-else-if="column.key === 'message'">
            <span class="message-cell" :title="record.message">{{ record.message }}</span>
          </template>
          <template v-else-if="column.key === 'userId'">
            {{ record.userId ?? '-' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无系统日志" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条日志记录</span>
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.size"
          :page-size-options="['10', '20', '50']"
          :total="total"
          show-size-changer
          show-quick-jumper
          @change="loadData"
          @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'

import { systemLogApi } from '@/api/business'
import type { SystemLog } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '级别', key: 'logLevel', width: 100, fixed: 'left' },
  { title: '类型', dataIndex: 'logType', key: 'logType', width: 130 },
  { title: '模块', key: 'module', width: 160 },
  { title: '操作', key: 'operation', width: 170 },
  { title: '日志内容', key: 'message', width: 460 },
  { title: '用户编号', key: 'userId', width: 110 },
  { title: '记录时间', key: 'createdAt', width: 180, fixed: 'right' }
]

const levelOptions = [
  { label: '调试', value: 'debug' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warn' },
  { label: '错误', value: 'error' }
]

const loading = ref(false)
const records = ref<SystemLog[]>([])
const total = ref(0)
const query = reactive({
  keyword: '',
  logLevel: undefined as string | undefined,
  module: '',
  page: 1,
  size: 10
})

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function levelLabel(value: string) {
  return {
    debug: '调试',
    info: '信息',
    warn: '警告',
    error: '错误'
  }[value] || value
}

function levelColor(value: string) {
  if (value === 'error') return 'error'
  if (value === 'warn') return 'warning'
  if (value === 'info') return 'processing'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await systemLogApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '系统日志加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  Object.assign(query, {
    keyword: '',
    logLevel: undefined,
    module: '',
    page: 1
  })
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.log-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.page-heading {
  margin-bottom: 18px;
}

.page-heading h2 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
}

.page-heading p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.query-form {
  padding: 14px 14px 0;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.query-form :deep(.ant-select) {
  width: 160px;
}

.query-form :deep(.ant-input) {
  width: 220px;
}

.query-form :deep(.ant-input-affix-wrapper) {
  width: 280px;
}

.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}

.message-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-card :deep(.ant-empty) {
  padding: 96px 0;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
}

@media (max-width: 1000px) {
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-select),
  .query-form :deep(.ant-input),
  .query-form :deep(.ant-input-affix-wrapper) {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
