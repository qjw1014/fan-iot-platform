<template>
  <div class="role-page">
    <a-card :bordered="false">
      <div class="page-heading">
        <div>
          <h2>角色权限</h2>
          <p>查看平台当前可分配角色；用户角色分配请在用户管理中完成</p>
        </div>
        <a-button :loading="loading" @click="loadRoles">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <a-alert
        title="当前后端仅提供启用角色的只读查询接口，尚未提供角色新增、编辑、删除及权限树配置接口。"
        type="info"
        show-icon
        :closable="false"
      />
    </a-card>

    <a-card class="table-card" title="角色列表" :bordered="false">
      <template #extra>
        <span class="record-summary">共 {{ roles.length }} 个可用角色</span>
      </template>

      <a-table
        v-if="loading || roles.length"
        :columns="columns"
        :data-source="roles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <div class="primary-cell">
              <strong>{{ record.roleName }}</strong>
              <span>{{ record.roleCode }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'description'">
            {{ textOrDash(record.description) }}
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'success' : 'default'">
              {{ record.enabled ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'permission'">
            <a-tag>暂无权限明细接口</a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无可用角色" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'

import { userApi } from '@/api/business'
import type { Role } from '@/types/business'
import { textOrDash } from '@/utils/format'

const columns = [
  { title: '角色', key: 'role', width: 240 },
  { title: '角色说明', key: 'description' },
  { title: '状态', key: 'enabled', width: 120 },
  { title: '权限配置', key: 'permission', width: 190 }
]

const loading = ref(false)
const roles = ref<Role[]>([])

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await userApi.roles()
  } catch (error) {
    message.error(errorText(error, '角色数据加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.role-page {
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

.record-summary {
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

@media (max-width: 640px) {
  .page-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
