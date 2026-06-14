<template>
  <div class="asset-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>项目管理</h2>
          <p>维护客户项目档案，为盒子和设备建立业务归属</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增项目
        </a-button>
      </div>

      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="所属客户">
          <a-select
            v-model:value="query.customerId"
            allow-clear
            placeholder="全部客户"
            :options="customerOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="项目编号、名称或位置"
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

    <a-card class="table-card" title="项目档案" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>

      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1120, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'project'">
            <div class="primary-cell">
              <strong>{{ record.projectName }}</strong>
              <span>{{ record.projectId }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'customer'">
            <div class="table-stack">
              <span>{{ textOrDash(record.customerName) }}</span>
              <small>{{ textOrDash(record.customerId) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'location'">
            {{ textOrDash(record.location) }}
          </template>
          <template v-else-if="column.key === 'remark'">
            <span class="ellipsis-cell" :title="record.remark">{{ textOrDash(record.remark) }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                :title="`确认删除项目“${record.projectName}”吗？`"
                description="删除后无法恢复，请确认项目已无业务关联。"
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
      <a-empty v-else description="暂无项目档案" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条项目记录</span>
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
      :title="editingId ? '编辑项目' : '新增项目'"
      width="640px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="项目编号" name="projectId">
              <a-input
                v-model:value="form.projectId"
                :disabled="Boolean(editingId)"
                placeholder="例如：PROJ-001"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属客户" name="customerId">
              <a-select
                v-model:value="form.customerId"
                show-search
                placeholder="请选择客户"
                :options="customerOptions"
                :filter-option="filterOption"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目名称" name="projectName">
              <a-input v-model:value="form.projectName" placeholder="请输入项目名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目位置" name="location">
              <a-input v-model:value="form.location" placeholder="请输入项目位置" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="4" placeholder="请输入备注" />
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
import type { DefaultOptionType } from 'ant-design-vue/es/select'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'

import { customerApi, projectApi } from '@/api/business'
import type { Customer, Project } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '项目', key: 'project', width: 220, fixed: 'left' },
  { title: '所属客户', key: 'customer', width: 190 },
  { title: '项目位置', key: 'location', width: 220 },
  { title: '备注', key: 'remark', width: 280 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

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
  customerId: undefined as string | undefined,
  page: 1,
  size: 10
})

const form = reactive<Partial<Project>>({
  projectId: '',
  customerId: undefined,
  projectName: '',
  location: '',
  remark: ''
})

const rules = {
  projectId: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

const customerOptions = computed(() =>
  customers.value.map((item) => ({
    label: `${item.customerName}（${item.customerId}）`,
    value: item.customerId
  }))
)

function filterOption(input: string, option?: DefaultOptionType) {
  return String(option?.label || '').toLowerCase().includes(input.toLowerCase())
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadCustomers() {
  try {
    const pageData = await customerApi.page({ page: 1, size: 100 })
    customers.value = pageData.records
  } catch (error) {
    message.error(errorText(error, '客户选项加载失败'))
  }
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await projectApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '项目数据加载失败'))
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
    page: 1
  })
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
    projectId: '',
    customerId: undefined,
    projectName: '',
    location: '',
    remark: ''
  })
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Project) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
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
      await projectApi.update(editingId.value, form)
    } else {
      await projectApi.create(form)
    }
    message.success('项目保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '项目保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: Project) {
  try {
    await projectApi.remove(row.id)
    message.success('项目已删除')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (error) {
    message.error(errorText(error, '项目删除失败'))
  }
}

onMounted(async () => {
  await loadCustomers()
  await loadData()
})
</script>

<style scoped>
.asset-page {
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
  font-weight: 600;
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

.query-form :deep(.ant-select) {
  width: 230px;
}

.query-form :deep(.ant-input-affix-wrapper) {
  width: 300px;
}

.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}

.primary-cell strong,
.primary-cell span,
.table-stack span,
.table-stack small {
  display: block;
}

.primary-cell strong {
  color: var(--text);
  font-weight: 500;
}

.primary-cell span,
.table-stack small {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.ellipsis-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

@media (max-width: 900px) {
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-select),
  .query-form :deep(.ant-input-affix-wrapper) {
    width: 100%;
  }
}

@media (max-width: 760px) {
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
