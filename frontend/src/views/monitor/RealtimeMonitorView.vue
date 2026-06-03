<template>
  <section class="monitor-page">
    <div class="monitor-stats">
      <div v-for="item in stats" :key="item.label" class="panel stat-card">
        <span>{{ item.label }}</span>
        <strong :class="item.type">{{ item.value }}</strong>
      </div>
    </div>

    <div class="monitor-grid">
      <div class="panel chart-card">
        <div class="panel-title">实时运行指标</div>
        <VChart :option="metricChartOption" autoresize />
      </div>

      <div class="panel table-card">
        <div class="panel-title">设备最新数据</div>
        <el-table v-loading="loading" :data="latestDevices" height="360">
          <el-table-column prop="deviceId" label="设备编号" min-width="130" />
          <el-table-column prop="deviceName" label="设备名称" min-width="140" />
          <el-table-column prop="rpm" label="转速" min-width="90" />
          <el-table-column prop="motorTemperature" label="电机温度" min-width="100" />
          <el-table-column prop="vibration" label="振动" min-width="90" />
          <el-table-column prop="power" label="功率" min-width="90" />
          <el-table-column prop="status" label="状态" min-width="90">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="timestamp" label="采集时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.timestamp) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'

import { monitorApi } from '@/api/business'
import type { RealtimeMetric, RealtimeOverview } from '@/types/business'
import { formatDateTime } from '@/utils/format'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const loading = ref(false)
const overview = ref<RealtimeOverview | null>(null)
const latestDevices = ref<RealtimeMetric[]>([])
let timer: number | undefined

const stats = computed(() => [
  { label: '在线盒子', value: overview.value?.onlineGateways || 0, type: 'success' },
  { label: '离线盒子', value: overview.value?.offlineGateways || 0, type: 'muted-value' },
  { label: '在线设备', value: overview.value?.onlineDevices || 0, type: 'success' },
  { label: '离线设备', value: overview.value?.offlineDevices || 0, type: 'muted-value' },
  { label: '告警设备', value: overview.value?.alarmDevices || 0, type: 'danger' },
  { label: '活动告警', value: overview.value?.activeAlarms || 0, type: 'danger' }
])

const metricChartOption = computed(() => {
  const devices = latestDevices.value.slice(0, 8)
  return {
    color: ['#4db8ff', '#ffb84d', '#ff5d73', '#37d889'],
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#9db8d8' } },
    grid: { left: 42, right: 20, top: 42, bottom: 36 },
    xAxis: {
      type: 'category',
      data: devices.map((item) => item.deviceId),
      axisLabel: { color: '#89a5c4' },
      axisLine: { lineStyle: { color: '#315777' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#89a5c4' },
      splitLine: { lineStyle: { color: 'rgba(84, 176, 255, 0.12)' } }
    },
    series: [
      { name: '转速', type: 'bar', data: devices.map((item) => item.rpm || 0) },
      { name: '温度', type: 'bar', data: devices.map((item) => item.motorTemperature || item.bearingTemperature || 0) },
      { name: '振动', type: 'bar', data: devices.map((item) => item.vibration || 0) },
      { name: '功率', type: 'bar', data: devices.map((item) => item.power || 0) }
    ]
  }
})

function statusLabel(value: string) {
  return { running: '运行', normal: '正常', warning: '告警', critical: '严重', offline: '离线', fault: '故障' }[value] || value
}

function statusTag(value: string) {
  return value === 'critical' || value === 'fault' ? 'danger' : value === 'warning' ? 'warning' : value === 'offline' ? 'info' : 'success'
}

async function loadData() {
  loading.value = true
  try {
    overview.value = await monitorApi.overview()
    latestDevices.value = overview.value.latestDevices
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  timer = window.setInterval(loadData, 10000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<style scoped>
.monitor-page {
  display: grid;
  gap: 16px;
}

.monitor-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  min-height: 96px;
  padding: 16px;
}

.stat-card span {
  color: var(--muted);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  font-size: 32px;
}

.monitor-grid {
  display: grid;
  gap: 16px;
}

.chart-card,
.table-card {
  padding: 16px;
}

.chart-card .echarts {
  width: 100%;
  height: 360px;
}

.success {
  color: var(--success);
}

.danger {
  color: var(--danger);
}

.muted-value {
  color: #9fb8d8;
}

@media (max-width: 1100px) {
  .monitor-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .monitor-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
