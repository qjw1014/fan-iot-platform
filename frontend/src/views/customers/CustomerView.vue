<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">客户管理</div>
        <p>维护客户基础档案，供项目、盒子和设备关联使用</p>
      </div>
      <div class="actions">
        <el-input v-model="query.keyword" clearable placeholder="搜索客户编号/名称/联系人" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增客户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="customerId" label="客户编号" min-width="130" />
      <el-table-column prop="customerName" label="客户名称" min-width="180" />
      <el-table-column prop="contactPerson" label="联系人" min-width="120">
        <template #default="{ row }">{{ textOrDash(row.contactPerson) }}</template>
      </el-table-column>
      <el-table-column prop="contactPhone" label="联系电话" min-width="140">
        <template #default="{ row }">{{ textOrDash(row.contactPhone) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180">
        <template #default="{ row }">{{ textOrDash(row.remark) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="客户编号" prop="customerId">
          <el-input v-model="form.customerId" placeholder="例如 CUST-001" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { customerApi } from '@/api/business'
import type { Customer } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Customer[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const form = reactive<Partial<Customer>>({
  customerId: '',
  customerName: '',
  contactPerson: '',
  contactPhone: '',
  remark: ''
})

const rules: FormRules = {
  customerId: [{ required: true, message: '请输入客户编号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await customerApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    customerId: '',
    customerName: '',
    contactPerson: '',
    contactPhone: '',
    remark: ''
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Customer) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await customerApi.update(editingId.value, form)
    } else {
      await customerApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: Customer) {
  await ElMessageBox.confirm(`确认删除客户「${row.customerName}」？`, '删除确认', { type: 'warning' })
  await customerApi.remove(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>
