<template>
  <div class="d200-page">
    <a-card :bordered="false">
      <div class="page-heading">
        <div>
          <h2>D200 原始数据</h2>
          <p>保存每条 MQTT 原始 JSON，无法映射标准字段时也不会丢失</p>
        </div>
      </div>
      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="盒子 SN">
          <a-input v-model:value="query.gatewaySn" allow-clear placeholder="请输入 SN" />
        </a-form-item>
        <a-form-item label="处理状态">
          <a-select
            v-model:value="processedText"
            allow-clear
            placeholder="全部状态"
            :options="processedOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="原始数据记录" :bordered="false">
      <template #extra><span class="record-summary">共 {{ total }} 条记录</span></template>
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1640, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'gatewaySn'">{{ textOrDash(record.gatewaySn) }}</template>
          <template v-else-if="column.key === 'imei'">{{ textOrDash(record.imei) }}</template>
          <template v-else-if="column.key === 'iccid'">{{ textOrDash(record.iccid) }}</template>
          <template v-else-if="column.key === 'deviceTime'">{{ formatDateTime(record.deviceTime) }}</template>
          <template v-else-if="column.key === 'receivedAt'">{{ formatDateTime(record.receivedAt) }}</template>
          <template v-else-if="column.key === 'processed'">
            <a-tag :color="record.processed ? 'success' : 'warning'">
              {{ record.processed ? '已处理' : '未处理' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'processError'">
            <span class="ellipsis-cell" :title="record.processError">{{ textOrDash(record.processError) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看 JSON</a-button>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无 D200 原始数据" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条数据</span>
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.size"
          :page-size-options="['10', '20', '50']"
          :total="total"
          show-size-changer
          @change="loadData"
          @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>

    <a-modal v-model:open="dialogVisible" title="D200 原始 JSON" width="760px" :footer="null">
      <pre class="json-box">{{ detailJson }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'

import { d200Api } from '@/api/business'
import type { D200RawPayload } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: 'SN', key: 'gatewaySn', width: 170, fixed: 'left' },
  { title: 'Topic', dataIndex: 'topic', key: 'topic', width: 260 },
  { title: 'QoS', dataIndex: 'qos', key: 'qos', width: 80 },
  { title: 'IMEI', key: 'imei', width: 170 },
  { title: 'ICCID', key: 'iccid', width: 190 },
  { title: '设备时间', key: 'deviceTime', width: 180 },
  { title: '接收时间', key: 'receivedAt', width: 180 },
  { title: '处理状态', key: 'processed', width: 110 },
  { title: '处理说明', key: 'processError', width: 280 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]
const processedOptions = [
  { label: '已处理', value: 'true' },
  { label: '未处理', value: 'false' }
]

const records = ref<D200RawPayload[]>([])
const loading = ref(false)
const total = ref(0)
const processedText = ref<string>()
const dialogVisible = ref(false)
const detailJson = ref('')
const query = reactive<{ gatewaySn: string; processed?: boolean; page: number; size: number }>({
  gatewaySn: '',
  processed: undefined,
  page: 1,
  size: 10
})

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadData() {
  query.processed =
    processedText.value == null ? undefined : processedText.value === 'true'
  loading.value = true
  try {
    const pageData = await d200Api.rawPayloads(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, 'D200 原始数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  query.gatewaySn = ''
  query.processed = undefined
  query.page = 1
  processedText.value = undefined
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

function openDetail(row: D200RawPayload) {
  detailJson.value = JSON.stringify(
    { rawPayload: row.rawPayload, dataPayload: row.dataPayload },
    null,
    2
  )
  dialogVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.d200-page {
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
.query-form :deep(.ant-input) {
  width: 240px;
}
.query-form :deep(.ant-select) {
  width: 160px;
}
.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}
.ellipsis-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
}
.json-box {
  max-height: 520px;
  margin: 0;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
  color: var(--text);
}
@media (max-width: 760px) {
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-input),
  .query-form :deep(.ant-select) {
    width: 100%;
  }
}
</style>
