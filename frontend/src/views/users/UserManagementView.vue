<template>
  <div class="user-page">
    <a-card class="query-card" :bordered="false">
      <div class="page-heading">
        <div>
          <h2>用户管理</h2>
          <p>维护平台登录账号、账号状态和已有角色分配</p>
        </div>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </div>

      <a-form class="query-form" layout="inline" @finish="resetAndLoad">
        <a-form-item label="账号状态">
          <a-select
            v-model:value="query.status"
            allow-clear
            placeholder="全部状态"
            :options="statusOptions"
            @change="resetAndLoad"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="用户名、姓名、手机或邮箱"
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

    <a-card class="table-card" title="用户账号" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ total }} 条记录</span>
      </template>

      <a-table
        v-if="loading || records.length"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1260, y: 'calc(100vh - 390px)' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <div class="primary-cell">
              <strong>{{ record.realName || record.username }}</strong>
              <span>{{ record.username }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'phone'">
            {{ textOrDash(record.phone) }}
          </template>
          <template v-else-if="column.key === 'email'">
            {{ textOrDash(record.email) }}
          </template>
          <template v-else-if="column.key === 'roles'">
            <a-space v-if="record.roleNames?.length" :size="[4, 4]" wrap>
              <a-tag v-for="(roleName, index) in record.roleNames" :key="`${record.id}-${index}`" color="blue">
                {{ roleName }}
              </a-tag>
            </a-space>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">
            {{ formatDateTime(record.lastLoginAt) }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm
                :title="`确认删除用户“${record.username}”吗？`"
                description="删除后该账号将无法登录，且操作无法恢复。"
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
      <a-empty v-else description="暂无用户账号" />

      <div v-if="total || loading" class="pagination-bar">
        <span>共 {{ total }} 条用户记录</span>
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
      :title="editingId ? '编辑用户' : '新增用户'"
      width="720px"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" name="username">
              <a-input
                v-model:value="form.username"
                :disabled="Boolean(editingId)"
                placeholder="请输入用户名"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="密码" name="password">
              <a-input-password
                v-model:value="form.password"
                autocomplete="new-password"
                :placeholder="editingId ? '留空表示不修改密码' : '请输入登录密码'"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="姓名" name="realName">
              <a-input v-model:value="form.realName" placeholder="请输入姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机" name="phone">
              <a-input v-model:value="form.phone" placeholder="请输入手机号码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账号状态" name="status">
              <a-select v-model:value="form.status" :options="statusOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="角色" name="roleCodes">
              <a-select
                v-model:value="form.roleCodes"
                mode="multiple"
                placeholder="请选择已有角色"
                :options="roleOptions"
              />
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

import { userApi } from '@/api/business'
import type { Role, UserAccount } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const columns = [
  { title: '用户', key: 'user', width: 190, fixed: 'left' },
  { title: '手机', key: 'phone', width: 150 },
  { title: '邮箱', key: 'email', width: 220 },
  { title: '角色', key: 'roles', width: 240 },
  { title: '状态', key: 'status', width: 100 },
  { title: '最后登录', key: 'lastLoginAt', width: 180 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

const statusOptions = [
  { label: '启用', value: 'enabled' },
  { label: '禁用', value: 'disabled' },
  { label: '锁定', value: 'locked' }
]

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<UserAccount[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({
  keyword: '',
  status: undefined as string | undefined,
  page: 1,
  size: 10
})
const form = reactive<Partial<UserAccount>>({
  status: 'enabled',
  roleCodes: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const roleOptions = computed(() =>
  roles.value.map((role) => ({
    label: `${role.roleName}（${role.roleCode}）`,
    value: role.roleCode
  }))
)

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function statusLabel(value: string) {
  return {
    enabled: '启用',
    disabled: '禁用',
    locked: '锁定'
  }[value] || value
}

function statusColor(value: string) {
  if (value === 'enabled') return 'success'
  if (value === 'locked') return 'warning'
  return 'default'
}

async function loadRoles() {
  try {
    roles.value = await userApi.roles()
  } catch (error) {
    message.error(errorText(error, '角色选项加载失败'))
  }
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await userApi.page(query)
    records.value = pageData.records
    total.value = pageData.total
  } catch (error) {
    message.error(errorText(error, '用户数据加载失败'))
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
    status: undefined,
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
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    status: 'enabled',
    roleCodes: []
  })
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserAccount) {
  editingId.value = row.id
  Object.assign(form, { ...row, password: '' })
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
      await userApi.update(editingId.value, form)
    } else {
      await userApi.create(form)
    }
    message.success('用户保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    message.error(errorText(error, '用户保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: UserAccount) {
  try {
    await userApi.remove(row.id)
    message.success('用户已删除')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (error) {
    message.error(errorText(error, '用户删除失败'))
  }
}

onMounted(async () => {
  await loadRoles()
  await loadData()
})
</script>

<style scoped>
.user-page {
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
  width: 170px;
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
