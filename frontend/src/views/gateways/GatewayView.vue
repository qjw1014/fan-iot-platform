<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">盒子管理</div>
        <p>维护物联网盒子档案、通信凭据和定位信息</p>
      </div>
      <div class="actions">
        <el-select v-model="query.customerId" clearable placeholder="客户筛选" @change="handleQueryCustomerChange">
          <el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" />
        </el-select>
        <el-select v-model="query.projectId" clearable placeholder="项目筛选" @change="loadData">
          <el-option v-for="item in projects" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="搜索盒子编号/名称/SN/地址" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增盒子</el-button>
      </div>
    </div>

    <div v-loading="loading" class="native-table-wrap">
      <table class="native-table">
        <thead>
          <tr>
            <th>盒子编号</th>
            <th>盒子名称</th>
            <th>客户</th>
            <th>项目</th>
            <th>在线状态</th>
            <th>经度</th>
            <th>纬度</th>
            <th>所属省市区</th>
            <th>地址</th>
            <th>最后定位时间</th>
            <th class="native-table-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id">
            <td>{{ row.gatewayId }}</td>
            <td>{{ row.gatewayName }}</td>
            <td>{{ textOrDash(row.customerName) }}</td>
            <td>{{ textOrDash(row.projectName) }}</td>
            <td><el-tag :type="row.onlineStatus === 'online' ? 'success' : 'info'" effect="dark">{{ row.onlineStatus === 'online' ? '在线' : '离线' }}</el-tag></td>
            <td>{{ numberOrDash(row.longitude) }}</td>
            <td>{{ numberOrDash(row.latitude) }}</td>
            <td>{{ regionText(row) }}</td>
            <td>{{ textOrDash(row.address) }}</td>
            <td>{{ formatDateTime(row.locationUpdatedAt) }}</td>
            <td class="native-table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!records.length">
            <td class="native-table-empty" colspan="11">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadData" @size-change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑盒子' : '新增盒子'" width="780px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <div class="form-grid">
          <el-form-item label="盒子编号" prop="gatewayId"><el-input v-model="form.gatewayId" /></el-form-item>
          <el-form-item label="序列号" prop="gatewaySn"><el-input v-model="form.gatewaySn" /></el-form-item>
          <el-form-item label="盒子名称" prop="gatewayName"><el-input v-model="form.gatewayName" /></el-form-item>
          <el-form-item label="盒子型号"><el-input v-model="form.gatewayModel" /></el-form-item>
          <el-form-item label="所属客户"><el-select v-model="form.customerId" clearable filterable @change="handleFormCustomerChange"><el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" /></el-select></el-form-item>
          <el-form-item label="所属项目"><el-select v-model="form.projectId" clearable filterable><el-option v-for="item in formProjects" :key="item.projectId" :label="item.projectName" :value="item.projectId" /></el-select></el-form-item>
          <el-form-item label="在线状态"><el-select v-model="form.onlineStatus"><el-option label="在线" value="online" /><el-option label="离线" value="offline" /></el-select></el-form-item>
          <el-form-item label="激活状态"><el-select v-model="form.activationStatus"><el-option label="未激活" value="inactive" /><el-option label="已激活" value="active" /><el-option label="已禁用" value="disabled" /></el-select></el-form-item>
          <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
          <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
          <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
          <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
          <el-form-item label="区县"><el-input v-model="form.district" /></el-form-item>
          <el-form-item label="固件版本"><el-input v-model="form.firmwareVersion" /></el-form-item>
        </div>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { customerApi, gatewayApi, projectApi } from '@/api/business'
import type { Customer, Gateway, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Gateway[]>([])
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const formProjects = ref<Project[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', customerId: '', projectId: '', page: 1, size: 10 })
const form = reactive<Partial<Gateway>>({ activationStatus: 'inactive', onlineStatus: 'offline' })
const rules: FormRules = {
  gatewayId: [{ required: true, message: '请输入盒子编号', trigger: 'blur' }],
  gatewaySn: [{ required: true, message: '请输入序列号', trigger: 'blur' }],
  gatewayName: [{ required: true, message: '请输入盒子名称', trigger: 'blur' }]
}

function regionText(row: Gateway) {
  return [row.province, row.city, row.district].filter(Boolean).join(' / ') || '-'
}

function numberOrDash(value?: number) {
  return value === undefined || value === null ? '-' : String(value)
}

async function loadCustomers() {
  customers.value = (await customerApi.page({ page: 1, size: 100 })).records
}
async function loadProjects(customerId = '') {
  projects.value = await projectApi.options({ customerId })
}
async function loadFormProjects(customerId = '') {
  formProjects.value = await projectApi.options({ customerId })
}
async function handleQueryCustomerChange() {
  query.projectId = ''
  await loadProjects(query.customerId)
  await loadData()
}
async function handleFormCustomerChange() {
  form.projectId = ''
  await loadFormProjects(form.customerId)
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
  Object.assign(form, { gatewayId: '', gatewaySn: '', gatewayName: '', gatewayModel: '', customerId: '', projectId: '', activationStatus: 'inactive', onlineStatus: 'offline', longitude: undefined, latitude: undefined, province: '', city: '', district: '', address: '', firmwareVersion: '', remark: '' })
  formProjects.value = projects.value
  formRef.value?.clearValidate()
}
function openCreate() {
  resetForm()
  dialogVisible.value = true
}
async function openEdit(row: Gateway) {
  editingId.value = row.id
  Object.assign(form, row)
  await loadFormProjects(row.customerId || '')
  dialogVisible.value = true
}
async function submit() {
  await formRef.value?.validate()
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
  min-width: 1500px;
  border-collapse: collapse;
  color: #d7e9ff;
  font-size: 14px;
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

.native-table tbody tr:hover {
  background: rgba(59, 130, 246, 0.12);
}

.native-table-actions {
  position: sticky;
  right: 0;
  background: #10233b;
}

.native-table tbody .native-table-actions {
  background: #0b1b2e;
}

.native-table-empty {
  height: 180px;
  color: #7c8da5;
  text-align: center;
}
</style>
