<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">盒子管理</div>
        <p>D200 串口 DTU 档案、MQTT 参数、定位和远程配置能力维护</p>
      </div>
      <div class="actions">
        <el-select v-model="query.customerId" clearable placeholder="客户筛选" @change="handleQueryCustomerChange">
          <el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" />
        </el-select>
        <el-select v-model="query.projectId" clearable placeholder="项目筛选" @change="loadData">
          <el-option v-for="item in projects" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="搜索SN/IMEI/Client ID/Topic" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增D200</el-button>
      </div>
    </div>

    <div v-loading="loading" class="native-table-wrap">
      <table class="native-table">
        <thead>
          <tr>
            <th>SN</th>
            <th>盒子名称</th>
            <th>IMEI</th>
            <th>ICCID</th>
            <th>MQTT Client ID</th>
            <th>MQTT版本</th>
            <th>发布Topic</th>
            <th>订阅Topic</th>
            <th>TLS</th>
            <th>在线状态</th>
            <th>最后通信时间</th>
            <th>定位</th>
            <th>远程配置</th>
            <th class="native-table-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id">
            <td>{{ row.gatewaySn }}</td>
            <td>{{ row.gatewayName }}</td>
            <td>{{ textOrDash(row.imei) }}</td>
            <td>{{ textOrDash(row.iccid) }}</td>
            <td>{{ textOrDash(row.mqttClientId) }}</td>
            <td>{{ row.mqttVersion || '3.1.1' }}</td>
            <td>{{ textOrDash(row.publishTopic) }}</td>
            <td>{{ textOrDash(row.subscribeTopic) }}</td>
            <td><el-tag :type="row.tlsEnabled ? 'danger' : 'info'" effect="dark">{{ row.tlsEnabled ? '启用' : '未启用' }}</el-tag></td>
            <td><el-tag :type="row.onlineStatus === 'online' ? 'success' : 'info'" effect="dark">{{ row.onlineStatus === 'online' ? '在线' : '离线' }}</el-tag></td>
            <td>{{ formatDateTime(row.lastSeenAt) }}</td>
            <td>
              <el-tag :type="row.locationSource === 'lbs' ? 'warning' : 'primary'" effect="dark">
                {{ row.locationSource === 'lbs' ? '基站定位' : '人工位置' }}
              </el-tag>
              <div class="muted">{{ formatDateTime(row.lastLocationTime) }}</div>
            </td>
            <td>
              <el-tag :type="row.remoteConfigSupported ? 'success' : 'info'" effect="dark">
                {{ row.remoteConfigSupported ? '支持' : '不支持' }}
              </el-tag>
              <span class="muted"> {{ formatDateTime(row.lastConfigTime) }}</span>
            </td>
            <td class="native-table-actions">
              <el-button link type="warning" @click="openLbs(row)">基站定位</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!records.length">
            <td class="native-table-empty" colspan="14">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadData" @size-change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑D200盒子' : '新增D200盒子'" width="920px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="126px">
        <div class="form-grid">
          <el-form-item label="盒子编号" prop="gatewayId"><el-input v-model="form.gatewayId" /></el-form-item>
          <el-form-item label="SN" prop="gatewaySn"><el-input v-model="form.gatewaySn" @blur="fillTopics" /></el-form-item>
          <el-form-item label="盒子名称" prop="gatewayName"><el-input v-model="form.gatewayName" /></el-form-item>
          <el-form-item label="型号"><el-input v-model="form.gatewayModel" /></el-form-item>
          <el-form-item label="IMEI"><el-input v-model="form.imei" /></el-form-item>
          <el-form-item label="ICCID"><el-input v-model="form.iccid" /></el-form-item>
          <el-form-item label="SIM卡号"><el-input v-model="form.simCardNo" /></el-form-item>
          <el-form-item label="MQTT Client ID"><el-input v-model="form.mqttClientId" /></el-form-item>
          <el-form-item label="MQTT用户名"><el-input v-model="form.mqttUsername" /></el-form-item>
          <el-form-item label="MQTT密码"><el-input v-model="form.mqttPassword" show-password placeholder="留空则不修改" /></el-form-item>
          <el-form-item label="MQTT版本"><el-select v-model="form.mqttVersion"><el-option label="3.1.1" value="3.1.1" /><el-option label="3.1" value="3.1" /></el-select></el-form-item>
          <el-form-item label="QoS"><el-select v-model="form.qos"><el-option :value="0" label="0" /><el-option :value="1" label="1" /><el-option :value="2" label="2" /></el-select></el-form-item>
          <el-form-item label="Keepalive"><el-input-number v-model="form.keepalive" :min="30" :max="65535" controls-position="right" /></el-form-item>
          <el-form-item label="TLS状态"><el-switch v-model="form.tlsEnabled" disabled inactive-text="D200不支持TLS" /></el-form-item>
          <el-form-item label="上行Topic"><el-input v-model="form.publishTopic" /></el-form-item>
          <el-form-item label="下行Topic"><el-input v-model="form.subscribeTopic" /></el-form-item>
          <el-form-item label="所属客户"><el-select v-model="form.customerId" clearable filterable @change="handleFormCustomerChange"><el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" /></el-select></el-form-item>
          <el-form-item label="所属项目"><el-select v-model="form.projectId" clearable filterable><el-option v-for="item in formProjects" :key="item.projectId" :label="item.projectName" :value="item.projectId" /></el-select></el-form-item>
          <el-form-item label="通信状态">
            <el-tag :type="form.onlineStatus === 'online' ? 'success' : 'info'" effect="dark">
              {{ form.onlineStatus === 'online' ? '在线' : '离线' }}
            </el-tag>
            <span class="status-hint">由最近通信时间自动判断</span>
          </el-form-item>
          <el-form-item label="激活状态"><el-select v-model="form.activationStatus"><el-option label="未激活" value="inactive" /><el-option label="已激活" value="active" /><el-option label="已禁用" value="disabled" /></el-select></el-form-item>
          <el-form-item label="远程配置"><el-switch v-model="form.remoteConfigSupported" active-text="支持" inactive-text="不支持" /></el-form-item>
          <el-form-item label="固件版本"><el-input v-model="form.firmwareVersion" /></el-form-item>
          <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
          <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
          <el-form-item label="定位来源"><el-select v-model="form.locationSource"><el-option label="手动维护" value="manual" /><el-option label="基站定位" value="lbs" /></el-select></el-form-item>
          <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
          <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
          <el-form-item label="区县"><el-input v-model="form.district" /></el-form-item>
        </div>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="lbsVisible" title="D200基站定位" width="520px">
      <el-alert
        title="在配置工具或串口助手发送 AT+LBS，将返回的 +LBS:CID,LAC 两个数字填入下方"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form :model="lbsForm" label-width="100px" class="lbs-form">
        <el-form-item label="盒子SN"><el-input v-model="lbsForm.gatewaySn" disabled /></el-form-item>
        <el-form-item label="MCC"><el-input-number v-model="lbsForm.mcc" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="MNC"><el-input-number v-model="lbsForm.mnc" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="CID"><el-input-number v-model="lbsForm.cid" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="LAC"><el-input-number v-model="lbsForm.lac" :min="0" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lbsVisible = false">取消</el-button>
        <el-button type="primary" :loading="locating" @click="submitLbs">查询并更新位置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { customerApi, gatewayApi, lbsApi, projectApi } from '@/api/business'
