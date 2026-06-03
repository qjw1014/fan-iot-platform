<template>
  <section class="ai-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">AI接口管理</div>
        <p>面向第三方 AI、大模型和数据分析系统提供标准化数据接口</p>
      </div>
      <div class="actions">
        <el-input v-model="apiKey" show-password placeholder="请输入 X-API-Key" />
        <el-input v-model="deviceId" placeholder="设备编号" />
        <el-button type="primary" @click="loadRealtime">查询实时数据</el-button>
        <el-button @click="loadHistory">查询历史数据</el-button>
      </div>
    </div>

    <div class="api-grid">
      <div class="panel api-card">
        <div class="panel-title">开放接口</div>
        <p>GET /api/ai/realtime/{deviceId}</p>
        <p>GET /api/ai/history</p>
        <p>POST /api/ai/export</p>
        <el-alert title="接口必须通过 X-API-Key 或 Authorization: ApiKey xxx 认证，禁止第三方直接访问数据库。" type="info" show-icon :closable="false" />
      </div>
      <div class="panel api-card">
        <div class="panel-title">导出数据</div>
        <el-radio-group v-model="format">
          <el-radio-button label="json">JSON</el-radio-button>
          <el-radio-button label="csv">CSV</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="exportData">导出</el-button>
      </div>
    </div>

    <pre class="panel result">{{ resultText }}</pre>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { aiApi } from '@/api/business'

const apiKey = ref('fanai_dev_key_please_change')
const deviceId = ref('')
const format = ref<'json' | 'csv'>('json')
const resultText = ref('请输入设备编号后查询。')

async function loadRealtime() {
  if (!deviceId.value) {
    ElMessage.warning('请输入设备编号')
    return
  }
  const data = await aiApi.realtime(deviceId.value, apiKey.value)
  resultText.value = JSON.stringify(data, null, 2)
}

async function loadHistory() {
  if (!deviceId.value) {
    ElMessage.warning('请输入设备编号')
    return
  }
  const data = await aiApi.history({ deviceId: deviceId.value, limit: 100 }, apiKey.value)
  resultText.value = JSON.stringify(data, null, 2)
}

async function exportData() {
  if (!deviceId.value) {
    ElMessage.warning('请输入设备编号')
    return
  }
  const response = await aiApi.exportData({ deviceId: deviceId.value, format: format.value, limit: 500 }, apiKey.value)
  if (format.value === 'csv') {
    const blob = new Blob([response.data as BlobPart], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${deviceId.value}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('CSV导出成功')
  } else {
    resultText.value = JSON.stringify(response.data, null, 2)
  }
}
</script>

<style scoped>
.ai-page {
  min-height: calc(100vh - 126px);
  padding: 18px;
}
.api-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.api-card {
  padding: 16px;
}
.api-card p {
  color: #d8edff;
}
.result {
  min-height: 360px;
  margin-top: 16px;
  padding: 16px;
  overflow: auto;
  color: #d8edff;
  white-space: pre-wrap;
}
@media (max-width: 900px) {
  .api-grid {
    grid-template-columns: 1fr;
  }
}
</style>
