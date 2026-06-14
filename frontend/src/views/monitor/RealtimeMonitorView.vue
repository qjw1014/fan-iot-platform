<template>
  <div class="monitor-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>实时数据</h2>
          <p>查看设备最新运行状态与实时遥测指标</p>
        </div>
        <span class="refresh-time">最后刷新：{{ formatDateTime(lastRefreshAt) }}</span>
      </div>

      <div class="filter-bar">
        <a-select
          v-model:value="query.deviceId"
          allow-clear
          show-search
          placeholder="选择设备"
          :filter-option="filterDevice"
          :options="deviceOptions"
          @change="handleQueryChange"
        />
        <a-select
          v-model:value="query.status"
          allow-clear
          placeholder="运行状态"
          :options="statusOptions"
          @change="handleQueryChange"
        />
        <a-button type="primary" :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新数据
        </a-button>
      </div>
    </a-card>

    <a-card class="current-card" title="当前设备实时数据" :bordered="false">
      <template #extra>
        <a-tag v-if="currentMetric" :color="statusColor(currentMetric.status)">
          {{ statusLabel(currentMetric.status) }}
        </a-tag>
      </template>

      <template v-if="currentMetric">
        <div class="device-summary">
          <div>
            <strong>{{ currentMetric.deviceName || currentMetric.deviceId }}</strong>
            <span>{{ currentMetric.deviceId }}</span>
          </div>
          <a-tag color="blue">{{ currentMetric.gatewayId }}</a-tag>
        </div>

        <div class="metric-grid">
          <div v-for="item in primaryMetrics" :key="item.label" class="metric-cell">
            <a-statistic :title="item.label" :value="item.value" :precision="item.precision">
              <template #suffix><span class="metric-unit">{{ item.unit }}</span></template>
            </a-statistic>
          </div>
        </div>

        <a-descriptions class="metric-descriptions" :column="{ xs: 1, sm: 2, lg: 4 }" bordered size="small">
          <a-descriptions-item label="频率">{{ metricText(currentMetric.frequency, 'Hz', 1) }}</a-descriptions-item>
          <a-descriptions-item label="压力">{{ metricText(currentMetric.pressure, 'Pa', 1) }}</a-descriptions-item>
          <a-descriptions-item label="风量">{{ metricText(currentMetric.airflow, 'm³/h', 1) }}</a-descriptions-item>
          <a-descriptions-item label="轴承温度">{{ metricText(currentMetric.bearingTemperature, '°C', 1) }}</a-descriptions-item>
          <a-descriptions-item label="告警编码">{{ currentMetric.alarmCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采集时间">{{ formatDateTime(currentMetric.timestamp) }}</a-descriptions-item>
          <a-descriptions-item label="接收时间">{{ formatDateTime(currentMetric.receivedAt) }}</a-descriptions-item>
          <a-descriptions-item label="通信状态">
            <a-tag :color="statusColor(currentMetric.status)">{{ statusLabel(currentMetric.status) }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </template>
      <a-empty v-else description="当前筛选条件下暂无实时数据" />
    </a-card>

    <div class="monitor-grid">
      <a-card class="chart-card" title="实时运行指标" :bordered="false">
        <template #extra><span class="card-extra">最多展示 8 台设备</span></template>
        <VChart v-if="filteredDevices.length" :option="metricChartOption" autoresize />
        <a-empty v-else description="暂无可展示的实时指标" />
      </a-card>

      <a-card class="table-card" title="设备最新数据" :bordered="false">
        <a-table
          v-if="loading || filteredDevices.length"
          :columns="columns"
          :data-source="filteredDevices"
          :loading="loading"
          :pagination="false"
          :scroll="{ x: 1120, y: 320 }"
          row-key="deviceId"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'device'">
              <div class="table-stack">
                <strong>{{ record.deviceName || record.deviceId }}</strong>
                <span>{{ record.deviceId }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'timestamp'">
              {{ formatDateTime(record.timestamp || record.receivedAt) }}
            </template>
            <template v-else>
              {{ tableMetric(record, column.dataIndex) }}
            </template>
          </template>
        </a-table>
        <a-empty v-else description="暂无设备最新数据" />
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'

import { deviceApi, monitorApi } from '@/api/business'
import { useThemeStore } from '@/stores/theme'
import type { Device, RealtimeMetric, RealtimeOverview } from '@/types/business'
import { formatDateTime } from '@/utils/format'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const columns = [
  { title: '设备', key: 'device', width: 180, fixed: 'left' },
  { title: '转速', dataIndex: 'rpm', key: 'rpm', width: 100 },
  { title: '电流', dataIndex: 'current', key: 'current', width: 90 },
  { title: '电压', dataIndex: 'voltage', key: 'voltage', width: 90 },
  { title: '功率', dataIndex: 'power', key: 'power', width: 90 },
  { title: '电机温度', dataIndex: 'motorTemperature', key: 'motorTemperature', width: 110 },
  { title: '振动', dataIndex: 'vibration', key: 'vibration', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '采集时间', key: 'timestamp', width: 180 }
]

const statusOptions = [
  { label: '运行', value: 'running' },
  { label: '正常', value: 'normal' },
  { label: '在线', value: 'online' },
  { label: '预警', value: 'warning' },
  { label: '严重', value: 'critical' },
  { label: '故障', value: 'fault' },
  { label: '离线', value: 'offline' }
]

const themeStore = useThemeStore()
const loading = ref(false)
const devices = ref<Device[]>([])
const latestDevices = ref<RealtimeMetric[]>([])
const overview = ref<RealtimeOverview | null>(null)
const lastRefreshAt = ref<string>()
const query = reactive({
  deviceId: undefined as string | undefined,
  status: undefined as string | undefined
})
let timer: number | undefined

const deviceOptions = computed(() =>
  devices.value.map((item) => ({
    label: `${item.deviceName}（${item.deviceId}）`,
    value: item.deviceId
  }))
)

const filteredDevices = computed(() =>
  latestDevices.value.filter((item) => {
    const matchesDevice = !query.deviceId || item.deviceId === query.deviceId
    const matchesStatus = !query.status || item.status === query.status
    return matchesDevice && matchesStatus
  })
)

const currentMetric = computed(() => {
  if (query.deviceId) {
    return filteredDevices.value.find((item) => item.deviceId === query.deviceId) || null
  }
  return filteredDevices.value[0] || null
})

const primaryMetrics = computed(() => {
  const metric = currentMetric.value
  return [
    { label: '转速', value: metric?.rpm, precision: 0, unit: 'rpm' },
    { label: '电流', value: metric?.current, precision: 2, unit: 'A' },
    { label: '电压', value: metric?.voltage, precision: 1, unit: 'V' },
    { label: '功率', value: metric?.power, precision: 2, unit: 'kW' },
    { label: '电机温度', value: metric?.motorTemperature, precision: 1, unit: '°C' },
    { label: '振动', value: metric?.vibration, precision: 2, unit: 'mm/s' }
  ]
})

const metricChartOption = computed(() => {
  const dark = themeStore.isDark
  const labelColor = dark ? '#91a4bd' : '#667085'
  const gridColor = dark ? '#233149' : '#e9edf3'
  const chartDevices = filteredDevices.value.slice(0, 8)
  return {
    animation: false,
    color: ['#1677ff', '#d89614', '#e5484d', '#22a06b'],
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      right: 0,
      textStyle: { color: labelColor },
      data: ['转速', '电机温度', '振动', '功率']
    },
    grid: { left: 52, right: 24, top: 48, bottom: 38 },
    xAxis: {
      type: 'category',
      data: chartDevices.map((item) => item.deviceId),
      axisLabel: { color: labelColor },
      axisLine: { lineStyle: { color: gridColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: labelColor },
      splitLine: { lineStyle: { color: gridColor } }
    },
    series: [
      { name: '转速', type: 'bar', data: chartDevices.map((item) => item.rpm ?? null) },
      { name: '电机温度', type: 'bar', data: chartDevices.map((item) => item.motorTemperature ?? null) },
      { name: '振动', type: 'bar', data: chartDevices.map((item) => item.vibration ?? null) },
      { name: '功率', type: 'bar', data: chartDevices.map((item) => item.power ?? null) }
    ]
  }
})

function filterDevice(input: string, option?: { label: string }) {
  return option?.label.toLowerCase().includes(input.toLowerCase()) ?? false
}

function statusLabel(value?: string) {
  return {
    online: '在线',
    running: '运行',
    normal: '正常',
    warning: '预警',
    critical: '严重',
    fault: '故障',
    offline: '离线'
  }[value || ''] || value || '未知'
}

function statusColor(value?: string) {
  if (value === 'critical' || value === 'fault') return 'error'
  if (value === 'warning') return 'warning'
  if (value === 'running' || value === 'normal' || value === 'online') return 'success'
  return 'default'
}

function metricText(value?: number, unit = '', precision = 1) {
  return value == null ? '-' : `${Number(value).toFixed(precision)} ${unit}`
}

function tableMetric(record: RealtimeMetric, dataIndex?: string) {
  if (!dataIndex) return '-'
  const value = record[dataIndex as keyof RealtimeMetric]
  return value == null || value === '' ? '-' : String(value)
}

async function loadDevices() {
  devices.value = (await deviceApi.page({ page: 1, size: 100 })).records
}

async function loadData() {
  loading.value = true
  try {
    const overviewData = await monitorApi.overview()
    overview.value = overviewData
    latestDevices.value = overviewData.latestDevices || []
    lastRefreshAt.value = new Date().toISOString()
  } finally {
    loading.value = false
  }
}

async function handleQueryChange() {
  if (query.deviceId && !latestDevices.value.some((item) => item.deviceId === query.deviceId)) {
    const latest = await monitorApi.latest({ deviceId: query.deviceId, limit: 1 })
    const otherDevices = latestDevices.value.filter((item) => item.deviceId !== query.deviceId)
    latestDevices.value = [...latest, ...otherDevices]
  }
}

onMounted(async () => {
  await Promise.all([loadDevices(), loadData()])
  timer = window.setInterval(loadData, 10_000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.monitor-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-heading h2 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
}

.page-heading p,
.refresh-time,
.card-extra {
  color: var(--muted);
  font-size: 13px;
}

.page-heading p {
  margin: 5px 0 0;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 170px auto;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.device-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.device-summary strong,
.device-summary span {
  display: block;
}

.device-summary strong {
  color: var(--text);
  font-size: 18px;
}

.device-summary span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-bottom: 16px;
  border-top: 1px solid var(--line);
  border-left: 1px solid var(--line);
}

.metric-cell {
  min-height: 92px;
  padding: 14px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.metric-cell :deep(.ant-statistic-title) {
  color: var(--muted);
  font-size: 12px;
}

.metric-cell :deep(.ant-statistic-content) {
  color: var(--text);
  font-size: 24px;
}

.metric-unit {
  margin-left: 3px;
  color: var(--muted);
  font-size: 12px;
}

.metric-descriptions :deep(.ant-descriptions-item-label) {
  color: var(--muted);
}

.monitor-grid {
  display: grid;
  gap: 16px;
}

.chart-card .echarts {
  width: 100%;
  height: 340px;
}

.chart-card :deep(.ant-empty),
.table-card :deep(.ant-empty),
.current-card :deep(.ant-empty) {
  padding: 72px 0;
}

.table-stack strong,
.table-stack span {
  display: block;
}

.table-stack strong {
  color: var(--text);
  font-weight: 500;
}

.table-stack span {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-heading {
    flex-direction: column;
  }

  .filter-bar,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
