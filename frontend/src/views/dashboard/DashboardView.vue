<template>
  <div class="dashboard-page">
    <div class="page-toolbar">
      <div>
        <h2>实时监控总览</h2>
        <p>汇总盒子通信、设备运行指标与最新告警</p>
      </div>
      <div class="toolbar-actions">
        <a-select
          v-model:value="selectedDeviceId"
          show-search
          allow-clear
          placeholder="选择重点设备"
          :options="deviceOptions"
          :filter-option="filterDevice"
          style="width: 220px"
          @change="loadHistory"
        />
        <a-button :loading="refreshing" @click="loadDashboard">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <section class="stat-grid">
      <a-card v-for="item in statistics" :key="item.label" class="stat-card" :bordered="false">
        <a-statistic :title="item.label" :value="item.value">
          <template #prefix>
            <span class="stat-indicator" :class="item.tone"></span>
          </template>
          <template #suffix><span class="stat-unit">{{ item.unit }}</span></template>
        </a-statistic>
        <div class="stat-note">{{ item.note }}</div>
      </a-card>
    </section>

    <section class="dashboard-grid upper-grid">
      <a-card class="fleet-card" title="设备运行状态矩阵" :bordered="false">
        <template #extra>
          <a-button type="link" size="small" @click="router.push('/devices')">查看设备</a-button>
        </template>

        <div v-if="latestDevices.length" class="device-matrix">
          <button
            v-for="device in latestDevices"
            :key="device.deviceId"
            type="button"
            class="device-cell"
            :class="{ selected: selectedDeviceId === device.deviceId }"
            @click="selectDevice(device.deviceId)"
          >
            <a-badge :status="badgeStatus(device.status)" />
            <span class="device-name">{{ device.deviceName || device.deviceId }}</span>
            <strong>{{ metricText(device.rpm, 'rpm') }}</strong>
            <small>{{ statusLabel(device.status) }}</small>
          </button>
        </div>
        <a-empty v-else description="暂无设备实时数据" />

        <div class="matrix-legend">
          <span><i class="online"></i>在线</span>
          <span><i class="alarm"></i>告警</span>
          <span><i class="offline"></i>离线</span>
        </div>
      </a-card>

      <a-card class="metric-card" title="关键运行指标" :bordered="false">
        <template #extra>
          <a-tag :color="selectedMetric?.status === 'alarm' ? 'error' : selectedMetric ? 'success' : 'default'">
            {{ selectedMetric ? statusLabel(selectedMetric.status) : '未选择' }}
          </a-tag>
        </template>

        <div class="selected-device">
          <strong>{{ selectedMetric?.deviceName || '请选择设备' }}</strong>
          <span>{{ selectedMetric?.deviceId || '-' }}</span>
        </div>
        <div class="metric-grid">
          <div v-for="metric in keyMetrics" :key="metric.label" class="metric-item">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.unit }}</small>
          </div>
        </div>
        <div class="communication-summary">
          <div>
            <span>最后数据时间</span>
            <strong>{{ formatDateTime(selectedMetric?.timestamp || selectedMetric?.receivedAt) }}</strong>
          </div>
          <div>
            <span>通信状态</span>
            <a-badge :status="selectedMetric ? 'success' : 'default'" :text="selectedMetric ? '数据接收正常' : '暂无实时数据'" />
          </div>
        </div>
      </a-card>
    </section>

    <section class="dashboard-grid lower-grid">
      <a-card class="trend-card" title="实时运行趋势" :bordered="false">
        <template #extra>
          <span class="card-extra">最近 {{ history.length }} 条数据</span>
        </template>
        <VChart v-if="history.length" :option="trendOption" autoresize />
        <a-empty v-else description="所选设备暂无历史数据" />
      </a-card>

      <a-card class="alarm-card" title="最新告警" :bordered="false">
        <template #extra>
          <a-button type="link" size="small" @click="router.push('/alarms')">全部告警</a-button>
        </template>
        <a-list :data-source="alarms" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #avatar>
                  <span class="alarm-dot" :class="item.alarmLevel"></span>
                </template>
                <template #title>
                  <div class="alarm-title">
                    <span>{{ item.alarmMessage }}</span>
                    <a-tag :color="alarmColor(item.alarmLevel)">{{ alarmLevelLabel(item.alarmLevel) }}</a-tag>
                  </div>
                </template>
                <template #description>
                  {{ item.deviceId }} · {{ formatDateTime(item.occurredAt) }}
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-if="!alarms.length" description="暂无告警记录" />
      </a-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'

