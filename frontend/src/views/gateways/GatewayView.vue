<template>
  <div class="asset-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>盒子管理</h2>
          <p>D200 串口 DTU 档案、MQTT 参数、定位和远程配置能力维护</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增 D200
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
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="SN、IMEI、Client ID 或 Topic"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
            <a-button @click="resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" title="D200 盒子档案" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>

      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1740, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'gateway'">
            <button class="primary-link" type="button" @click="openDetails(record)">
              <strong>{{ record.gatewayName }}</strong>
              <span>SN：{{ record.gatewaySn }}</span>
              <small>{{ record.gatewayId }}</small>
            </button>
          </template>
          <template v-else-if="column.key === 'owner'">
            <div class="table-stack">
              <span>{{ textOrDash(record.customerName || record.customerId) }}</span>
              <small>{{ textOrDash(record.projectName || record.projectId) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'identity'">
            <div class="table-stack">
              <span>IMEI：{{ textOrDash(record.imei) }}</span>
              <small>ICCID：{{ textOrDash(record.iccid) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'mqtt'">
            <div class="table-stack">
              <span>{{ textOrDash(record.mqttClientId) }}</span>
              <small>MQTT {{ textOrDash(record.mqttVersion) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'activationStatus'">
            <a-tag :color="activationColor(record.activationStatus)">
              {{ activationLabel(record.activationStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'onlineStatus'">
            <a-tag :color="record.onlineStatus === 'online' ? 'success' : 'default'">
              {{ record.onlineStatus === 'online' ? '在线' : '离线' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastSeenAt'">
            {{ formatDateTime(record.lastSeenAt) }}
          </template>
          <template v-else-if="column.key === 'location'">
            <div class="table-stack">
              <span>{{ textOrDash(locationText(record)) }}</span>
              <small>
                <a-tag :color="locationColor(record.locationSource)">
                  {{ locationSourceLabel(record.locationSource) }}
                </a-tag>
                {{ formatDateTime(record.lastLocationTime) }}
              </small>
            </div>
          </template>
          <template v-else-if="column.key === 'remoteConfig'">
            <div class="table-stack">
              <span>
                <a-tag :color="record.remoteConfigSupported ? 'success' : 'default'">
                  {{ record.remoteConfigSupported ? '支持' : '不支持' }}
                </a-tag>
              </span>
              <small>{{ formatDateTime(record.lastConfigTime) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="2">
              <a-button type="link" size="small" @click="openDetails(record)">详情</a-button>
              <a-button type="link" size="small" @click="openLbs(record)">基站定位</a-button>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                :title="`确认删除盒子“${record.gatewayName}”吗？`"
                description="删除后无法恢复，请确认该盒子已无设备关联。"
                ok-text="删除"
                cancel-text="取消"
                ok-type="danger"
                @confirm="remove(record)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无盒子档案" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条盒子记录</span>
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
      :title="editingId ? '编辑 D200 盒子' : '新增 D200 盒子'"
      width="980px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-divider orientation="left">基础资料</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="盒子编号" name="gatewayId">
              <a-input v-model:value="form.gatewayId" :disabled="Boolean(editingId)" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="SN" name="gatewaySn">
              <a-input v-model:value="form.gatewaySn" @blur="fillTopics" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="盒子名称" name="gatewayName">
              <a-input v-model:value="form.gatewayName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="型号">
              <a-input v-model:value="form.gatewayModel" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="IMEI">
              <a-input v-model:value="form.imei" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="ICCID">
              <a-input v-model:value="form.iccid" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="SIM 卡号">
              <a-input v-model:value="form.simCardNo" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="固件版本">
              <a-input v-model:value="form.firmwareVersion" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="激活状态">
              <a-select v-model:value="form.activationStatus" :options="activationOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">MQTT 参数</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="MQTT Client ID">
              <a-input v-model:value="form.mqttClientId" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="MQTT 用户名">
              <a-input v-model:value="form.mqttUsername" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="MQTT 密码">
              <a-input-password v-model:value="form.mqttPassword" placeholder="编辑时留空表示不修改" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="MQTT 版本">
              <a-select v-model:value="form.mqttVersion" :options="mqttVersionOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="QoS">
              <a-select v-model:value="form.qos" :options="qosOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="Keepalive">
              <a-input-number v-model:value="form.keepalive" :min="30" :max="65535" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上行 Topic">
              <a-input v-model:value="form.publishTopic" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="下行 Topic">
              <a-input v-model:value="form.subscribeTopic" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="TLS 状态">
              <a-switch v-model:checked="form.tlsEnabled" disabled />
              <span class="field-help inline-help">D200 不支持 TLS</span>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="远程配置">
              <a-switch v-model:checked="form.remoteConfigSupported" checked-children="支持" un-checked-children="不支持" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">业务归属与通信</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="所属客户">
              <a-select
                v-model:value="form.customerId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="customerOptions"
                @change="handleFormCustomerChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="所属项目">
              <a-select
                v-model:value="form.projectId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="formProjectOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="通信状态">
              <a-tag :color="form.onlineStatus === 'online' ? 'success' : 'default'">
                {{ form.onlineStatus === 'online' ? '在线' : '离线' }}
              </a-tag>
              <span class="field-help inline-help">根据最近通信时间自动判断</span>
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">定位信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="经度">
              <a-input-number v-model:value="form.longitude" :min="-180" :max="180" :precision="7" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="纬度">
              <a-input-number v-model:value="form.latitude" :min="-90" :max="90" :precision="7" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="定位来源">
              <a-select v-model:value="form.locationSource" :options="locationSourceOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="省">
              <a-input v-model:value="form.province" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="市">
              <a-input v-model:value="form.city" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="区县">
              <a-input v-model:value="form.district" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址">
              <a-input v-model:value="form.address" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="3" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailsVisible" title="盒子详情" width="640">
      <template v-if="selectedGateway">
        <div class="detail-header">
          <div>
            <h3>{{ selectedGateway.gatewayName }}</h3>
            <span>{{ selectedGateway.gatewaySn }}</span>
          </div>
          <a-space>
            <a-tag :color="activationColor(selectedGateway.activationStatus)">
              {{ activationLabel(selectedGateway.activationStatus) }}
            </a-tag>
            <a-tag :color="selectedGateway.onlineStatus === 'online' ? 'success' : 'default'">
              {{ selectedGateway.onlineStatus === 'online' ? '在线' : '离线' }}
            </a-tag>
          </a-space>
        </div>

        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="盒子编号">{{ selectedGateway.gatewayId }}</a-descriptions-item>
          <a-descriptions-item label="型号">{{ textOrDash(selectedGateway.gatewayModel) }}</a-descriptions-item>
          <a-descriptions-item label="IMEI">{{ textOrDash(selectedGateway.imei) }}</a-descriptions-item>
          <a-descriptions-item label="ICCID">{{ textOrDash(selectedGateway.iccid) }}</a-descriptions-item>
          <a-descriptions-item label="SIM 卡号">{{ textOrDash(selectedGateway.simCardNo) }}</a-descriptions-item>
          <a-descriptions-item label="所属客户">{{ textOrDash(selectedGateway.customerName || selectedGateway.customerId) }}</a-descriptions-item>
          <a-descriptions-item label="所属项目">{{ textOrDash(selectedGateway.projectName || selectedGateway.projectId) }}</a-descriptions-item>
          <a-descriptions-item label="MQTT Client ID">{{ textOrDash(selectedGateway.mqttClientId) }}</a-descriptions-item>
          <a-descriptions-item label="MQTT 用户名">{{ textOrDash(selectedGateway.mqttUsername) }}</a-descriptions-item>
          <a-descriptions-item label="MQTT 版本">{{ textOrDash(selectedGateway.mqttVersion) }}</a-descriptions-item>
          <a-descriptions-item label="QoS">{{ selectedGateway.qos ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="Keepalive">{{ selectedGateway.keepalive ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="上行 Topic">{{ textOrDash(selectedGateway.publishTopic) }}</a-descriptions-item>
          <a-descriptions-item label="下行 Topic">{{ textOrDash(selectedGateway.subscribeTopic) }}</a-descriptions-item>
          <a-descriptions-item label="TLS">{{ selectedGateway.tlsEnabled ? '启用' : '未启用' }}</a-descriptions-item>
          <a-descriptions-item label="固件版本">{{ textOrDash(selectedGateway.firmwareVersion) }}</a-descriptions-item>
          <a-descriptions-item label="最后通信">{{ formatDateTime(selectedGateway.lastSeenAt) }}</a-descriptions-item>
          <a-descriptions-item label="定位来源">{{ locationSourceLabel(selectedGateway.locationSource) }}</a-descriptions-item>
          <a-descriptions-item label="地图坐标">{{ coordinateText(selectedGateway) }}</a-descriptions-item>
          <a-descriptions-item label="地址">{{ textOrDash(selectedGateway.address) }}</a-descriptions-item>
          <a-descriptions-item label="最后定位">{{ formatDateTime(selectedGateway.lastLocationTime) }}</a-descriptions-item>
          <a-descriptions-item label="最后配置">{{ formatDateTime(selectedGateway.lastConfigTime) }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ textOrDash(selectedGateway.remark) }}</a-descriptions-item>
        </a-descriptions>

        <div class="drawer-actions">
          <a-button @click="openLbs(selectedGateway)">基站定位</a-button>
          <a-button type="primary" @click="openEdit(selectedGateway)">编辑盒子</a-button>
        </div>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="lbsVisible"
      :confirm-loading="locating"
      title="D200 基站定位"
      width="560px"
      ok-text="查询并更新位置"
      @ok="submitLbs"
    >
      <a-alert
        title="在配置工具或串口助手发送 AT+LBS，将返回的 CID、LAC 填入下方"
        type="info"
        :closable="false"
        show-icon
      />
      <a-form class="lbs-form" :model="lbsForm" layout="vertical">
        <a-form-item label="盒子 SN">
          <a-input v-model:value="lbsForm.gatewaySn" disabled />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="MCC">
              <a-input-number v-model:value="lbsForm.mcc" :min="0" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="MNC">
              <a-input-number v-model:value="lbsForm.mnc" :min="0" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="CID" required>
              <a-input-number v-model:value="lbsForm.cid" :min="0" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="LAC" required>
              <a-input-number v-model:value="lbsForm.lac" :min="0" class="full-width" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'

import { customerApi, gatewayApi, lbsApi, projectApi } from '@/api/business'
import type { Customer, Gateway, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '盒子', key: 'gateway', width: 230, fixed: 'left' },
  { title: '业务归属', key: 'owner', width: 190 },
  { title: '通信标识', key: 'identity', width: 220 },
  { title: 'MQTT', key: 'mqtt', width: 190 },
  { title: '激活状态', key: 'activationStatus', width: 110 },
  { title: '在线状态', key: 'onlineStatus', width: 100 },
  { title: '最后通信时间', key: 'lastSeenAt', width: 180 },
  { title: '定位', key: 'location', width: 260 },
  { title: '远程配置', key: 'remoteConfig', width: 180 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' }
]

const activationOptions = [
  { label: '未激活', value: 'inactive' },
  { label: '已激活', value: 'active' },
  { label: '已禁用', value: 'disabled' }
]
const mqttVersionOptions = [
  { label: '3.1.1', value: '3.1.1' },
  { label: '3.1', value: '3.1' }
]
const qosOptions = [0, 1, 2].map((value) => ({ label: String(value), value }))
const locationSourceOptions = [
  { label: '手动维护', value: 'manual' },
  { label: '基站定位', value: 'lbs' }
]

const loading = ref(false)
const saving = ref(false)
const locating = ref(false)
const dialogVisible = ref(false)
const detailsVisible = ref(false)
const lbsVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Gateway[]>([])
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const formProjects = ref<Project[]>([])
const selectedGateway = ref<Gateway | null>(null)
const total = ref(0)
const editingId = ref<number | null>(null)

const query = reactive({
  keyword: '',
  customerId: undefined as string | undefined,
  projectId: undefined as string | undefined,
  page: 1,
  size: 10
})
const form = reactive<Partial<Gateway>>({})
const lbsForm = reactive({
  gatewaySn: '',
  imei: '',
  mcc: 460,
  mnc: 3,
  lac: undefined as number | undefined,
  cid: undefined as number | undefined
})

const rules = {
  gatewayId: [{ required: true, message: '请输入盒子编号', trigger: 'blur' }],
  gatewaySn: [{ required: true, message: '请输入 SN', trigger: 'blur' }],
  gatewayName: [{ required: true, message: '请输入盒子名称', trigger: 'blur' }]
}

const customerOptions = computed(() =>
  customers.value.map((item) => ({
    label: `${item.customerName}（${item.customerId}）`,
    value: item.customerId
  }))
)
const projectOptions = computed(() =>
  projects.value.map((item) => ({
    label: item.projectName,
    value: item.projectId
  }))
)
const formProjectOptions = computed(() =>
  formProjects.value.map((item) => ({
    label: item.projectName,
    value: item.projectId
  }))
)

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function activationLabel(value: string) {
  return {
    inactive: '未激活',
    active: '已激活',
    disabled: '已禁用'
  }[value] || value
}

function activationColor(value: string) {
  if (value === 'active') return 'success'
  if (value === 'disabled') return 'error'
  return 'warning'
}

function locationSourceLabel(value?: string) {
  if (value === 'lbs') return '基站定位'
  if (value === 'manual') return '人工位置'
  return '未定位'
}

function locationColor(value?: string) {
  if (value === 'lbs') return 'processing'
  if (value === 'manual') return 'success'
  return 'default'
}

function coordinateText(row: Gateway) {
  return row.longitude != null && row.latitude != null ? `${row.longitude}, ${row.latitude}` : '-'
}

function locationText(row: Gateway) {
  if (row.address) return row.address
  if (row.longitude != null && row.latitude != null) return coordinateText(row)
  return ''
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

async function loadFormProjects(customerId = '') {
  try {
    formProjects.value = await projectApi.options({ customerId })
  } catch (error) {
    message.error(errorText(error, '项目选项加载失败'))
  }
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await gatewayApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '盒子数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function handleQueryCustomerChange() {
  query.projectId = undefined
  await loadProjects(query.customerId || '')
  await resetAndLoad()
}

async function resetQuery() {
  Object.assign(query, {
    keyword: '',
    customerId: undefined,
    projectId: undefined,
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

function fillTopics() {
  if (!form.gatewaySn) return
  form.mqttClientId ||= form.gatewaySn
  form.publishTopic ||= `iot/d200/${form.gatewaySn}/up`
  form.subscribeTopic ||= `iot/d200/${form.gatewaySn}/down`
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    gatewayId: '',
    gatewaySn: '',
    gatewayName: '',
    gatewayModel: 'D200',
    imei: '',
    iccid: '',
    simCardNo: '',
    customerId: undefined,
    projectId: undefined,
    activationStatus: 'inactive',
    onlineStatus: 'offline',
    mqttClientId: '',
    mqttUsername: '',
    mqttPassword: '',
    publishTopic: '',
    subscribeTopic: '',
    mqttVersion: '3.1.1',
    qos: 1,
    keepalive: 60,
    tlsEnabled: false,
    remoteConfigSupported: true,
    longitude: undefined,
    latitude: undefined,
    province: '',
    city: '',
    district: '',
    address: '',
    locationSource: 'manual',
    firmwareVersion: '',
    remark: ''
  })
  nextTick(() => formRef.value?.clearValidate())
}

async function openCreate() {
  resetForm()
  await loadFormProjects()
  dialogVisible.value = true
}

function openDetails(row: Gateway) {
  selectedGateway.value = row
  detailsVisible.value = true
}

async function openEdit(row: Gateway) {
  detailsVisible.value = false
  editingId.value = row.id
  Object.assign(form, row, { mqttPassword: '', tlsEnabled: false })
  await loadFormProjects(row.customerId || '')
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleFormCustomerChange() {
  form.projectId = undefined
  await loadFormProjects(form.customerId || '')
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  fillTopics()
  saving.value = true
  try {
    if (editingId.value) {
      await gatewayApi.update(editingId.value, form)
    } else {
      await gatewayApi.create(form)
    }
    message.success('盒子保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '盒子保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: Gateway) {
  try {
    await gatewayApi.remove(row.id)
    message.success('盒子已删除')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (error) {
    message.error(errorText(error, '盒子删除失败'))
  }
}

function openLbs(row: Gateway) {
  detailsVisible.value = false
  Object.assign(lbsForm, {
    gatewaySn: row.gatewaySn,
    imei: row.imei || '',
    mcc: row.lbsMcc ?? 460,
    mnc: row.lbsMnc ?? 3,
    lac: row.lbsLac,
    cid: row.lbsCid
  })
  lbsVisible.value = true
}

async function submitLbs() {
  if (lbsForm.cid == null || lbsForm.lac == null) {
    message.error('请输入 CID 和 LAC')
    return
  }

  locating.value = true
  try {
    const result = await lbsApi.locate({
      gatewaySn: lbsForm.gatewaySn,
      imei: lbsForm.imei,
      mcc: lbsForm.mcc,
      mnc: lbsForm.mnc,
      lac: lbsForm.lac,
      cid: lbsForm.cid
    })
    message.success(`定位成功：${result.longitude}, ${result.latitude}`)
    lbsVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '基站定位失败'))
  } finally {
    locating.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadProjects(), loadFormProjects()])
  await loadData()
})
</script>

<style scoped>
.asset-page {
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
    repeat(2, minmax(210px, 1fr))
    minmax(310px, 1.45fr)
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

.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}

.primary-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.primary-link strong,
.primary-link span,
.primary-link small,
.table-stack span,
.table-stack small {
  display: block;
}

.primary-link strong {
  color: var(--primary);
  font-weight: 500;
}

.primary-link span,
.primary-link small,
.table-stack small {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.table-stack small {
  line-height: 24px;
}

.table-card :deep(.ant-empty) {
  padding: 96px 0;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
}

.full-width {
  width: 100%;
}

.field-help {
  color: var(--muted);
  font-size: 12px;
}

.inline-help {
  margin-left: 10px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.lbs-form {
  margin-top: 18px;
}

@media (max-width: 1300px) {
  .query-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .query-form :deep(.ant-form-item:last-child) {
    grid-column: auto;
  }

  .query-form :deep(.ant-select),
  .query-form :deep(.ant-input-affix-wrapper) {
    width: 100%;
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
