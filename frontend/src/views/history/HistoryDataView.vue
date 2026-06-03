<template>
  <section class="history-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">历史数据</div>
        <p>按设备查询遥测历史曲线，支持转速、温度、振动、功率趋势分析</p>
      </div>
      <div class="actions">
        <el-select v-model="query.deviceId" filterable placeholder="选择设备" @change="loadHistory">
          <el-option v-for="item in devices" :key="item.deviceId" :label="`${item.deviceName} (${item.deviceId})`" :value="item.deviceId" />
        </el-select>
        <el-date-picker
          v-model="range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ssZ"
          @change="loadHistory"
        />
        <el-button :icon="Search" @click="loadHistory">查询</el-button>
      </div>
    </div>

    <div class="history-grid">
      <div class="chart panel">
        <div class="panel-title">温度趋势图</div>
        <VChart :option="temperatureOption" autoresize />
      </div>
      <div class="chart panel">
        <div class="panel-title">振动趋势图</div>
        <VChart :option="vibrationOption" autoresize />
      </div>
      <div class="chart panel">
        <div class="panel-title">功率趋势图</div>
        <VChart :option="powerOption" autoresize />
      </div>
      <div class="chart panel">
        <div class="panel-title">转速趋势图</div>
        <VChart :option="rpmOption" autoresize />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'

import { deviceApi, monitorApi } from '@/api/business'
import type { Device, TelemetryHistoryPoint } from '@/types/business'
import { formatDateTime } from '@/utils/format'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const devices = ref<Device[]>([])
const records = ref<TelemetryHistoryPoint[]>([])
const range = ref<[string, string] | null>(null)
const query = reactive({
  deviceId: '',
  limit: 500
})

const xAxis = computed(() => records.value.map((item) => formatDateTime(item.timestamp)))

const temperatureOption = computed(() =>
  lineOption(['电机温度', '轴承温度'], ['#ffb84d', '#ff5d73'], [
    records.value.map((item) => item.motorTemperature || 0),
    records.value.map((item) => item.bearingTemperature || 0)
  ])
)

const vibrationOption = computed(() =>
  lineOption(['振动'], ['#4db8ff'], [records.value.map((item) => item.vibration || 0)])
)

const powerOption = computed(() =>
  lineOption(['功率'], ['#37d889'], [records.value.map((item) => item.power || 0)])
)

const rpmOption = computed(() =>
  lineOption(['转速'], ['#9b8cff'], [records.value.map((item) => item.rpm || 0)])
)

function lineOption(names: string[], colors: string[], data: number[][]) {
  return {
    color: colors,
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#9db8d8' } },
    grid: { left: 42, right: 20, top: 42, bottom: 36 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxis.value,
      axisLabel: { color: '#89a5c4' },
      axisLine: { lineStyle: { color: '#315777' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#89a5c4' },
      splitLine: { lineStyle: { color: 'rgba(84, 176, 255, 0.12)' } }
    },
    series: names.map((name, index) => ({
      name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: data[index],
      lineStyle: { width: 3 },
      areaStyle: { opacity: 0.12 }
    }))
  }
}

async function loadDevices() {
  const page = await deviceApi.page({ page: 1, size: 100 })
  devices.value = page.records
  if (!query.deviceId && devices.value.length > 0) {
    query.deviceId = devices.value[0].deviceId
  }
}

async function loadHistory() {
  if (!query.deviceId) {
    records.value = []
    return
  }
  records.value = await monitorApi.history({
    deviceId: query.deviceId,
    start: range.value?.[0],
    end: range.value?.[1],
    limit: query.limit
  })
}

onMounted(async () => {
  await loadDevices()
  await loadHistory()
})
</script>

<style scoped>
.history-page {
  min-height: calc(100vh - 126px);
  padding: 18px;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart {
  min-height: 310px;
  padding: 16px;
}

.chart .echarts {
  width: 100%;
  height: 240px;
}

@media (max-width: 1100px) {
  .history-grid {
    grid-template-columns: 1fr;
  }
}
</style>