import { alarmApi, deviceApi, monitorApi } from '@/api/business'
import { useThemeStore } from '@/stores/theme'
import type { Alarm, Device, RealtimeMetric, RealtimeOverview, TelemetryHistoryPoint } from '@/types/business'
import { formatDateTime } from '@/utils/format'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const themeStore = useThemeStore()
const refreshing = ref(false)
const selectedDeviceId = ref<string>()
const devices = ref<Device[]>([])
const latestDevices = ref<RealtimeMetric[]>([])
const alarms = ref<Alarm[]>([])
const history = ref<TelemetryHistoryPoint[]>([])
const overview = ref<RealtimeOverview>({
  onlineGateways: 0,
  offlineGateways: 0,
  onlineDevices: 0,
  offlineDevices: 0,
  alarmDevices: 0,
  activeAlarms: 0,
  latestDevices: []
})

let refreshTimer: number | undefined

const selectedMetric = computed(() =>
  latestDevices.value.find((item) => item.deviceId === selectedDeviceId.value)
)

const deviceOptions = computed(() =>
  devices.value.map((item) => ({
    label: `${item.deviceName}（${item.deviceId}）`,
    value: item.deviceId
  }))
)

const statistics = computed(() => [
  { label: '在线盒子', value: overview.value.onlineGateways, unit: '台', tone: 'success', note: `离线 ${overview.value.offlineGateways} 台` },
  { label: '在线设备', value: overview.value.onlineDevices, unit: '台', tone: 'success', note: `离线 ${overview.value.offlineDevices} 台` },
  { label: '告警设备', value: overview.value.alarmDevices, unit: '台', tone: 'danger', note: '当前存在设备告警' },
  { label: '活动告警', value: overview.value.activeAlarms, unit: '条', tone: 'warning', note: '待确认与处理中' },
  { label: '设备总数', value: overview.value.onlineDevices + overview.value.offlineDevices, unit: '台', tone: 'primary', note: '平台已接入设备' },
  {
    label: '设备在线率',
    value: deviceOnlineRate.value,
    unit: '%',
    tone: deviceOnlineRate.value >= 90 ? 'success' : 'warning',
    note: '按实时通信状态统计'
  }
])

const deviceOnlineRate = computed(() => {
  const total = overview.value.onlineDevices + overview.value.offlineDevices
  return total ? Number(((overview.value.onlineDevices / total) * 100).toFixed(1)) : 0
})

const keyMetrics = computed(() => [
  { label: '转速', value: metricValue(selectedMetric.value?.rpm, 0), unit: 'rpm' },
  { label: '电机温度', value: metricValue(selectedMetric.value?.motorTemperature, 1), unit: '°C' },
  { label: '振动', value: metricValue(selectedMetric.value?.vibration, 2), unit: 'mm/s' },
  { label: '功率', value: metricValue(selectedMetric.value?.power, 2), unit: 'kW' },
  { label: '电流', value: metricValue(selectedMetric.value?.current, 2), unit: 'A' },
  { label: '电压', value: metricValue(selectedMetric.value?.voltage, 1), unit: 'V' }
])

const trendOption = computed(() => {
  const dark = themeStore.isDark
  const labelColor = dark ? '#91a4bd' : '#667085'
  const gridColor = dark ? '#233149' : '#e9edf3'
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
    grid: { left: 50, right: 50, top: 46, bottom: 32 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: history.value.map((item) => formatDateTime(item.timestamp).slice(11, 16)),
      axisLabel: { color: labelColor },
      axisLine: { lineStyle: { color: gridColor } }
    },
    yAxis: [
      {
        type: 'value',
        name: 'rpm',
        axisLabel: { color: labelColor },
        splitLine: { lineStyle: { color: gridColor } }
      },
      {
        type: 'value',
        name: '温度/振动/功率',
        axisLabel: { color: labelColor },
        splitLine: { show: false }
      }
    ],
    series: [
      { name: '转速', type: 'line', symbol: 'none', smooth: true, data: history.value.map((item) => item.rpm ?? null) },
      { name: '电机温度', type: 'line', yAxisIndex: 1, symbol: 'none', smooth: true, data: history.value.map((item) => item.motorTemperature ?? null) },
      { name: '振动', type: 'line', yAxisIndex: 1, symbol: 'none', smooth: true, data: history.value.map((item) => item.vibration ?? null) },
      { name: '功率', type: 'line', yAxisIndex: 1, symbol: 'none', smooth: true, data: history.value.map((item) => item.power ?? null) }
    ]
  }
})

function metricValue(value?: number, precision = 1) {
  return value == null ? '-' : Number(value).toFixed(precision)
}

function metricText(value?: number, unit = '') {
  return value == null ? '-' : `${Number(value).toFixed(0)} ${unit}`
}

