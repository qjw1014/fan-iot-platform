<template>
  <div class="ai-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>AI 接口管理</h2>
          <p>面向第三方 AI、大模型和数据分析系统调用平台标准数据</p>
        </div>
        <a-tag color="processing">Open API</a-tag>
      </div>

      <a-form class="query-form" layout="inline">
        <a-form-item label="API Key" required>
          <a-input-password
            v-model:value="apiKey"
            allow-clear
            autocomplete="off"
            placeholder="请输入 X-API-Key"
          />
        </a-form-item>
        <a-form-item label="设备编号" required>
          <a-input v-model:value="deviceId" allow-clear placeholder="请输入设备编号" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="loadingAction === 'realtime'" @click="loadRealtime">
              查询实时数据
            </a-button>
            <a-button :loading="loadingAction === 'history'" @click="loadHistory">
              查询历史数据
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="15">
        <a-card title="开放接口" :bordered="false">
          <a-table
            :columns="endpointColumns"
            :data-source="endpoints"
            :pagination="false"
            row-key="path"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'method'">
                <a-tag :color="record.method === 'GET' ? 'processing' : 'success'">
                  {{ record.method }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'path'">
                <code>{{ record.path }}</code>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="9">
        <a-card title="接入信息" :bordered="false">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="认证请求头">X-API-Key</a-descriptions-item>
            <a-descriptions-item label="备用认证">Authorization: ApiKey xxx</a-descriptions-item>
            <a-descriptions-item label="实时查询设备">
              {{ deviceId || '尚未选择设备' }}
            </a-descriptions-item>
            <a-descriptions-item label="历史查询条数">100</a-descriptions-item>
            <a-descriptions-item label="导出条数">500</a-descriptions-item>
          </a-descriptions>
          <a-alert
            class="security-alert"
            title="第三方系统必须通过 API Key 调用开放接口，不允许直接访问数据库。"
            type="info"
            show-icon
            :closable="false"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="数据导出" :bordered="false">
      <div class="export-bar">
        <a-radio-group v-model:value="format" button-style="solid">
          <a-radio-button value="json">JSON</a-radio-button>
          <a-radio-button value="csv">CSV</a-radio-button>
        </a-radio-group>
        <a-button type="primary" :loading="loadingAction === 'export'" @click="exportData">
          <template #icon><DownloadOutlined /></template>
          导出数据
        </a-button>
      </div>
    </a-card>

    <a-card title="接口响应" :bordered="false">
      <template #extra>
        <a-button v-if="resultText" type="link" size="small" @click="clearResult">清空</a-button>
      </template>
      <pre v-if="resultText" class="result-view">{{ resultText }}</pre>
      <a-empty v-else description="输入 API Key 和设备编号后调用接口" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { DownloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

import { aiApi } from '@/api/business'

const endpointColumns = [
  { title: '方法', key: 'method', width: 90 },
  { title: '调用地址', key: 'path', width: 300 },
  { title: '现有用途', dataIndex: 'description', key: 'description' }
]

const endpoints = [
  { method: 'GET', path: '/api/ai/realtime/{deviceId}', description: '查询指定设备最新标准数据' },
  { method: 'GET', path: '/api/ai/history', description: '查询指定设备历史标准数据' },
  { method: 'POST', path: '/api/ai/export', description: '按 JSON 或 CSV 格式导出历史数据' }
]

const apiKey = ref('')
const deviceId = ref('')
const format = ref<'json' | 'csv'>('json')
const resultText = ref('')
const loadingAction = ref<'realtime' | 'history' | 'export' | ''>('')

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function validateInput() {
  if (!apiKey.value.trim()) {
    message.error('请输入 API Key')
    return false
  }
  if (!deviceId.value.trim()) {
    message.error('请输入设备编号')
    return false
  }
  return true
}

async function loadRealtime() {
  if (!validateInput()) return
  loadingAction.value = 'realtime'
  try {
    const data = await aiApi.realtime(deviceId.value.trim(), apiKey.value.trim())
    resultText.value = JSON.stringify(data, null, 2)
  } catch (error) {
    message.error(errorText(error, '实时数据查询失败'))
  } finally {
    loadingAction.value = ''
  }
}

async function loadHistory() {
  if (!validateInput()) return
  loadingAction.value = 'history'
  try {
    const data = await aiApi.history(
      { deviceId: deviceId.value.trim(), limit: 100 },
      apiKey.value.trim()
    )
    resultText.value = JSON.stringify(data, null, 2)
  } catch (error) {
    message.error(errorText(error, '历史数据查询失败'))
  } finally {
    loadingAction.value = ''
  }
}

async function exportData() {
  if (!validateInput()) return
  loadingAction.value = 'export'
  try {
    const response = await aiApi.exportData(
      { deviceId: deviceId.value.trim(), format: format.value, limit: 500 },
      apiKey.value.trim()
    )
    if (format.value === 'csv') {
      const blob = new Blob([response.data as BlobPart], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${deviceId.value.trim()}.csv`
      link.click()
      URL.revokeObjectURL(url)
      message.success('CSV 导出成功')
    } else {
      resultText.value = JSON.stringify(response.data, null, 2)
      message.success('JSON 数据已生成')
    }
  } catch (error) {
    message.error(errorText(error, '数据导出失败'))
  } finally {
    loadingAction.value = ''
  }
}

function clearResult() {
  resultText.value = ''
}
</script>

<style scoped>
.ai-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.query-form :deep(.ant-input-affix-wrapper),
.query-form :deep(.ant-input) {
  width: 280px;
}

.security-alert {
  margin-top: 16px;
}

.export-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.result-view {
  min-height: 280px;
  max-height: 520px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
  color: var(--text);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-page :deep(.ant-empty) {
  padding: 72px 0;
}

code {
  color: var(--primary);
  font-family: "Cascadia Code", Consolas, monospace;
}

@media (max-width: 900px) {
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-input-affix-wrapper),
  .query-form :deep(.ant-input) {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .page-heading,
  .export-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
