<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">D200字段映射</div>
        <p>将 D200 data 内的 k1、k2 等键名映射为平台标准风机字段</p>
      </div>
      <div class="actions">
        <el-input v-model="query.gatewayId" clearable placeholder="网关编号" @keyup.enter="loadData" />
        <el-input v-model="query.deviceId" clearable placeholder="设备编号" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增映射</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="gatewayId" label="网关编号" min-width="140" />
      <el-table-column prop="deviceId" label="设备编号" min-width="140" />
      <el-table-column prop="sourceKey" label="D200键名" min-width="120" />
      <el-table-column prop="targetField" label="标准字段" min-width="160" />
      <el-table-column prop="scaleFactor" label="倍率" min-width="100" />
      <el-table-column prop="offsetValue" label="偏移" min-width="100" />
      <el-table-column prop="unit" label="单位" min-width="90" />
      <el-table-column label="启用" min-width="90"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" effect="dark">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :total="total" @current-change="loadData" @size-change="loadData" /></div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑字段映射' : '新增字段映射'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="网关编号" prop="gatewayId"><el-input v-model="form.gatewayId" /></el-form-item>
        <el-form-item label="设备编号" prop="deviceId"><el-input v-model="form.deviceId" /></el-form-item>
        <el-form-item label="D200键名" prop="sourceKey"><el-input v-model="form.sourceKey" placeholder="例如 k1" /></el-form-item>
        <el-form-item label="标准字段" prop="targetField">
          <el-select v-model="form.targetField" filterable>
            <el-option v-for="field in targetFields" :key="field" :label="field" :value="field" />
          </el-select>
        </el-form-item>
        <el-form-item label="倍率"><el-input-number v-model="form.scaleFactor" :precision="6" :step="0.1" /></el-form-item>
        <el-form-item label="偏移"><el-input-number v-model="form.offsetValue" :precision="6" :step="0.1" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { d200Api } from '@/api/business'
import type { D200FieldMapping } from '@/types/business'

const targetFields = ['rpm', 'current', 'voltage', 'power', 'frequency', 'pressure', 'airflow', 'motorTemperature', 'bearingTemperature', 'vibration', 'status', 'alarmCode']
const records = ref<D200FieldMapping[]>([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({ gatewayId: '', deviceId: '', page: 1, size: 10 })
const form = reactive<Partial<D200FieldMapping>>({ scaleFactor: 1, offsetValue: 0, enabled: true })
const rules: FormRules = {
  gatewayId: [{ required: true, message: '请输入网关编号', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  sourceKey: [{ required: true, message: '请输入D200键名', trigger: 'blur' }],
  targetField: [{ required: true, message: '请选择标准字段', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const page = await d200Api.mappings(query)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}
function openCreate() { editingId.value = null; Object.assign(form, { gatewayId: '', deviceId: '', sourceKey: '', targetField: '', scaleFactor: 1, offsetValue: 0, unit: '', enabled: true }); dialogVisible.value = true }
function openEdit(row: D200FieldMapping) { editingId.value = row.id; Object.assign(form, row); dialogVisible.value = true }
async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    editingId.value ? await d200Api.updateMapping(editingId.value, form) : await d200Api.createMapping(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally { saving.value = false }
}
async function remove(row: D200FieldMapping) {
  await ElMessageBox.confirm(`确认删除映射 ${row.sourceKey} -> ${row.targetField}？`, '删除确认', { type: 'warning' })
  await d200Api.removeMapping(row.id)
  ElMessage.success('删除成功')
  await loadData()
}
onMounted(loadData)
</script>
