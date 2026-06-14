import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

export const menuRoutes: RouteRecordRaw[] = [
  { path: '/dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页仪表盘' } },
  { path: '/customers', name: 'customers', component: () => import('@/views/customers/CustomerView.vue'), meta: { title: '客户管理' } },
  { path: '/projects', name: 'projects', component: () => import('@/views/projects/ProjectView.vue'), meta: { title: '项目管理' } },
  { path: '/gateways', name: 'gateways', component: () => import('@/views/gateways/GatewayView.vue'), meta: { title: '盒子管理' } },
  { path: '/devices', name: 'devices', component: () => import('@/views/devices/DeviceView.vue'), meta: { title: '设备管理' } },
  { path: '/monitor', name: 'monitor', component: () => import('@/views/monitor/RealtimeMonitorView.vue'), meta: { title: '实时数据' } },
  { path: '/history', name: 'history', component: () => import('@/views/history/HistoryDataView.vue'), meta: { title: '历史数据' } },
  { path: '/alarms', name: 'alarms', component: () => import('@/views/alarms/AlarmCenterView.vue'), meta: { title: '告警记录' } },
  { path: '/ai-api', name: 'ai-api', component: () => import('@/views/ai/AiApiView.vue'), meta: { title: 'AI接口管理' } },
  { path: '/users', name: 'users', component: () => import('@/views/users/UserManagementView.vue'), meta: { title: '用户管理' } },
  { path: '/roles', name: 'roles', component: () => import('@/views/system/RolePermissionView.vue'), meta: { title: '角色权限' } },
  { path: '/system-logs', name: 'system-logs', component: () => import('@/views/system/SystemLogView.vue'), meta: { title: '系统日志' } },
  { path: '/settings', name: 'settings', component: () => import('@/views/system/SystemSettingsView.vue'), meta: { title: '系统设置' } },
  { path: '/d200-field-mappings', name: 'd200-field-mappings', component: () => import('@/views/d200/D200FieldMappingView.vue'), meta: { title: 'D200字段映射', menuHidden: true } },
  { path: '/d200-raw-payloads', name: 'd200-raw-payloads', component: () => import('@/views/d200/D200RawPayloadView.vue'), meta: { title: 'D200原始数据', menuHidden: true } },
  { path: '/d200-config-tasks', name: 'd200-config-tasks', component: () => import('@/views/d200/D200ConfigTaskView.vue'), meta: { title: 'D200远程配置', menuHidden: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/login/LoginView.vue'), meta: { public: true, title: '登录' } },
    { path: '/', component: () => import('@/layouts/AppLayout.vue'), redirect: '/dashboard', children: menuRoutes }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && authStore.isLoggedIn) {
    return '/dashboard'
  }
  document.title = `${String(to.meta.title || '首页仪表盘')} - 工业风机 IoT 管理平台`
  return true
})

export default router
