<template>
  <div class="d200-page">
    <a-card :bordered="false">
      <div class="page-heading">
        <div>
          <h2>D200 远程配置任务</h2>
          <p>记录远程配置任务和状态，底层下发协议确认后再接入实际下发</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增任务
        </a-button>
      </div>
      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="盒子 SN">
          <a-input v-model:value="query.gatewaySn" allow-clear placeholder="请输入 SN" />
        </a-form-item>
        <a-form-item label="任务状态">
          <a-select
            v-model:value="query.status"
            allow-clear
            placeholder="全部状态"
            :options="statusOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="配置任务记录" :bordered="false">
      <template #extra><span class="record-summary">共 {{ total }} 条记录</span></template>
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1420, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDateTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'sentAt'">{{ formatDateTime(record.sentAt) }}</template>
          <template v-else-if="column.key === 'completedAt'">{{ formatDateTime(record.completedAt) }}</template>
          <template v-else-if="column.key === 'errorMessage'">
            <span class="ellipsis-cell" :title="record.errorMessage">{{ textOrDash(record.errorMessage) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openDetail(record)">查看配置</a-button>
              <a-popconfirm
                title="确认将该任务标记为超时吗？"
                ok-text="确认"
                cancel-text="取消"
                @confirm="mark(record.id, 'timeout')"
              >
                <a-button type="link" size="small">标记超时</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无远程配置任务" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条任务</span>
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.size"
          :page-size-options="['10', '20', '50']"
          :total="total"
          show-size-changer
          @change="loadData"
          @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="dialogVisible"
      :confirm-loading="saving"
      title="新增 D200 远程配置任务"
      width="700px"
      @ok="submit"
    >
      <a-alert
        title="当前仅记录任务，实际远程配置协议尚未接入。"
        type="info"
        show-icon
        :closable="false"
      />
      <a-form ref="formRef" class="task-form" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="网关编号" name="gatewayId">
              <a-input v-model:value="form.gatewayId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="SN" name="gatewaySn">
              <a-input v-model:value="form.gatewaySn" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="任务类型" name="taskType">
              <a-input v-model:value="form.taskType" placeholder="例如 mqtt_config / modbus_points" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="配置 JSON" name="configText">
              <a-textarea v-model:value="configText" :rows="9" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="detailVisible" title="配置内容" width="720px" :footer="null">
      <pre class="json-box">{{ detailJson }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { nextTick, onMounted, reactive, ref } from 'vue'

import { d200Api } from '@/api/business'
import type { D200ConfigTask } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const statuses = ['pending', 'sent', 'success', 'failed', 'timeout']
const statusOptions = statuses.map((status) => ({ label: statusText(status), value: status }))
const columns = [
  { title: 'SN', dataIndex: 'gatewaySn', key: 'gatewaySn', width: 180, fixed: 'left' },
  { title: '网关编号', dataIndex: 'gatewayId', key: 'gatewayId', width: 150 },
  { title: '任务类型', dataIndex: 'taskType', key: 'taskType', width: 170 },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '发送时间', key: 'sentAt', width: 180 },
  { title: '完成时间', key: 'completedAt', width: 180 },
  { title: '错误信息', key: 'errorMessage', width: 260 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]

const records = ref<D200ConfigTask[]>([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailJson = ref('')
const configText = ref('{\n  "note": "实际远程配置协议待厂家确认"\n}')
const formRef = ref<FormInstance>()
const query = reactive({ gatewaySn: '', status: undefined as string | undefined, page: 1, size: 10 })
const form = reactive({ gatewayId: '', gatewaySn: '', taskType: '' })
const rules = {
  gatewayId: [{ required: true, message: '请输入网关编号', trigger: 'blur' }],
  gatewaySn: [{ required: true, message: '请输入 SN', trigger: 'blur' }],
  taskType: [{ required: true, message: '请输入任务类型', trigger: 'blur' }]
}

function statusText(status: string) {
  return ({ pending: '待发送', sent: '已发送', success: '成功', failed: '失败', timeout: '超时' } as Record<string, string>)[status] || status
}

function statusColor(status: string) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'error'
  if (status === 'timeout') return 'warning'
  if (status === 'sent') return 'processing'
  return 'default'
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await d200Api.configTasks(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '远程配置任务加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  Object.assign(query, { gatewaySn: '', status: undefined, page: 1 })
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

function openCreate() {
  Object.assign(form, { gatewayId: '', gatewaySn: '', taskType: '' })
  configText.value = '{\n  "note": "实际远程配置协议待厂家确认"\n}'
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  let configPayload: unknown
  try {
    configPayload = JSON.parse(configText.value)
  } catch {
    message.error('配置内容不是有效的 JSON')
    return
  }

  saving.value = true
  try {
    await d200Api.createConfigTask({ ...form, configPayload })
    message.success('任务已创建')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '任务创建失败'))
  } finally {
    saving.value = false
  }
}

async function mark(id: number, status: string) {
  try {
    await d200Api.markConfigTask(id, status)
    message.success('状态已更新')
    await loadData()
  } catch (error) {
    message.error(errorText(error, '任务状态更新失败'))
  }
}

function openDetail(row: D200ConfigTask) {
  detailJson.value = JSON.stringify(row.configPayload, null, 2)
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.d200-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.page-heading h2 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
}
.page-heading p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.query-form {
  padding: 14px 14px 0;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}
.query-form :deep(.ant-input) {
  width: 220px;
}
.query-form :deep(.ant-select) {
  width: 160px;
}
.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}
.ellipsis-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
}
.task-form {
  margin-top: 18px;
}
.json-box {
  max-height: 520px;
  margin: 0;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
  color: var(--text);
}
@media (max-width: 760px) {
  .page-heading,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-input),
  .query-form :deep(.ant-select) {
    width: 100%;
  }
}
</style>