function statusLabel(value?: string) {
  return {
    online: '在线',
    running: '运行',
    offline: '离线',
    alarm: '告警',
    warning: '预警',
    maintenance: '维护',
    disabled: '禁用'
  }[value || ''] || value || '未知'
}

function badgeStatus(value?: string) {
  if (value === 'alarm') return 'error'
  if (value === 'online' || value === 'running') return 'success'
  if (value === 'warning' || value === 'maintenance') return 'warning'
  return 'default'
}

function alarmLevelLabel(value: string) {
  return { critical: '严重', warning: '警告', normal: '一般' }[value] || value
}

function alarmColor(value: string) {
  return value === 'critical' ? 'error' : value === 'warning' ? 'warning' : 'default'
}

function filterDevice(input: string, option?: { label: string }) {
  return option?.label.toLowerCase().includes(input.toLowerCase()) ?? false
}

async function selectDevice(deviceId: string) {
  selectedDeviceId.value = deviceId
  await loadHistory()
}

async function loadHistory() {
  if (!selectedDeviceId.value) {
    history.value = []
    return
  }
  history.value = await monitorApi.history({ deviceId: selectedDeviceId.value, limit: 48 })
}

async function loadDashboard() {
  refreshing.value = true
  try {
    const [overviewData, devicePage, alarmPage] = await Promise.all([
      monitorApi.overview(),
      deviceApi.page({ page: 1, size: 100 }),
      alarmApi.page({ page: 1, size: 6 })
    ])
    overview.value = overviewData
    latestDevices.value = overviewData.latestDevices || []
    devices.value = devicePage.records
    alarms.value = alarmPage.records

    if (!selectedDeviceId.value) {
      selectedDeviceId.value = latestDevices.value[0]?.deviceId || devices.value[0]?.deviceId
    }
    await loadHistory()
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await loadDashboard()
  refreshTimer = window.setInterval(loadDashboard, 30_000)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 16px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-toolbar h2 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
}

.page-toolbar p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.stat-card :deep(.ant-card-body) {
  padding: 16px;
}

.stat-card :deep(.ant-statistic-title) {
  color: var(--muted);
}

.stat-card :deep(.ant-statistic-content) {
  color: var(--text);
  font-size: 27px;
}

.stat-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  border-radius: 50%;
  background: var(--primary);
  vertical-align: middle;
}

.stat-indicator.success {
  background: var(--success);
}

.stat-indicator.warning {
  background: var(--warning);
}

.stat-indicator.danger {
  background: var(--danger);
}

.stat-unit {
  color: var(--muted);
  font-size: 13px;
}

.stat-note {
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 16px;
}

.device-matrix {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.device-cell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 5px 7px;
  min-height: 74px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.device-cell:hover,
.device-cell.selected {
  border-color: var(--primary);
  background: var(--hover);
}

.device-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-cell strong {
  grid-column: 2;
  font-size: 16px;
}

.device-cell small {
  grid-column: 2;
  color: var(--muted);
}

.matrix-legend {
  display: flex;
  gap: 18px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 12px;
}

.matrix-legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 5px;
  border-radius: 50%;
}

.matrix-legend .online {
  background: var(--success);
}

.matrix-legend .alarm {
  background: var(--danger);
}

.matrix-legend .offline {
  background: var(--muted);
}

.selected-device {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.selected-device strong {
  color: var(--text);
  font-size: 16px;
}

.selected-device span {
  color: var(--muted);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid var(--line);
  border-left: 1px solid var(--line);
}

.metric-item {
  min-height: 74px;
  padding: 11px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.metric-item span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.metric-item strong {
  display: inline-block;
  margin-top: 5px;
  color: var(--text);
  font-size: 21px;
}

.metric-item small {
  margin-left: 4px;
  color: var(--muted);
}

.communication-summary {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.communication-summary > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--muted);
  font-size: 12px;
}

.communication-summary strong {
  color: var(--text);
  font-weight: 500;
}

.trend-card :deep(.ant-card-body) {
  height: 330px;
}

.trend-card .echarts {
  width: 100%;
  height: 100%;
}

.card-extra {
  color: var(--muted);
  font-size: 12px;
}

.alarm-card :deep(.ant-list-item) {
  padding-inline: 0;
}

.alarm-dot {
  display: block;
  width: 9px;
  height: 9px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--muted);
}

.alarm-dot.critical {
  background: var(--danger);
}

.alarm-dot.warning {
  background: var(--warning);
}

.alarm-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.alarm-title > span {
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .device-matrix {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-toolbar,
  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .device-matrix {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
