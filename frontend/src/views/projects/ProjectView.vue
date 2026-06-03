<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">项目管理</div>
        <p>维护客户项目档案，为盒子和设备建立业务归属</p>
      </div>
      <div class="actions">
        <el-select v-model="query.customerId" clearable placeholder="客户筛选" @change="loadData">
          <el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="搜索项目编号/名称/位置" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增项目</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="projectId" label="项目编号" min-width="130" />
      <el-table-column prop="projectName" label="项目名称" min-width="180" />
      <el-table-column prop="customerName" label="所属客户" min-width="160">
        <template #default="{ row }">{{ textOrDash(row.customerName) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="项目位置" min-width="180">
        <template #default="{ row }">{{ textOrDash(row.location) }}</template>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目' : '新增项目'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="项目编号" prop="projectId">
          <el-input v-model="form.projectId" placeholder="例如 PROJ-001" />
        </el-form-item>
        <el-form-item label="所属客户" prop="customerId">
          <el-select v-model="form.customerId" filterable placeholder="请选择客户">
            <el-option v-for="item in customers" :key="item.customerId" :label="item.customerName" :value="item.customerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入项目位置" />
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

import { customerApi, projectApi } from '@/api/business'
import type { Customer, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<Project[]>([])
const customers = ref<Customer[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)

const query = reactive({
  keyword: '',
  customerId: '',
  page: 1,
  size: 10
})

const form = reactive<Partial<Project>>({
  projectId: '',
  customerId: '',
  projectName: '',
  location: '',
  remark: ''
})

const rules: FormRules = {
  projectId: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

async function loadCustomers() {
  const pageData = await customerApi.page({ page: 1, size: 100 })
  customers.value = pageData.records
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await projectApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    projectId: '',
    customerId: '',
    projectName: '',
    location: '',
    remark: ''
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Project) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await projectApi.update(editingId.value, form)
    } else {
      await projectApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: Project) {
  await ElMessageBox.confirm(`确认删除项目「${row.projectName}」？`, '删除确认', { type: 'warning' })
  await projectApi.remove(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(async () => {
  await loadCustomers()
  await loadData()
})
</script>
