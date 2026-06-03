import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

export const menuRoutes: RouteRecordRaw[] = [
  { path: '/dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页仪表盘', icon: 'DataAnalysis' } },
  { path: '/customers', name: 'customers', component: () => import('@/views/customers/CustomerView.vue'), meta: { title: '客户管理', icon: 'OfficeBuilding' } },
  { path: '/projects', name: 'projects', component: () => import('@/views/projects/ProjectView.vue'), meta: { title: '项目管理', icon: 'Folder' } },
  { path: '/gateways', name: 'gateways', component: () => import('@/views/gateways/GatewayView.vue'), meta: { title: '盒子管理', icon: 'Box' } },
  { path: '/devices', name: 'devices', component: () => import('@/views/devices/DeviceView.vue'), meta: { title: '设备管理', icon: 'Cpu' } },
  { path: '/monitor', name: 'monitor', component: () => import('@/views/monitor/RealtimeMonitorView.vue'), meta: { title: '实时监控', icon: 'Monitor' } },
  { path: '/history', name: 'history', component: () => import('@/views/history/HistoryDataView.vue'), meta: { title: '历史数据', icon: 'TrendCharts' } },
  { path: '/alarms', name: 'alarms', component: () => import('@/views/alarms/AlarmCenterView.vue'), meta: { title: '告警中心', icon: 'Warning' } },
  { path: '/ai-api', name: 'ai-api', component: () => import('@/views/ai/AiApiView.vue'), meta: { title: 'AI接口管理', icon: 'Connection' } },
  { path: '/users', name: 'users', component: () => import('@/views/users/UserManagementView.vue'), meta: { title: '用户管理', icon: 'User' } },
  { path: '/settings', name: 'settings', component: () => import('@/views/system/SystemSettingsView.vue'), meta: { title: '系统设置', icon: 'Setting' } }
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
  document.title = `${String(to.meta.title || '首页仪表盘')} - 工业风机物联网私有云平台`
  return true
})

export default router
