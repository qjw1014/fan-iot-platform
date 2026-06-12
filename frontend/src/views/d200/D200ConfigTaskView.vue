<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">D200远程配置任务</div>
        <p>先记录远程配置任务和状态，底层下发协议确认后再接入实际下发</p>
      </div>
      <div class="actions">
        <el-input v-model="query.gatewaySn" clearable placeholder="SN筛选" @keyup.enter="loadData" />
        <el-select v-model="query.status" clearable placeholder="任务状态" @change="loadData">
          <el-option v-for="status in statuses" :key="status" :label="statusText(status)" :value="status" />
        </el-select>
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增任务</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="gatewaySn" label="SN" min-width="130" />
      <el-table-column prop="gatewayId" label="网关编号" min-width="130" />
      <el-table-column prop="taskType" label="任务类型" min-width="140" />
      <el-table-column label="状态" min-width="110"><template #default="{ row }"><el-tag effect="dark">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      <el-table-column label="发送时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.sentAt) }}</template></el-table-column>
      <el-table-column label="完成时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.completedAt) }}</template></el-table-column>
      <el-table-column prop="errorMessage" label="错误信息" min-width="220"><template #default="{ row }">{{ textOrDash(row.errorMessage) }}</template></el-table-column>
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看配置</el-button>
          <el-button link type="warning" @click="mark(row.id, 'timeout')">标记超时</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="loadData" @size-change="loadData" /></div>

    <el-dialog v-model="dialogVisible" title="新增D200远程配置任务" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="网关编号" prop="gatewayId"><el-input v-model="form.gatewayId" /></el-form-item>
        <el-form-item label="SN" prop="gatewaySn"><el-input v-model="form.gatewaySn" /></el-form-item>
        <el-form-item label="任务类型" prop="taskType"><el-input v-model="form.taskType" placeholder="例如 mqtt_config / modbus_points" /></el-form-item>
        <el-form-item label="配置JSON" prop="configText"><el-input v-model="configText" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存任务</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="配置内容" width="720px">
      <pre class="json-box">{{ detailJson }}</pre>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { d200Api } from '@/api/business'
import type { D200ConfigTask } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const statuses = ['pending', 'sent', 'success', 'failed', 'timeout']
const records = ref<D200ConfigTask[]>([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailJson = ref('')
const configText = ref('{\n  "note": "实际远程配置协议待厂家确认"\n}')
const formRef = ref<FormInstance>()
const query = reactive({ gatewaySn: '', status: '', page: 1, size: 10 })
const form = reactive({ gatewayId: '', gatewaySn: '', taskType: '' })
const rules: FormRules = {
  gatewayId: [{ required: true, message: '请输入网关编号', trigger: 'blur' }],
  gatewaySn: [{ required: true, message: '请输入SN', trigger: 'blur' }],
  taskType: [{ required: true, message: '请输入任务类型', trigger: 'blur' }]
}

function statusText(status: string) {
  return ({ pending: '待发送', sent: '已发送', success: '成功', failed: '失败', timeout: '超时' } as Record<string, string>)[status] || status
}
async function loadData() {
  loading.value = true
  try {
    const page = await d200Api.configTasks(query)
    records.value = page.records
    total.value = page.total
  } finally { loading.value = false }
}
function openCreate() {
  Object.assign(form, { gatewayId: '', gatewaySn: '', taskType: '' })
  configText.value = '{\n  "note": "实际远程配置协议待厂家确认"\n}'
  dialogVisible.value = true
}
async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await d200Api.createConfigTask({ ...form, configPayload: JSON.parse(configText.value) })
    ElMessage.success('任务已创建')
    dialogVisible.value = false
    await loadData()
  } finally { saving.value = false }
}
async function mark(id: number, status: string) {
  await d200Api.markConfigTask(id, status)
  ElMessage.success('状态已更新')
  await loadData()
}
function openDetail(row: D200ConfigTask) {
  detailJson.value = JSON.stringify(row.configPayload, null, 2)
  detailVisible.value = true
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
