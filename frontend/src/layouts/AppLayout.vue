<template>
  <div class="tech-page app-shell">
    <aside class="side">
      <div class="brand">
        <div class="brand-mark">FI</div>
        <div>
          <strong>工业风机物联网私有云平台</strong>
          <span>Fan IoT Private Cloud</span>
        </div>
      </div>

      <el-menu router :default-active="route.path" background-color="transparent" text-color="#9db8d8" active-text-color="#ffffff">
        <el-menu-item v-for="item in menuRoutes" :key="item.path" :index="item.path">
          <el-icon><component :is="item.meta?.icon" /></el-icon>
          <span>{{ item.meta?.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <h1>{{ route.meta.title }}</h1>
          <p>{{ currentTime }}</p>
        </div>
        <div class="top-actions">
          <el-tag type="success" effect="dark">平台在线</el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-button" type="button">
              <el-icon><UserFilled /></el-icon>
              <span>{{ authStore.user?.realName || authStore.user?.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, UserFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { menuRoutes } from '@/router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentTime = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
    .format(new Date())
    .replace(/\//g, '-')
)

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100vh;
}
.side {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 18px 12px;
  border-right: 1px solid var(--line);
  background: rgba(6, 17, 31, 0.9);
}
.brand {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 62px;
  margin-bottom: 16px;
  padding: 0 8px;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: linear-gradient(135deg, #1463a8, #10365d);
  color: #ffffff;
  font-weight: 800;
}
.brand strong,
.brand span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.brand strong {
  font-size: 15px;
}
.brand span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}
.main {
  min-width: 0;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 78px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: rgba(8, 20, 36, 0.72);
  backdrop-filter: blur(14px);
}
.topbar h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
}
.topbar p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(13, 31, 54, 0.86);
  color: var(--text);
  cursor: pointer;
}
.content {
  padding: 20px 24px 28px;
}
@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
  }
  .side {
    position: static;
    height: auto;
  }
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
