<template>
  <section class="management-page panel">
    <div class="toolbar">
      <div>
        <div class="panel-title">用户管理</div>
        <p>维护平台登录账号、状态和角色分配</p>
      </div>
      <div class="actions">
        <el-select v-model="query.status" clearable placeholder="状态" @change="loadData">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
          <el-option label="锁定" value="locked" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="搜索用户名/姓名/手机/邮箱" @keyup.enter="loadData" />
        <el-button :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" height="calc(100vh - 300px)">
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="realName" label="姓名" min-width="120"><template #default="{ row }">{{ textOrDash(row.realName) }}</template></el-table-column>
      <el-table-column prop="phone" label="手机" min-width="130"><template #default="{ row }">{{ textOrDash(row.phone) }}</template></el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180"><template #default="{ row }">{{ textOrDash(row.email) }}</template></el-table-column>
      <el-table-column label="角色" min-width="180"><template #default="{ row }">{{ row.roleNames?.join('、') || '-' }}</template></el-table-column>
      <el-table-column prop="status" label="状态" min-width="90"><template #default="{ row }"><el-tag :type="statusTag(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" min-width="170"><template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template></el-table-column>
      <el-table-column label="操作" fixed="right" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadData" @size-change="loadData" /></div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" show-password type="password" :placeholder="editingId ? '留空则不修改' : '请输入密码'" /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="form.status"><el-option label="启用" value="enabled" /><el-option label="禁用" value="disabled" /><el-option label="锁定" value="locked" /></el-select></el-form-item>
        </div>
        <el-form-item label="角色"><el-select v-model="form.roleCodes" multiple placeholder="请选择角色"><el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api/business'
import type { Role, UserAccount } from '@/types/business'
import { formatDateTime, textOrDash } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<UserAccount[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const form = reactive<Partial<UserAccount>>({ status: 'enabled', roleCodes: [] })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

function statusLabel(value: string) {
  return { enabled: '启用', disabled: '禁用', locked: '锁定' }[value] || value
}
function statusTag(value: string) {
  return value === 'enabled' ? 'success' : value === 'locked' ? 'warning' : 'info'
}
async function loadRoles() {
  roles.value = await userApi.roles()
}
async function loadData() {
  loading.value = true
  try {
    const page = await userApi.page(query)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}
function resetForm() {
  editingId.value = null
  Object.assign(form, { username: '', password: '', realName: '', phone: '', email: '', status: 'enabled', roleCodes: [] })
  formRef.value?.clearValidate()
}
function openCreate() {
  resetForm()
  dialogVisible.value = true
}
function openEdit(row: UserAccount) {
  editingId.value = row.id
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}
async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    editingId.value ? await userApi.update(editingId.value, form) : await userApi.create(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}
async function remove(row: UserAccount) {
  await ElMessageBox.confirm(`确认删除用户“${row.username}”？`, '删除确认', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  await loadData()
}
onMounted(async () => {
  await loadRoles()
  await loadData()
})
</script>
