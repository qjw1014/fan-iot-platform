<template>
  <div class="asset-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>客户管理</h2>
          <p>维护客户基础档案，供项目、盒子和设备建立归属关系</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增客户
        </a-button>
      </div>

      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="客户编号、名称或联系人"
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

    <a-card class="table-card" title="客户档案" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>

      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1040, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'customer'">
            <div class="primary-cell">
              <strong>{{ record.customerName }}</strong>
              <span>{{ record.customerId }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'contactPerson'">
            {{ textOrDash(record.contactPerson) }}
          </template>
          <template v-else-if="column.key === 'contactPhone'">
            {{ textOrDash(record.contactPhone) }}
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
                :title="`确认删除客户“${record.customerName}”吗？`"
                description="删除后无法恢复，请确认该客户已无业务关联。"
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
      <a-empty v-else description="暂无客户档案" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条客户记录</span>
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
      :title="editingId ? '编辑客户' : '新增客户'"
      width="600px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户编号" name="customerId">
              <a-input
                v-model:value="form.customerId"
                :disabled="Boolean(editingId)"
                placeholder="例如：CUST-001"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户名称" name="customerName">
              <a-input v-model:value="form.customerName" placeholder="请输入客户名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="form.contactPerson" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="form.contactPhone" placeholder="请输入联系电话" />
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
import { message } from 'ant-design-vue'
import { nextTick, onMounted, reactive, ref } from 'vue'

import { customerApi } from '@/api/business'
import type { Customer } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '客户', key: 'customer', width: 220, fixed: 'left' },
  { title: '联系人', key: 'contactPerson', width: 140 },
  { title: '联系电话', key: 'contactPhone', width: 160 },
  { title: '备注', key: 'remark', width: 300 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

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

const rules = {
  customerId: [{ required: true, message: '请输入客户编号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await customerApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '客户数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  query.keyword = ''
  query.page = 1
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
    customerId: '',
    customerName: '',
    contactPerson: '',
    contactPhone: '',
    remark: ''
  })
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Customer) {
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
      await customerApi.update(editingId.value, form)
    } else {
      await customerApi.create(form)
    }
    message.success('客户保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '客户保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: Customer) {
  try {
    await customerApi.remove(row.id)
    message.success('客户已删除')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (error) {
    message.error(errorText(error, '客户删除失败'))
  }
}

onMounted(loadData)
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

.query-form :deep(.ant-form-item:first-child) {
  min-width: min(360px, 100%);
}

.query-form :deep(.ant-form-item-control) {
  min-width: 260px;
}

.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
}

.primary-cell strong,
.primary-cell span {
  display: block;
}

.primary-cell strong {
  color: var(--text);
  font-weight: 500;
}

.primary-cell span {
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

@media (max-width: 760px) {
  .page-heading,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control) {
    width: 100%;
    min-width: 0;
  }

  :deep(.ant-modal) {
    width: calc(100vw - 24px) !important;
  }
}
</style>
