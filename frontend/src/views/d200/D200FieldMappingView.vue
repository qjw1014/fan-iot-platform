<template>
  <div class="d200-page">
    <a-card :bordered="false">
      <div class="page-heading">
        <div>
          <h2>D200 字段映射</h2>
          <p>将 D200 data 内的 k1、k2 等键名映射为平台标准风机字段</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增映射
        </a-button>
      </div>
      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="网关编号">
          <a-input v-model:value="query.gatewayId" allow-clear placeholder="请输入网关编号" />
        </a-form-item>
        <a-form-item label="设备编号">
          <a-input v-model:value="query.deviceId" allow-clear placeholder="请输入设备编号" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="字段映射记录" :bordered="false">
      <template #extra><span class="record-summary">共 {{ total }} 条记录</span></template>
      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'success' : 'default'">
              {{ record.enabled ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit || '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                :title="`确认删除映射 ${record.sourceKey} → ${record.targetField}？`"
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
      <a-empty v-else description="暂无字段映射" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条映射</span>
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.size"
          :page-size-options="['10', '20', '50']"
          :total="total"
          show-size-changer
          @change="loadData"
          @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="dialogVisible"
      :confirm-loading="saving"
      :title="editingId ? '编辑字段映射' : '新增字段映射'"
      width="640px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="网关编号" name="gatewayId">
              <a-input v-model:value="form.gatewayId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="设备编号" name="deviceId">
              <a-input v-model:value="form.deviceId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="D200 键名" name="sourceKey">
              <a-input v-model:value="form.sourceKey" placeholder="例如 k1" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标准字段" name="targetField">
              <a-select
                v-model:value="form.targetField"
                show-search
                :options="targetFieldOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="倍率">
              <a-input-number v-model:value="form.scaleFactor" :precision="6" :step="0.1" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="偏移">
              <a-input-number v-model:value="form.offsetValue" :precision="6" :step="0.1" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位">
              <a-input v-model:value="form.unit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用">
              <a-switch v-model:checked="form.enabled" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { nextTick, onMounted, reactive, ref } from 'vue'

import { d200Api } from '@/api/business'
import type { D200FieldMapping } from '@/types/business'

const targetFields = ['rpm', 'current', 'voltage', 'power', 'frequency', 'pressure', 'airflow', 'motorTemperature', 'bearingTemperature', 'vibration', 'status', 'alarmCode']
const targetFieldOptions = targetFields.map((field) => ({ label: field, value: field }))
const columns = [
  { title: '网关编号', dataIndex: 'gatewayId', key: 'gatewayId', width: 150 },
  { title: '设备编号', dataIndex: 'deviceId', key: 'deviceId', width: 150 },
  { title: 'D200 键名', dataIndex: 'sourceKey', key: 'sourceKey', width: 130 },
  { title: '标准字段', dataIndex: 'targetField', key: 'targetField', width: 170 },
  { title: '倍率', dataIndex: 'scaleFactor', key: 'scaleFactor', width: 100 },
  { title: '偏移', dataIndex: 'offsetValue', key: 'offsetValue', width: 100 },
  { title: '单位', key: 'unit', width: 100 },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

const records = ref<D200FieldMapping[]>([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({ gatewayId: '', deviceId: '', page: 1, size: 10 })
const form = reactive<Partial<D200FieldMapping>>({ scaleFactor: 1, offsetValue: 0, enabled: true })
const rules = {
  gatewayId: [{ required: true, message: '请输入网关编号', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  sourceKey: [{ required: true, message: '请输入 D200 键名', trigger: 'blur' }],
  targetField: [{ required: true, message: '请选择标准字段', trigger: 'change' }]
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await d200Api.mappings(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '字段映射加载失败'))
  } finally {
    loading.value = false
  }
}

async function resetAndLoad() {
  query.page = 1
  await loadData()
}

async function resetQuery() {
  Object.assign(query, { gatewayId: '', deviceId: '', page: 1 })
  await loadData()
}

async function handleSizeChange(_current: number, size: number) {
  query.page = 1
  query.size = size
  await loadData()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { gatewayId: '', deviceId: '', sourceKey: '', targetField: '', scaleFactor: 1, offsetValue: 0, unit: '', enabled: true })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: D200FieldMapping) {
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
      await d200Api.updateMapping(editingId.value, form)
    } else {
      await d200Api.createMapping(form)
    }
    message.success('字段映射已保存')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '字段映射保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: D200FieldMapping) {
  try {
    await d200Api.removeMapping(row.id)
    message.success('字段映射已删除')
    await loadData()
  } catch (error) {
    message.error(errorText(error, '字段映射删除失败'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.d200-page {
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
.query-form :deep(.ant-input) {
  width: 220px;
}
.record-summary,
.pagination-bar {
  color: var(--muted);
  font-size: 13px;
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
@media (max-width: 760px) {
  .page-heading,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }
  .query-form :deep(.ant-form-item),
  .query-form :deep(.ant-form-item-control),
  .query-form :deep(.ant-input) {
    width: 100%;
  }
}
</style>
