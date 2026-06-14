<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { theme as antTheme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const themeConfig = computed(() => ({
  algorithm: themeStore.isDark ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
  token: {
    colorPrimary: '#1677ff',
    colorSuccess: '#22a06b',
    colorWarning: '#d89614',
    colorError: '#e5484d',
    borderRadius: 6,
    fontSize: 14,
    wireframe: false
  }
}))
</script>
