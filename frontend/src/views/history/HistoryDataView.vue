<template>
  <div class="history-page">
    <a-card class="filter-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>历史数据</h2>
          <p>按设备和时间范围查询遥测记录与运行趋势</p>
        </div>
        <span class="record-summary">当前 {{ records.length }} 条记录</span>
      </div>

      <div class="filter-bar">
        <a-select
          v-model:value="query.deviceId"
          show-search
          allow-clear
          placeholder="选择设备"
          :filter-option="filterDevice"
          :options="deviceOptions"
        />
        <a-range-picker
          v-model:value="range"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
          :placeholder="['开始时间', '结束时间']"
        />
        <a-button type="primary" :loading="loading" @click="loadHistory">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
        <a-button @click="resetQuery">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
        <a-button :disabled="!records.length" @click="exportCsv">
          <template #icon><DownloadOutlined /></template>
          导出 CSV
        </a-button>
      </div>
    </a-card>

    <section class="history-grid">
      <a-card class="chart-card" title="温度趋势" :bordered="false">
        <VChart v-if="records.length" :option="temperatureOption" autoresize />
        <a-empty v-else description="暂无温度趋势数据" />
      </a-card>
      <a-card class="chart-card" title="振动趋势" :bordered="false">
        <VChart v-if="records.length" :option="vibrationOption" autoresize />
        <a-empty v-else description="暂无振动趋势数据" />
      </a-card>
      <a-card class="chart-card" title="功率趋势" :bordered="false">
        <VChart v-if="records.length" :option="powerOption" autoresize />
        <a-empty v-else description="暂无功率趋势数据" />
      </a-card>
      <a-card class="chart-card" title="转速趋势" :bordered="false">
        <VChart v-if="records.length" :option="rpmOption" autoresize />
        <a-empty v-else description="暂无转速趋势数据" />
      </a-card>
    </section>

    <a-card class="table-card" title="历史遥测明细" :bordered="false">
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="tableRecords"
        :loading="loading"
        :pagination="{
          pageSize: 20,
          pageSizeOptions: ['20', '50', '100'],
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`
        }"
        :scroll="{ x: 1080, y: 420 }"
        row-key="_rowKey"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'timestamp'">
            {{ formatDateTime(record.timestamp) }}
          </template>
          <template v-else>
            {{ metricValue(record[column.dataIndex], column.precision) }}
          </template>
        </template>
      </a-table>
      <a-empty v-else description="请选择设备并查询历史数据" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'

import { deviceApi, monitorApi } from '@/api/business'
import { useThemeStore } from '@/stores/theme'
import type { Device, TelemetryHistoryPoint } from '@/types/business'
import { formatDateTime } from '@/utils/format'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const columns = [
  { title: '采集时间', key: 'timestamp', width: 180, fixed: 'left' },
  { title: '转速 (rpm)', dataIndex: 'rpm', key: 'rpm', width: 120, precision: 0 },
  { title: '电流 (A)', dataIndex: 'current', key: 'current', width: 110, precision: 2 },
  { title: '电压 (V)', dataIndex: 'voltage', key: 'voltage', width: 110, precision: 1 },
  { title: '功率 (kW)', dataIndex: 'power', key: 'power', width: 120, precision: 2 },
  { title: '电机温度 (°C)', dataIndex: 'motorTemperature', key: 'motorTemperature', width: 140, precision: 1 },
  { title: '轴承温度 (°C)', dataIndex: 'bearingTemperature', key: 'bearingTemperature', width: 140, precision: 1 },
  { title: '振动 (mm/s)', dataIndex: 'vibration', key: 'vibration', width: 130, precision: 2 }
]

const themeStore = useThemeStore()
const loading = ref(false)
const devices = ref<Device[]>([])
const records = ref<TelemetryHistoryPoint[]>([])
const range = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive({
  deviceId: undefined as string | undefined,
  limit: 500
})

const deviceOptions = computed(() =>
  devices.value.map((item) => ({
    label: `${item.deviceName}（${item.deviceId}）`,
    value: item.deviceId
  }))
)

const tableRecords = computed(() =>
  records.value.map((item, index) => ({ ...item, _rowKey: `${item.timestamp}-${index}` }))
)

const xAxis = computed(() => records.value.map((item) => formatDateTime(item.timestamp)))

const temperatureOption = computed(() =>
  lineOption(['电机温度', '轴承温度'], ['#d89614', '#e5484d'], [
    records.value.map((item) => item.motorTemperature ?? null),
    records.value.map((item) => item.bearingTemperature ?? null)
  ])
)

const vibrationOption = computed(() =>
  lineOption(['振动'], ['#1677ff'], [records.value.map((item) => item.vibration ?? null)])
)

const powerOption = computed(() =>
  lineOption(['功率'], ['#22a06b'], [records.value.map((item) => item.power ?? null)])
)

const rpmOption = computed(() =>
  lineOption(['转速'], ['#722ed1'], [records.value.map((item) => item.rpm ?? null)])
)

function lineOption(names: string[], colors: string[], data: Array<Array<number | null>>) {
  const dark = themeStore.isDark
  const labelColor = dark ? '#91a4bd' : '#667085'
  const gridColor = dark ? '#233149' : '#e9edf3'
  return {
    animation: false,
    color: colors,
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      right: 0,
      data: names,
      textStyle: { color: labelColor }
    },
    grid: { left: 52, right: 24, top: 46, bottom: 42 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxis.value,
      axisLabel: {
        color: labelColor,
        formatter: (value: string) => value.slice(5, 16)
      },
      axisLine: { lineStyle: { color: gridColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: labelColor },
      splitLine: { lineStyle: { color: gridColor } }
    },
    series: names.map((name, index) => ({
      name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: data[index],
      lineStyle: { width: 2 },
      areaStyle: { opacity: 0.08 }
    }))
  }
}

function filterDevice(input: string, option?: { label: string }) {
  return option?.label.toLowerCase().includes(input.toLowerCase()) ?? false
}

function metricValue(value: unknown, precision = 1) {
  if (value == null || value === '') return '-'
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(precision) : String(value)
}

async function loadDevices() {
  devices.value = (await deviceApi.page({ page: 1, size: 100 })).records
  if (!query.deviceId && devices.value.length) {
    query.deviceId = devices.value[0].deviceId
  }
}

async function loadHistory() {
  if (!query.deviceId) {
    records.value = []
    message.info('请先选择设备')
    return
  }

  loading.value = true
  try {
    records.value = await monitorApi.history({
      deviceId: query.deviceId,
      start: range.value?.[0]?.format(),
      end: range.value?.[1]?.format(),
      limit: query.limit
    })
  } finally {
    loading.value = false
  }
}

async function resetQuery() {
  range.value = null
  query.deviceId = devices.value[0]?.deviceId
  await loadHistory()
}

function csvCell(value: unknown) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function exportCsv() {
  if (!records.value.length) {
    message.info('当前没有可导出的历史数据')
    return
  }

  const headers = ['采集时间', '转速', '电流', '电压', '功率', '电机温度', '轴承温度', '振动']
  const rows = records.value.map((item) => [
    formatDateTime(item.timestamp),
    item.rpm,
    item.current,
    item.voltage,
    item.power,
    item.motorTemperature,
    item.bearingTemperature,
    item.vibration
  ])
  const content = [headers, ...rows].map((row) => row.map(csvCell).join(',')).join('\r\n')
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `历史数据-${query.deviceId}-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  await loadDevices()
  await loadHistory()
})
</script>

<style scoped>
.history-page {
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

.page-heading p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.record-summary {
  color: var(--muted);
  font-size: 13px;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(320px, 1.5fr) auto auto auto;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card .echarts {
  width: 100%;
  height: 280px;
}

.chart-card :deep(.ant-empty) {
  padding: 76px 0;
}

.table-card :deep(.ant-empty) {
  padding: 96px 0;
}

@media (max-width: 1180px) {
  .filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .history-grid,
  .filter-bar {
    grid-template-columns: 1fr;
  }

  .page-heading {
    flex-direction: column;
  }
}
</style>
