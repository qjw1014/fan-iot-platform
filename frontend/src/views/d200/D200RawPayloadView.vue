<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">D200原始数据</div>
        <p>保存每条 MQTT 原始 JSON，无法映射标准字段时也不会丢失</p>
      </div>
      <div class="actions">
        <el-input v-model="query.gatewaySn" clearable placeholder="SN筛选" @keyup.enter="loadData" />
        <el-select v-model="processedText" clearable placeholder="处理状态" @change="loadData">
          <el-option label="已处理" value="true" />
          <el-option label="未处理" value="false" />
        </el-select>
        <el-button :icon="Search" @click="loadData">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="gatewaySn" label="SN" min-width="130" />
      <el-table-column prop="topic" label="Topic" min-width="220" />
      <el-table-column prop="qos" label="QoS" min-width="70" />
      <el-table-column prop="imei" label="IMEI" min-width="150" />
      <el-table-column prop="iccid" label="ICCID" min-width="170" />
      <el-table-column label="设备时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.deviceTime) }}</template></el-table-column>
      <el-table-column label="接收时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template></el-table-column>
      <el-table-column label="处理状态" min-width="100"><template #default="{ row }"><el-tag :type="row.processed ? 'success' : 'warning'" effect="dark">{{ row.processed ? '已处理' : '未处理' }}</el-tag></template></el-table-column>
      <el-table-column prop="processError" label="处理说明" min-width="260"><template #default="{ row }">{{ textOrDash(row.processError) }}</template></el-table-column>
      <el-table-column label="操作" fixed="right" width="120"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">查看JSON</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="loadData" @size-change="loadData" /></div>

    <el-dialog v-model="dialogVisible" title="D200原始JSON" width="760px">
      <pre class="json-box">{{ detailJson }}</pre>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { d200Api } from '@/api/business'
import type { D200RawPayload } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const records = ref<D200RawPayload[]>([])
const loading = ref(false)
const total = ref(0)
const processedText = ref('')
const dialogVisible = ref(false)
const detailJson = ref('')
const query = reactive<{ gatewaySn: string; processed?: boolean; page: number; size: number }>({ gatewaySn: '', processed: undefined, page: 1, size: 10 })

async function loadData() {
  query.processed = processedText.value === '' ? undefined : processedText.value === 'true'
  loading.value = true
  try {
    const page = await d200Api.rawPayloads(query)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openDetail(row: D200RawPayload) {
  detailJson.value = JSON.stringify({ rawPayload: row.rawPayload, dataPayload: row.dataPayload }, null, 2)
  dialogVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.json-box {
  max-height: 520px;
  overflow: auto;
  padding: 14px;
  border-radius: 8px;
  background: #06111f;
  color: #d7e9ff;
}
</style>
