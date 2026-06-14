<template>
  <div class="device-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>设备管理</h2>
          <p>维护工业风机设备档案、归属关系、通信状态和安装位置</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增设备
        </a-button>
      </div>

      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="所属客户">
          <a-select
            v-model:value="query.customerId"
            allow-clear
            placeholder="全部客户"
            :options="customerOptions"
            @change="handleQueryCustomerChange"
          />
        </a-form-item>
        <a-form-item label="所属项目">
          <a-select
            v-model:value="query.projectId"
            allow-clear
            placeholder="全部项目"
            :options="projectOptions"
            @change="handleQueryProjectChange"
          />
        </a-form-item>
        <a-form-item label="设备状态">
          <a-select
            v-model:value="query.status"
            allow-clear
            placeholder="全部状态"
            :options="statusOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="设备编号、名称、位置或地址"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" title="设备档案" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1320, y: 'calc(100vh - 340px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'device'">
            <button class="device-link" type="button" @click="openDetails(record)">
              <strong>{{ record.deviceName }}</strong>
              <span>{{ record.deviceId }}</span>
            </button>
          </template>
          <template v-else-if="column.key === 'gateway'">
            <div class="table-stack">
              <span>{{ textOrDash(record.gatewayName) }}</span>
              <small>{{ textOrDash(record.gatewaySn || record.gatewayId) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'location'">
            <button class="location-link" type="button" @click="openLocation(record)">
              <EnvironmentOutlined />
              <span>{{ record.installLocation || record.address || '查看或设置位置' }}</span>
            </button>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastSeenAt'">
            <span :class="{ muted: !record.lastSeenAt }">{{ formatDateTime(record.lastSeenAt) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openDetails(record)">详情</a-button>
              <a-button type="link" size="small" @click="openLocation(record)">位置</a-button>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                :title="`确认删除设备“${record.deviceName}”吗？`"
                description="删除后无法恢复。"
                ok-text="删除"
                cancel-text="取消"
                ok-type="danger"
                @confirm="remove(record)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else>
            {{ displayColumnValue(record, column.dataIndex) }}
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无设备档案" />

      <div class="pagination-bar">
        <span>共 {{ total }} 条设备记录</span>
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

    <a-modal
      v-model:open="dialogVisible"
      :confirm-loading="saving"
      :title="editingId ? '编辑设备' : '新增设备'"
      width="760px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="设备编号" name="deviceId">
              <a-input v-model:value="form.deviceId" :disabled="Boolean(editingId)" placeholder="请输入设备唯一编号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="设备名称" name="deviceName">
              <a-input v-model:value="form.deviceName" placeholder="请输入设备名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属客户">
              <a-select
                v-model:value="form.customerId"
                allow-clear
                show-search
                :options="customerOptions"
                @change="handleFormCustomerChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属项目">
              <a-select
                v-model:value="form.projectId"
                allow-clear
                show-search
                :options="formProjectOptions"
                @change="handleFormProjectChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属盒子" name="gatewayId">
              <a-select v-model:value="form.gatewayId" show-search :options="formGatewayOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="设备型号">
              <a-input v-model:value="form.deviceModel" placeholder="例如：离心风机" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="设备状态">
              <a-select v-model:value="form.status" disabled :options="statusOptions" />
              <span class="field-help">设备状态由实时通信和告警数据自动更新</span>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="安装位置">
              <a-input v-model:value="form.installLocation" placeholder="例如：一号车间东侧风机房" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="经度">
              <a-input-number v-model:value="form.longitude" :max="180" :min="-180" :precision="7" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="纬度">
              <a-input-number v-model:value="form.latitude" :max="90" :min="-90" :precision="7" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="详细地址">
              <a-input v-model:value="form.address" placeholder="请输入设备安装地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailsVisible" title="设备详情" width="520">
      <template v-if="selectedDevice">
        <div class="detail-header">
          <div>
            <h3>{{ selectedDevice.deviceName }}</h3>
            <span>{{ selectedDevice.deviceId }}</span>
          </div>
          <a-tag :color="statusColor(selectedDevice.status)">{{ statusLabel(selectedDevice.status) }}</a-tag>
        </div>
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="所属客户">{{ textOrDash(selectedDevice.customerName) }}</a-descriptions-item>
          <a-descriptions-item label="所属项目">{{ textOrDash(selectedDevice.projectName) }}</a-descriptions-item>
          <a-descriptions-item label="所属盒子">{{ textOrDash(selectedDevice.gatewayName || selectedDevice.gatewayId) }}</a-descriptions-item>
          <a-descriptions-item label="盒子 SN">{{ textOrDash(selectedDevice.gatewaySn) }}</a-descriptions-item>
          <a-descriptions-item label="设备型号">{{ textOrDash(selectedDevice.deviceModel) }}</a-descriptions-item>
          <a-descriptions-item label="安装位置">{{ textOrDash(selectedDevice.installLocation) }}</a-descriptions-item>
          <a-descriptions-item label="详细地址">{{ textOrDash(selectedDevice.address) }}</a-descriptions-item>
          <a-descriptions-item label="地图坐标">{{ coordinateText(selectedDevice) }}</a-descriptions-item>
          <a-descriptions-item label="最后通信">{{ formatDateTime(selectedDevice.lastSeenAt) }}</a-descriptions-item>
        </a-descriptions>
        <div class="drawer-actions">
          <a-button @click="openLocation(selectedDevice)">
            <template #icon><EnvironmentOutlined /></template>
            查看位置
          </a-button>
          <a-button type="primary" @click="openEdit(selectedDevice)">编辑设备</a-button>
        </div>
      </template>
    </a-drawer>

    <DeviceLocationDialog v-model="locationVisible" :device="locationDevice" @saved="loadData" />
  </div>
</template>

<script setup lang="ts">
import {
  EnvironmentOutlined,
  PlusOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'

import { customerApi, deviceApi, gatewayApi, projectApi } from '@/api/business'
import DeviceLocationDialog from '@/components/DeviceLocationDialog.vue'
import type { Customer, Device, Gateway, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '设备', key: 'device', width: 180, fixed: 'left' },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 150 },
  { title: '所属盒子', key: 'gateway', width: 180 },
  { title: '安装位置', key: 'location', width: 210 },
  { title: '状态', key: 'status', width: 100 },
  { title: '最后通信时间', key: 'lastSeenAt', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const statusOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
  { label: '告警', value: 'alarm' },
  { label: '维护', value: 'maintenance' },
  { label: '禁用', value: 'disabled' }
]

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detailsVisible = ref(false)
const locationVisible = ref(false)
const selectedDevice = ref<Device | null>(null)
const locationDevice = ref<Device | null>(null)
const formRef = ref<FormInstance>()
const records = ref<Device[]>([])
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const gateways = ref<Gateway[]>([])
const formProjects = ref<Project[]>([])
const formGateways = ref<Gateway[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', customerId: undefined as string | undefined, projectId: undefined as string | undefined, status: undefined as string | undefined, page: 1, size: 10 })
const form = reactive<Partial<Device>>({ status: 'offline' })

const rules = {
  deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  gatewayId: [{ required: true, message: '请选择所属盒子', trigger: 'change' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const customerOptions = computed(() =>
  customers.value.map((item) => ({ label: item.customerName, value: item.customerId }))
)
const projectOptions = computed(() =>
  projects.value.map((item) => ({ label: item.projectName, value: item.projectId }))
)
const formProjectOptions = computed(() =>
  formProjects.value.map((item) => ({ label: item.projectName, value: item.projectId }))
)
const formGatewayOptions = computed(() =>
  formGateways.value.map((item) => ({
    label: `${item.gatewayName}（${item.gatewaySn || item.gatewayId}）`,
    value: item.gatewayId
  }))
)

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function displayValue(value: unknown) {
  if (typeof value === 'string') return textOrDash(value)
  return value == null ? '-' : String(value)
}

function displayColumnValue(record: Device, dataIndex?: string) {
  return dataIndex ? displayValue(record[dataIndex as keyof Device]) : '-'
}

function coordinateText(row: Device) {
  return row.longitude != null && row.latitude != null ? `${row.longitude}, ${row.latitude}` : '-'
}

function statusLabel(value: string) {
  return {
    online: '在线',
    offline: '离线',
    alarm: '告警',
    maintenance: '维护',
    disabled: '禁用'
  }[value] || value
}

function statusColor(value: string) {
  if (value === 'online') return 'success'
  if (value === 'alarm') return 'error'
  if (value === 'maintenance') return 'warning'
  return 'default'
}

function openDetails(row: Device) {
  selectedDevice.value = row
  detailsVisible.value = true
}

function openLocation(row: Device) {
  locationDevice.value = row
  locationVisible.value = true
}

async function loadCustomers() {
  try {
    customers.value = (await customerApi.page({ page: 1, size: 100 })).records
  } catch (error) {
    message.error(errorText(error, '客户选项加载失败'))
  }
}

async function loadProjects(customerId = '') {
  try {
    projects.value = await projectApi.options({ customerId })
  } catch (error) {
    message.error(errorText(error, '项目选项加载失败'))
  }
}

async function loadGateways(customerId = '', projectId = '') {
  try {
    gateways.value = await gatewayApi.options({ customerId, projectId })
  } catch (error) {
    message.error(errorText(error, '盒子选项加载失败'))
  }
}

async function loadFormProjects(customerId = '') {
  try {
    formProjects.value = await projectApi.options({ customerId })
  } catch (error) {
    message.error(errorText(error, '项目选项加载失败'))
  }
}

async function loadFormGateways(customerId = '', projectId = '') {
  try {
    formGateways.value = await gatewayApi.options({ customerId, projectId })
  } catch (error) {
    message.error(errorText(error, '盒子选项加载失败'))
  }
}

async function handleQueryCustomerChange() {
  query.projectId = undefined
  await loadProjects(query.customerId || '')
  await resetAndLoad()
}

async function handleQueryProjectChange() {
  await loadGateways(query.customerId || '', query.projectId || '')
  await resetAndLoad()
}

async function handleFormCustomerChange() {
  form.projectId = undefined
  form.gatewayId = undefined
  await loadFormProjects(form.customerId || '')
  await loadFormGateways(form.customerId || '')
}

async function handleFormProjectChange() {
  form.gatewayId = undefined
  await loadFormGateways(form.customerId || '', form.projectId || '')
}

async function loadData() {
  loading.value = true
  try {
    const page = await deviceApi.page(query)
    records.value = page.records
    total.value = page.total
  } catch (error) {
    message.error(errorText(error, '设备数据加载失败'))
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
    keyword: '',
    customerId: undefined,
    projectId: undefined,
    status: undefined,
    page: 1
  })
  await loadProjects()
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    deviceId: '',
    gatewayId: undefined,
    customerId: undefined,
    projectId: undefined,
    deviceName: '',
    deviceModel: '',
    installLocation: '',
    longitude: undefined,
    latitude: undefined,
    address: '',
    status: 'offline',
    remark: ''
  })
  formProjects.value = projects.value
  formGateways.value = gateways.value
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Device) {
  detailsVisible.value = false
  editingId.value = row.id
  Object.assign(form, row)
  await loadFormProjects(row.customerId || '')
  await loadFormGateways(row.customerId || '', row.projectId || '')
  dialogVisible.value = true
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await deviceApi.update(editingId.value, form)
    } else {
      await deviceApi.create(form)
    }
    message.success('设备保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '设备保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: Device) {
  try {
    await deviceApi.remove(row.id)
    message.success('设备已删除')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (error) {
    message.error(errorText(error, '设备删除失败'))
  }
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadProjects(), loadGateways(), loadFormProjects(), loadFormGateways()])
  await loadData()
})
</script>

<style scoped>
.device-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.query-card,
.table-card {
  width: 100%;
  min-width: 0;
}

.query-card :deep(.ant-card-body),
.table-card :deep(.ant-card-body),
.table-card :deep(.ant-table-wrapper) {
  min-width: 0;
}

.table-card :deep(.ant-card-body) {
  overflow: hidden;
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
  font-weight: 600;
}

.page-heading p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.query-form {
  display: grid;
  grid-template-columns:
    repeat(3, minmax(190px, 1fr))
    minmax(290px, 1.45fr)
    auto;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.query-form :deep(.ant-form-item) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  min-width: 0;
  margin: 0;
}

.query-form :deep(.ant-form-item:last-child) {
  justify-self: end;
}

.query-form :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}

.query-form :deep(.ant-form-item-control),
.query-form :deep(.ant-form-item-control-input),
.query-form :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

.query-form :deep(.ant-select) {
  width: 100%;
}

.query-form :deep(.ant-input-affix-wrapper) {
  width: 100%;
}

.record-summary {
  color: var(--muted);
  font-size: 13px;
}

.table-card :deep(.ant-empty) {
  padding: 96px 0;
}

.device-link,
.location-link {
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.device-link strong,
.device-link span {
  display: block;
}

.device-link strong {
  color: var(--primary);
  font-weight: 500;
}

.device-link span,
.table-stack small {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.table-stack span,
.table-stack small {
  display: block;
}

.location-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 190px;
  color: var(--primary);
}

.location-link span {
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
  color: var(--muted);
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.field-help {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.detail-header h3 {
  margin: 0;
  color: var(--text);
  font-size: 18px;
}

.detail-header span {
  color: var(--muted);
  font-size: 12px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 1450px) {
  .query-form {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .query-form :deep(.ant-form-item:nth-child(4)) {
    grid-column: span 2;
  }

  .query-form :deep(.ant-form-item:last-child) {
    grid-column: auto;
  }

  .query-form :deep(.ant-select),
  .query-form :deep(.ant-input-affix-wrapper) {
    width: 100%;
  }
}

@media (max-width: 1000px) {
  .query-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .query-form :deep(.ant-form-item:nth-child(4)) {
    grid-column: auto;
  }

  .query-form :deep(.ant-form-item:last-child) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .query-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .query-form :deep(.ant-form-item:last-child) {
    grid-column: auto;
    justify-self: stretch;
  }

  .page-heading,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  :deep(.ant-modal) {
    width: calc(100vw - 24px) !important;
  }
}
</style>