import type { Customer, Gateway, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const lbsVisible = ref(false)
const locating = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Gateway[]>([])
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const formProjects = ref<Project[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', customerId: '', projectId: '', page: 1, size: 10 })
const form = reactive<Partial<Gateway>>({})
const lbsForm = reactive({ gatewaySn: '', imei: '', mcc: 460, mnc: 3, lac: undefined as number | undefined, cid: undefined as number | undefined })
const rules: FormRules = {
  gatewayId: [{ required: true, message: '请输入盒子编号', trigger: 'blur' }],
  gatewaySn: [{ required: true, message: '请输入SN', trigger: 'blur' }],
  gatewayName: [{ required: true, message: '请输入盒子名称', trigger: 'blur' }]
}

async function loadCustomers() { customers.value = (await customerApi.page({ page: 1, size: 100 })).records }
async function loadProjects(customerId = '') { projects.value = await projectApi.options({ customerId }) }
async function loadFormProjects(customerId = '') { formProjects.value = await projectApi.options({ customerId }) }
async function handleQueryCustomerChange() { query.projectId = ''; await loadProjects(query.customerId); await loadData() }
async function handleFormCustomerChange() { form.projectId = ''; await loadFormProjects(form.customerId) }

function fillTopics() {
  if (!form.gatewaySn) return
  form.mqttClientId ||= form.gatewaySn
  form.publishTopic ||= `iot/d200/${form.gatewaySn}/up`
  form.subscribeTopic ||= `iot/d200/${form.gatewaySn}/down`
}

async function loadData() {
  loading.value = true
  try {
    const page = await gatewayApi.page(query)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    gatewayId: '', gatewaySn: '', gatewayName: '', gatewayModel: 'D200', imei: '', iccid: '', simCardNo: '',
    customerId: '', projectId: '', activationStatus: 'inactive', onlineStatus: 'offline',
    mqttClientId: '', mqttUsername: '', mqttPassword: '', publishTopic: '', subscribeTopic: '',
    mqttVersion: '3.1.1', qos: 1, keepalive: 60, tlsEnabled: false, remoteConfigSupported: true,
    longitude: undefined, latitude: undefined, province: '', city: '', district: '', address: '',
    locationSource: 'manual', firmwareVersion: '', remark: ''
  })
  formProjects.value = projects.value
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Gateway) {
  editingId.value = row.id
  Object.assign(form, row, { mqttPassword: '', tlsEnabled: false })
  await loadFormProjects(row.customerId || '')
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  fillTopics()
  saving.value = true
  try {
    editingId.value ? await gatewayApi.update(editingId.value, form) : await gatewayApi.create(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: Gateway) {
  await ElMessageBox.confirm(`确认删除盒子“${row.gatewayName}”？`, '删除确认', { type: 'warning' })
  await gatewayApi.remove(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

function openLbs(row: Gateway) {
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
    ElMessage.warning('请输入CID和LAC')
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
    ElMessage.success(`定位成功：${result.longitude}, ${result.latitude}`)
    lbsVisible.value = false
    await loadData()
  } finally {
    locating.value = false
  }
}

onMounted(async () => {
  await loadCustomers()
  await loadProjects()
  await loadFormProjects()
  await loadData()
})
</script>

<style scoped>
.native-table-wrap {
  min-height: calc(100vh - 300px);
  overflow: auto;
  border: 1px solid rgba(66, 153, 225, 0.22);
  border-radius: 8px;
  background: rgba(8, 21, 36, 0.72);
}

.native-table {
  width: 100%;
  min-width: 2100px;
  border-collapse: collapse;
  color: #d7e9ff;
  font-size: 14px;
}

.lbs-form {
  margin-top: 18px;
}

.lbs-form :deep(.el-input-number) {
  width: 100%;
}

.status-hint {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.native-table th,
.native-table td {
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(88, 166, 255, 0.14);
  text-align: left;
  white-space: nowrap;
}

.native-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #10233b;
  color: #8fc8ff;
  font-weight: 600;
}

.native-table tbody tr:hover { background: rgba(59, 130, 246, 0.12); }
.native-table-actions { position: sticky; right: 0; background: #10233b; }
.native-table tbody .native-table-actions { background: #0b1b2e; }
.native-table-empty { height: 180px; color: #7c8da5; text-align: center; }
.muted { color: #7c8da5; }
</style>
