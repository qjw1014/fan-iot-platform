<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">系统设置</div>
        <p>查看平台运行与操作日志，支撑审计和故障排查</p>
      </div>
      <div class="actions">
        <el-select v-model="query.logLevel" clearable placeholder="日志级别" @change="loadData">
          <el-option label="调试" value="debug" />
          <el-option label="信息" value="info" />
          <el-option label="警告" value="warn" />
          <el-option label="错误" value="error" />
        </el-select>
        <el-input v-model="query.module" clearable placeholder="模块" @keyup.enter="loadData" />
        <el-input v-model="query.keyword" clearable placeholder="搜索操作/内容" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="logLevel" label="级别" min-width="90"><template #default="{ row }"><el-tag :type="levelTag(row.logLevel)" effect="dark">{{ levelLabel(row.logLevel) }}</el-tag></template></el-table-column>
      <el-table-column prop="logType" label="类型" min-width="110" />
      <el-table-column prop="module" label="模块" min-width="130"><template #default="{ row }">{{ textOrDash(row.module) }}</template></el-table-column>
      <el-table-column prop="operation" label="操作" min-width="130"><template #default="{ row }">{{ textOrDash(row.operation) }}</template></el-table-column>
      <el-table-column prop="message" label="内容" min-width="320" />
      <el-table-column prop="createdAt" label="时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
    </el-table>

    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadData" @size-change="loadData" /></div>
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { systemLogApi } from '@/api/business'
import type { SystemLog } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const records = ref<SystemLog[]>([])
const total = ref(0)
const query = reactive({ keyword: '', logLevel: '', module: '', page: 1, size: 10 })

function levelLabel(value: string) {
  return { debug: '调试', info: '信息', warn: '警告', error: '错误' }[value] || value
}
function levelTag(value: string) {
  return value === 'error' ? 'danger' : value === 'warn' ? 'warning' : value === 'info' ? 'success' : 'info'
}
async function loadData() {
  loading.value = true
  try {
    const page = await systemLogApi.page(query)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}
onMounted(loadData)
</script>
