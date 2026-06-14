<template>
  <a-layout class="app-shell">
    <a-layout-sider
      v-model:collapsed="collapsed"
      class="app-sider"
      :collapsed-width="64"
      :theme="themeStore.isDark ? 'dark' : 'light'"
      :trigger="null"
      collapsible
      width="216"
    >
      <div class="brand" :class="{ collapsed }">
        <div class="brand-mark">FI</div>
        <div v-if="!collapsed" class="brand-copy">
          <strong>工业风机 IoT 管理平台</strong>
          <span>Fan IoT Platform</span>
        </div>
      </div>

      <a-menu
        v-model:openKeys="openKeys"
        :selected-keys="[route.path]"
        class="app-menu"
        mode="inline"
        :theme="themeStore.isDark ? 'dark' : 'light'"
        @click="handleMenuClick"
      >
        <a-menu-item key="/dashboard">
          <template #icon><DashboardOutlined /></template>
          首页仪表盘
        </a-menu-item>

        <a-sub-menu key="asset-center">
          <template #icon><DatabaseOutlined /></template>
          <template #title>资产中心</template>
          <a-menu-item key="/customers">客户管理</a-menu-item>
          <a-menu-item key="/projects">项目管理</a-menu-item>
          <a-menu-item key="/gateways">盒子管理</a-menu-item>
          <a-menu-item key="/devices">设备管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="monitor-center">
          <template #icon><MonitorOutlined /></template>
          <template #title>监控中心</template>
          <a-menu-item key="/monitor">实时数据</a-menu-item>
          <a-menu-item key="/history">历史数据</a-menu-item>
          <a-menu-item key="/alarms">告警记录</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="open-capability">
          <template #icon><ApiOutlined /></template>
          <template #title>开放能力</template>
          <a-menu-item key="/ai-api">AI接口管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="system-management">
          <template #icon><SettingOutlined /></template>
          <template #title>系统管理</template>
          <a-menu-item key="/users">用户管理</a-menu-item>
          <a-menu-item key="/roles">角色权限</a-menu-item>
          <a-menu-item key="/system-logs">系统日志</a-menu-item>
          <a-menu-item key="/settings">系统设置</a-menu-item>
        </a-sub-menu>
      </a-menu>

      <button class="collapse-button" type="button" :title="collapsed ? '展开菜单' : '折叠菜单'" @click="collapsed = !collapsed">
        <MenuUnfoldOutlined v-if="collapsed" />
        <MenuFoldOutlined v-else />
        <span v-if="!collapsed">折叠菜单</span>
      </button>
    </a-layout-sider>

    <a-layout class="main-layout">
      <a-layout-header class="app-header">
        <div class="header-title">
          <a-breadcrumb>
            <a-breadcrumb-item>工业风机 IoT 管理平台</a-breadcrumb-item>
            <a-breadcrumb-item>{{ route.meta.title }}</a-breadcrumb-item>
          </a-breadcrumb>
          <h1>{{ route.meta.title }}</h1>
        </div>

        <div class="header-actions">
          <a-badge status="success" text="平台在线" />
          <a-tooltip :title="themeStore.isDark ? '切换明亮主题' : '切换暗色主题'">
            <a-button type="text" class="header-icon-button" @click="themeStore.toggleTheme">
              <BulbOutlined v-if="themeStore.isDark" />
              <BgColorsOutlined v-else />
            </a-button>
          </a-tooltip>
          <a-dropdown>
            <a-button type="text" class="user-button">
              <UserOutlined />
              <span>{{ authStore.user?.realName || authStore.user?.username || '管理员' }}</span>
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="handleUserCommand">
                <a-menu-item key="settings">
                  <SettingOutlined />
                  系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="app-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import {
  ApiOutlined,
  BgColorsOutlined,
  BulbOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DownOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MonitorOutlined,
  SettingOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const collapsed = ref(false)
const openKeys = ref(['asset-center', 'monitor-center', 'open-capability', 'system-management'])

function handleMenuClick({ key }: { key: string }) {
  if (key !== route.path) {
    router.push(key)
  }
}

function handleUserCommand({ key }: { key: string }) {
  if (key === 'settings') {
    router.push('/settings')
    return
  }
  if (key === 'logout') {
    authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--bg);
}

.app-sider {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 100vh;
  overflow: hidden;
  border-right: 1px solid var(--line);
  box-shadow: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid var(--line);
  overflow: hidden;
}

.brand.collapsed {
  justify-content: center;
  padding: 0;
}

.brand-mark {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 6px;
  background: #1677ff;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  place-items: center;
}

.brand-copy {
  min-width: 0;
}

.brand-copy strong,
.brand-copy span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy strong {
  color: var(--text);
  font-size: 14px;
}

.brand-copy span {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
}

.app-menu {
  height: calc(100vh - 112px);
  padding: 8px;
  overflow-y: auto;
  border-inline-end: 0 !important;
}

.collapse-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.collapse-button:hover {
  background: var(--hover);
  color: var(--primary);
}

.main-layout {
  min-width: 0;
  background: var(--bg);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 15;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid var(--line);
  background: var(--header-bg);
  line-height: normal;
}

.header-title h1 {
  margin: 4px 0 0;
  color: var(--text);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
}

.header-title :deep(.ant-breadcrumb) {
  color: var(--muted);
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon-button,
.user-button {
  color: var(--text);
}

.user-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.app-content {
  min-height: calc(100vh - 64px);
  padding: 20px 24px 28px;
  background: var(--bg);
}

@media (max-width: 900px) {
  .app-content {
    padding: 16px;
  }

  .header-title :deep(.ant-breadcrumb),
  .header-actions :deep(.ant-badge) {
    display: none;
  }

  .user-button span {
    display: none;
  }
}
</style>
