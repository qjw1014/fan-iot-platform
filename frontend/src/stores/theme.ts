import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type AppTheme = 'light' | 'dark'

const STORAGE_KEY = 'fan-iot-theme'

function initialTheme(): AppTheme {
  const saved = window.localStorage.getItem(STORAGE_KEY)
  return saved === 'dark' ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const current = ref<AppTheme>(initialTheme())
  const isDark = computed(() => current.value === 'dark')

  function setTheme(value: AppTheme) {
    current.value = value
  }

  function toggleTheme() {
    current.value = isDark.value ? 'light' : 'dark'
  }

  watch(
    current,
    (value) => {
      document.documentElement.dataset.theme = value
      document.documentElement.style.colorScheme = value
      window.localStorage.setItem(STORAGE_KEY, value)
    },
    { immediate: true }
  )

  return {
    current,
    isDark,
    setTheme,
    toggleTheme
  }
})
