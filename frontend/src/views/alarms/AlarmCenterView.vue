<template>
  <div class="alarm-page">
    <a-card class="filter-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>告警记录</h2>
          <p>查询设备告警，跟踪确认、恢复与关闭状态</p>
        </div>
        <span class="record-summary">共 {{ total }} 条告警</span>
      </div>

      <div class="filter-bar">
        <a-select
          v-model:value="query.alarmLevel"
          allow-clear
          placeholder="告警等级"
          :options="levelOptions"
        />
        <a-select
          v-model:value="query.status"
          allow-clear
          placeholder="告警状态"
          :options="statusOptions"
        />
        <a-input
          v-model:value="query.keyword"
          allow-clear
          placeholder="搜索设备编号、告警内容或编码"
          @press-enter="resetAndLoad"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-button type="primary" :loading="loading" @click="resetAndLoad">查询</a-button>
        <a-button @click="resetQuery">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </div>
    </a-card>

    <a-card class="alarm-chart" title="告警等级分布" :bordered="false">
      <VChart v-if="records.length" :option="levelOption" autoresize />
      <a-empty v-else description="当前筛选条件下暂无告警分布" />
    </a-card>

    <a-card class="alarm-table" title="告警明细" :bordered="false">
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1320, y: 'calc(100vh - 520px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'alarm'">
            <div class="alarm-content">
              <strong>{{ record.alarmMessage }}</strong>
              <span>{{ record.alarmType }} · {{ record.alarmCode || '无告警编码' }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'gatewayId'">
            {{ textOrDash(record.gatewayId) }}
          </template>
          <template v-else-if="column.key === 'alarmLevel'">
            <a-tag :color="levelColor(record.alarmLevel)">
              {{ levelLabel(record.alarmLevel) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">
              {{ statusLabel(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'occurredAt'">
            {{ formatDateTime(record.occurredAt) }}
          </template>
          <template v-else-if="column.key === 'recoveredAt'">
            {{ formatDateTime(record.recoveredAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button
                type="link"
                size="small"
                :disabled="record.acknowledged || record.status === 'closed'"
                @click="acknowledge(record)"
              >
                确认告警
              </a-button>
              <a-button
                type="link"
                danger
                size="small"
                :disabled="record.status === 'closed'"
                @click="closeAlarm(record)"
              >
                关闭
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无告警记录" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条告警记录</span>
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.size"
          :page-size-options="['10', '20', '50']"
          :total="total"
          show-size-changer
          show-quick-jumper
          @change="loadData"
          @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'

import { alarmApi } from '@/api/business'
import { useThemeStore } from '@/stores/theme'
import type { Alarm } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const columns = [
  { title: '告警编号', dataIndex: 'alarmId', key: 'alarmId', width: 150, fixed: 'left' },
  { title: '设备编号', dataIndex: 'deviceId', key: 'deviceId', width: 140 },
  { title: '盒子编号', key: 'gatewayId', width: 140 },
  { title: '告警内容', key: 'alarm', width: 300 },
  { title: '告警等级', key: 'alarmLevel', width: 110 },
  { title: '告警状态', key: 'status', width: 110 },
  { title: '发生时间', key: 'occurredAt', width: 180 },
  { title: '恢复时间', key: 'recoveredAt', width: 180 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]

const levelOptions = [
  { label: '严重', value: 'critical' },
  { label: '警告', value: 'warning' },
  { label: '提示', value: 'normal' }
]

const statusOptions = [
  { label: '未处理', value: 'active' },
  { label: '处理中', value: 'acknowledged' },
  { label: '已恢复', value: 'recovered' },
  { label: '已关闭', value: 'closed' }
]

const themeStore = useThemeStore()
const loading = ref(false)
const records = ref<Alarm[]>([])
const total = ref(0)
const query = reactive({
  keyword: undefined as string | undefined,
  alarmLevel: undefined as string | undefined,
  status: undefined as string | undefined,
  page: 1,
  size: 10
})

const levelOption = computed(() => {
  const dark = themeStore.isDark
  const labelColor = dark ? '#91a4bd' : '#667085'
  const critical = records.value.filter((item) => item.alarmLevel === 'critical').length
  const warning = records.value.filter((item) => item.alarmLevel === 'warning').length
  const info = records.value.filter((item) => item.alarmLevel === 'normal' || item.alarmLevel === 'info').length
  return {
    animation: false,
    color: ['#e5484d', '#d89614', '#1677ff'],
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      right: 24,
      top: 'center',
      textStyle: { color: labelColor }
    },
    series: [
      {
        name: '告警等级',
        type: 'pie',
        center: ['42%', '50%'],
        radius: ['48%', '72%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: dark ? '#111c2e' : '#ffffff',
          borderWidth: 3
        },
        label: {
          color: labelColor,
          formatter: '{b}\n{c} 条'
        },
        data: [
          { name: '严重', value: critical },
          { name: '警告', value: warning },
          { name: '提示', value: info }
        ]
      }
    ]
  }
})

function levelLabel(value: string) {
  return {
    critical: '严重',
    warning: '警告',
    normal: '提示',
    info: '提示'
  }[value] || value
}

function levelColor(value: string) {
  if (value === 'critical') return 'error'
  if (value === 'warning') return 'warning'
  return 'processing'
}

function statusLabel(value: string) {
  return {
    active: '未处理',
    acknowledged: '处理中',
    recovered: '已处理',
    closed: '已处理'
  }[value] || value
}

function statusColor(value: string) {
  if (value === 'active') return 'error'
  if (value === 'acknowledged') return 'warning'
  if (value === 'recovered' || value === 'closed') return 'success'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await alarmApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  Object.assign(query, {
    keyword: undefined,
    alarmLevel: undefined,
    status: undefined,
    page: 1
  })
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

async function acknowledge(row: Alarm) {
  await alarmApi.acknowledge(row.id)
  message.success('告警已确认')
  await loadData()
}

async function closeAlarm(row: Alarm) {
  await alarmApi.close(row.id)
  message.success('告警已关闭')
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.alarm-page {
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
  grid-template-columns: 150px 150px minmax(260px, 1fr) auto auto;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.alarm-chart .echarts {
  width: 100%;
  height: 260px;
}

.alarm-chart :deep(.ant-empty) {
  padding: 70px 0;
}

.alarm-table :deep(.ant-empty) {
  padding: 96px 0;
}

.alarm-content strong,
.alarm-content span {
  display: block;
}

.alarm-content strong {
  overflow: hidden;
  color: var(--text);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-content span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-heading,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>
