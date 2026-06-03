<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">设备管理</div>
        <p>维护工业风机设备档案、安装位置和地图坐标</p>
      </div>
      <div class="actions">
        <el-select v-model="query.customerId" clearable placeholder="客户筛选" @change="handleQueryCustomerChange"><el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" /></el-select>
        <el-select v-model="query.projectId" clearable placeholder="项目筛选" @change="handleQueryProjectChange"><el-option v-for="item in projects" :key="item.projectId" :label="item.projectName" :value="item.projectId" /></el-select>
        <el-select v-model="query.status" clearable placeholder="状态" @change="loadData"><el-option label="在线" value="online" /><el-option label="离线" value="offline" /><el-option label="告警" value="alarm" /></el-select>
        <el-input v-model="query.keyword" clearable placeholder="搜索设备编号/名称/位置/地址" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增设备</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="deviceId" label="设备编号" min-width="130" />
      <el-table-column prop="deviceName" label="设备名称" min-width="150" />
      <el-table-column prop="customerName" label="客户" min-width="140"><template #default="{ row }">{{ textOrDash(row.customerName) }}</template></el-table-column>
      <el-table-column prop="projectName" label="项目" min-width="140"><template #default="{ row }">{{ textOrDash(row.projectName) }}</template></el-table-column>
      <el-table-column prop="installLocation" label="设备位置" min-width="160"><template #default="{ row }">{{ textOrDash(row.installLocation) }}</template></el-table-column>
      <el-table-column label="地图坐标" min-width="190"><template #default="{ row }">{{ coordinateText(row) }}</template></el-table-column>
      <el-table-column prop="address" label="地址" min-width="220"><template #default="{ row }">{{ textOrDash(row.address) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" min-width="100"><template #default="{ row }"><el-tag :type="statusTag(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadData" @size-change="loadData" /></div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备' : '新增设备'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <div class="form-grid">
          <el-form-item label="设备编号" prop="deviceId"><el-input v-model="form.deviceId" /></el-form-item>
          <el-form-item label="设备名称" prop="deviceName"><el-input v-model="form.deviceName" /></el-form-item>
          <el-form-item label="所属客户"><el-select v-model="form.customerId" clearable filterable @change="handleFormCustomerChange"><el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" /></el-select></el-form-item>
          <el-form-item label="所属项目"><el-select v-model="form.projectId" clearable filterable @change="handleFormProjectChange"><el-option v-for="item in formProjects" :key="item.projectId" :label="item.projectName" :value="item.projectId" /></el-select></el-form-item>
          <el-form-item label="所属盒子" prop="gatewayId"><el-select v-model="form.gatewayId" filterable><el-option v-for="item in formGateways" :key="item.gatewayId" :label="item.gatewayName" :value="item.gatewayId" /></el-select></el-form-item>
          <el-form-item label="设备型号"><el-input v-model="form.deviceModel" /></el-form-item>
          <el-form-item label="设备位置"><el-input v-model="form.installLocation" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="form.status"><el-option label="在线" value="online" /><el-option label="离线" value="offline" /><el-option label="告警" value="alarm" /><el-option label="维护" value="maintenance" /><el-option label="禁用" value="disabled" /></el-select></el-form-item>
          <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
          <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="7" :step="0.000001" controls-position="right" /></el-form-item>
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
import { customerApi, deviceApi, gatewayApi, projectApi } from '@/api/business'
import type { Customer, Device, Gateway, Project } from '@/types/business'
import { textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Device[]>([])
const customers = ref<Customer[]>([])
const projects = ref<Project[]>([])
const gateways = ref<Gateway[]>([])
const formProjects = ref<Project[]>([])
const formGateways = ref<Gateway[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', customerId: '', projectId: '', gatewayId: '', status: '', page: 1, size: 10 })
const form = reactive<Partial<Device>>({ status: 'offline' })
const rules: FormRules = {
  deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  gatewayId: [{ required: true, message: '请选择盒子', trigger: 'change' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}
function coordinateText(row: Device) {
  return row.longitude != null && row.latitude != null ? `${row.longitude}, ${row.latitude}` : '-'
}
function statusLabel(value: string) {
  return { online: '在线', offline: '离线', alarm: '告警', maintenance: '维护', disabled: '禁用' }[value] || value
}
function statusTag(value: string) {
  return value === 'online' ? 'success' : value === 'alarm' ? 'danger' : value === 'maintenance' ? 'warning' : 'info'
}
async function loadCustomers() { customers.value = (await customerApi.page({ page: 1, size: 100 })).records }
async function loadProjects(customerId = '') { projects.value = await projectApi.options({ customerId }) }
async function loadGateways(customerId = '', projectId = '') { gateways.value = await gatewayApi.options({ customerId, projectId }) }
async function loadFormProjects(customerId = '') { formProjects.value = await projectApi.options({ customerId }) }
async function loadFormGateways(customerId = '', projectId = '') { formGateways.value = await gatewayApi.options({ customerId, projectId }) }
async function handleQueryCustomerChange() { query.projectId = ''; query.gatewayId = ''; await loadProjects(query.customerId); await loadGateways(query.customerId); await loadData() }
async function handleQueryProjectChange() { query.gatewayId = ''; await loadGateways(query.customerId, query.projectId); await loadData() }
async function handleFormCustomerChange() { form.projectId = ''; form.gatewayId = ''; await loadFormProjects(form.customerId); await loadFormGateways(form.customerId) }
async function handleFormProjectChange() { form.gatewayId = ''; await loadFormGateways(form.customerId, form.projectId) }
async function loadData() { loading.value = true; try { const page = await deviceApi.page(query); records.value = page.records; total.value = page.total } finally { loading.value = false } }
function resetForm() { editingId.value = null; Object.assign(form, { deviceId: '', gatewayId: '', customerId: '', projectId: '', deviceName: '', deviceModel: '', installLocation: '', longitude: undefined, latitude: undefined, address: '', status: 'offline', remark: '' }); formProjects.value = projects.value; formGateways.value = gateways.value; formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
async function openEdit(row: Device) { editingId.value = row.id; Object.assign(form, row); await loadFormProjects(row.customerId || ''); await loadFormGateways(row.customerId || '', row.projectId || ''); dialogVisible.value = true }
async function submit() { await formRef.value?.validate(); saving.value = true; try { editingId.value ? await deviceApi.update(editingId.value, form) : await deviceApi.create(form); ElMessage.success('保存成功'); dialogVisible.value = false; await loadData() } finally { saving.value = false } }
async function remove(row: Device) { await ElMessageBox.confirm(`确认删除设备“${row.deviceName}”？`, '删除确认', { type: 'warning' }); await deviceApi.remove(row.id); ElMessage.success('删除成功'); await loadData() }
onMounted(async () => { await loadCustomers(); await loadProjects(); await loadGateways(); await loadFormProjects(); await loadFormGateways(); await loadData() })
</script>
