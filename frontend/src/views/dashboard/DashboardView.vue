<template>
  <div class="dashboard">
    <section class="panel map-panel">
      <div class="panel-title">设备地图</div>
      <div v-if="mapDevices.length" class="map-canvas">
        <div v-for="device in mapDevices" :key="device.deviceId" class="map-point" :class="device.status" :style="pointStyle(device)">
          <span></span>
          <em>{{ device.deviceName }}</em>
        </div>
      </div>
      <el-empty v-else description="暂无真实定位坐标，请在设备管理中维护位置" />
      <div class="map-legend">
        <span><i class="online"></i>在线设备 {{ statusCount.online }}</span>
        <span><i class="offline"></i>离线设备 {{ statusCount.offline }}</span>
        <span><i class="alarm"></i>告警设备 {{ statusCount.alarm }}</span>
      </div>
    </section>

    <section class="panel chart-card">
      <div class="panel-title">实时运行指标</div>
      <VChart :option="metricOption" autoresize />
    </section>

    <section class="panel list-panel">
      <div class="panel-title">设备状态列表</div>
      <div v-for="device in mapDevices" :key="device.id" class="list-row">
        <span :class="['status-dot', device.status === 'alarm' ? 'danger' : device.status === 'online' ? '' : 'warning']"></span>
        <div><strong>{{ device.deviceName }}</strong><p>{{ device.installLocation || device.address || device.deviceId }}</p></div>
        <el-tag :type="device.status === 'alarm' ? 'danger' : device.status === 'online' ? 'success' : 'info'" effect="dark">{{ statusLabel(device.status) }}</el-tag>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { deviceApi } from '@/api/business'
import type { Device } from '@/types/business'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const devices = ref<Device[]>([])
const mapDevices = computed(() => devices.value
  .filter((item) => item.longitude != null && item.latitude != null)
  .slice(0, 12))
const statusCount = computed(() => ({
  online: devices.value.filter((item) => item.status === 'online').length,
  offline: devices.value.filter((item) => item.status === 'offline').length,
  alarm: devices.value.filter((item) => item.status === 'alarm').length
}))

const metricOption = computed(() => ({
  color: ['#37d889', '#ffb84d', '#ff5d73'],
  tooltip: { trigger: 'axis' },
  legend: { textStyle: { color: '#9db8d8' } },
  grid: { left: 42, right: 18, top: 42, bottom: 36 },
  xAxis: { type: 'category', data: ['在线', '离线', '告警'], axisLabel: { color: '#89a5c4' } },
  yAxis: { type: 'value', axisLabel: { color: '#89a5c4' }, splitLine: { lineStyle: { color: 'rgba(84, 176, 255, 0.12)' } } },
  series: [{ name: '设备数', type: 'bar', data: [statusCount.value.online, statusCount.value.offline, statusCount.value.alarm] }]
}))

function statusLabel(value: string) {
  return { online: '在线', offline: '离线', alarm: '告警', maintenance: '维护', disabled: '禁用' }[value] || value
}

function pointStyle(device: Device) {
  const longitude = Number(device.longitude)
  const latitude = Number(device.latitude)
  return {
    left: `${Math.min(88, Math.max(8, ((longitude + 180) / 360) * 100))}%`,
    top: `${Math.min(84, Math.max(12, (1 - (latitude + 90) / 180) * 100))}%`
  }
}

onMounted(async () => {
  devices.value = (await deviceApi.page({ page: 1, size: 100 })).records
})
</script>

<style scoped>
.dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 16px;
}
.map-panel,
.chart-card,
.list-panel {
  padding: 16px;
}
.map-panel {
  min-height: 460px;
}
.map-canvas {
  position: relative;
  height: 360px;
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background:
    linear-gradient(rgba(77, 184, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(77, 184, 255, 0.08) 1px, transparent 1px),
    radial-gradient(circle at 36% 42%, rgba(55, 216, 137, 0.12), transparent 26%),
    rgba(8, 20, 36, 0.82);
  background-size: 36px 36px, 36px 36px, 100% 100%, 100% 100%;
}
.map-point {
  position: absolute;
  transform: translate(-50%, -50%);
}
.map-point span {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 18px rgba(55, 216, 137, 0.9);
}
.map-point.offline span {
  background: #8aa6c8;
  box-shadow: 0 0 14px rgba(138, 166, 200, 0.8);
}
.map-point.alarm span {
  background: var(--danger);
  box-shadow: 0 0 20px rgba(255, 93, 115, 0.95);
}
.map-point em {
  position: absolute;
  top: 14px;
  left: 12px;
  min-width: 92px;
  color: #d8edff;
  font-size: 12px;
  font-style: normal;
}
.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 14px;
  color: var(--muted);
}
.map-legend i {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 6px;
  border-radius: 50%;
  background: var(--success);
}
.map-legend .offline {
  background: #8aa6c8;
}
.map-legend .alarm {
  background: var(--danger);
}
.chart-card .echarts {
  width: 100%;
  height: 360px;
}
.list-panel {
  grid-column: 1 / -1;
}
.list-row {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  border-top: 1px solid rgba(84, 176, 255, 0.12);
}
.list-row p {
  margin: 4px 0 0;
  color: var(--muted);
}
@media (max-width: 1000px) {
  .dashboard {
    grid-template-columns: 1fr;
  }
}
</style>
