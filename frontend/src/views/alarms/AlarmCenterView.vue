<template>
  <section class="alarm-page">
    <div class="panel alarm-chart">
      <div class="panel-title">告警等级分布</div>
      <VChart :option="levelOption" autoresize />
    </div>

    <div class="panel alarm-table">
      <div class="toolbar">
        <div>
          <div class="panel-title">告警中心</div>
          <p>查看活动告警，支持确认和关闭</p>
        </div>
        <div class="actions">
          <el-select v-model="query.alarmLevel" clearable placeholder="告警等级" @change="loadData">
            <el-option label="普通" value="normal" />
            <el-option label="警告" value="warning" />
            <el-option label="严重" value="critical" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="状态" @change="loadData">
            <el-option label="活动" value="active" />
            <el-option label="已确认" value="acknowledged" />
            <el-option label="已恢复" value="recovered" />
            <el-option label="已关闭" value="closed" />
          </el-select>
          <el-input v-model="query.keyword" clearable placeholder="搜索设备/消息/编码" @keyup.enter="loadData" />
          <el-button :icon="Search" @click="loadData">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 430px)">
        <el-table-column prop="deviceId" label="设备编号" min-width="140" />
        <el-table-column prop="gatewayId" label="盒子编号" min-width="140">
          <template #default="{ row }">{{ textOrDash(row.gatewayId) }}</template>
        </el-table-column>
        <el-table-column prop="alarmMessage" label="告警内容" min-width="260" />
        <el-table-column prop="alarmLevel" label="等级" min-width="100">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.alarmLevel)" effect="dark">{{ levelLabel(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="occurredAt" label="发生时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="170">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.acknowledged" @click="acknowledge(row)">确认</el-button>
            <el-button link type="danger" :disabled="row.status === 'closed'" @click="closeAlarm(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'

import { alarmApi } from '@/api/business'
import type { Alarm } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const loading = ref(false)
const records = ref<Alarm[]>([])
const total = ref(0)
const query = reactive({
  keyword: '',
  alarmLevel: '',
  status: '',
  page: 1,
  size: 10
})

const levelOption = computed(() => {
  const warning = records.value.filter((item) => item.alarmLevel === 'warning').length
  const critical = records.value.filter((item) => item.alarmLevel === 'critical').length
  const normal = records.value.filter((item) => item.alarmLevel === 'normal').length
  return {
    color: ['#37d889', '#ffb84d', '#ff5d73'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#9db8d8' } },
    series: [
      {
        name: '告警等级',
        type: 'pie',
        radius: ['46%', '70%'],
        data: [
          { name: '普通', value: normal },
          { name: '警告', value: warning },
          { name: '严重', value: critical }
        ],
        label: { color: '#d8edff' }
      }
    ]
  }
})

function levelLabel(value: string) {
  return { normal: '普通', warning: '警告', critical: '严重' }[value] || value
}

function levelTag(value: string) {
  return value === 'critical' ? 'danger' : value === 'warning' ? 'warning' : 'success'
}

function statusLabel(value: string) {
  return { active: '活动', acknowledged: '已确认', recovered: '已恢复', closed: '已关闭' }[value] || value
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

async function acknowledge(row: Alarm) {
  await alarmApi.acknowledge(row.id)
  ElMessage.success('确认成功')
  await loadData()
}

async function closeAlarm(row: Alarm) {
  await alarmApi.close(row.id)
  ElMessage.success('关闭成功')
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.alarm-page {
  display: grid;
  gap: 16px;
}

.alarm-chart,
.alarm-table {
  padding: 16px;
}

.alarm-chart .echarts {
  width: 100%;
  height: 240px;
}
</style>
