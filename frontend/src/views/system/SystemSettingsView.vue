<template>
  <div class="settings-page">
    <a-card :bordered="false">
      <div class="page-heading">
        <div>
          <h2>系统设置</h2>
          <p>配置当前浏览器中的平台界面偏好</p>
        </div>
        <a-tag color="processing">自动保存</a-tag>
      </div>
    </a-card>

    <a-card title="界面主题" :bordered="false">
      <a-form class="settings-form" layout="vertical">
        <a-form-item label="主题模式">
          <a-radio-group v-model:value="themeStore.current" button-style="solid">
            <a-radio-button value="light">
              <BulbOutlined />
              明亮主题
            </a-radio-button>
            <a-radio-button value="dark">
              <BgColorsOutlined />
              暗色主题
            </a-radio-button>
          </a-radio-group>
          <div class="setting-help">选择后立即应用，并自动保存在当前浏览器中。</div>
        </a-form-item>

        <a-divider />

        <a-form-item label="暗色模式">
          <div class="switch-row">
            <div>
              <strong>{{ darkModeEnabled ? '暗色主题已启用' : '当前使用明亮主题' }}</strong>
              <span>此开关与上方主题模式使用同一项持久化设置。</span>
            </div>
            <a-switch
              v-model:checked="darkModeEnabled"
              checked-children="开"
              un-checked-children="关"
            />
          </div>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="当前配置" :bordered="false">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="当前主题">
          <a-tag :color="themeStore.isDark ? 'blue' : 'gold'">
            {{ themeStore.isDark ? '暗色主题' : '明亮主题' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="保存位置">当前浏览器 localStorage</a-descriptions-item>
        <a-descriptions-item label="生效方式">切换后立即生效</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { BgColorsOutlined, BulbOutlined } from '@ant-design/icons-vue'
import { computed } from 'vue'

import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const darkModeEnabled = computed({
  get: () => themeStore.isDark,
  set: (enabled: boolean) => themeStore.setTheme(enabled ? 'dark' : 'light')
})
</script>

<style scoped>
.settings-page {
  display: grid;
  gap: 16px;
  max-width: 980px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.settings-form {
  max-width: 680px;
}

.setting-help {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 48px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
}

.switch-row strong,
.switch-row span {
  display: block;
}

.switch-row strong {
  color: var(--text);
  font-weight: 500;
}

.switch-row span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .page-heading,
  .switch-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
