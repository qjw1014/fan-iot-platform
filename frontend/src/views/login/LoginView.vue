<template>
  <main class="login-page">
    <img
      class="login-hero"
      src="@/assets/login/industrial-fan-hero.png"
      alt="工业风机设备"
    />
    <div class="visual-shade"></div>

    <div class="login-shell">
      <section class="login-visual">
        <div class="visual-copy">
          <p class="visual-eyebrow">INDUSTRIAL FAN IOT</p>
          <h1>工业风机物联网平台</h1>
          <p class="visual-subtitle">连接物理世界 · 驱动智能运维</p>
          <span class="visual-accent"></span>
        </div>

        <div class="feature-grid" aria-label="平台能力">
          <div v-for="feature in features" :key="feature.title" class="feature-item">
            <component :is="feature.icon" class="feature-icon" />
            <div>
              <strong>{{ feature.title }}</strong>
              <span>{{ feature.description }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="language-indicator">
          <GlobalOutlined />
          <span>简体中文</span>
        </div>

        <div class="login-content">
          <div class="login-heading">
            <span class="mobile-brand">工业风机 IoT 管理平台</span>
            <h2>欢迎登录</h2>
            <p>工业风机物联网平台</p>
          </div>

          <a-form
            ref="formRef"
            class="login-form"
            :model="form"
            :rules="rules"
            size="large"
            @finish="submit"
          >
            <a-form-item name="username">
              <a-input
                v-model:value="form.username"
                autocomplete="username"
                placeholder="请输入用户名"
              >
                <template #prefix><UserOutlined /></template>
              </a-input>
            </a-form-item>

            <a-form-item name="password">
              <a-input-password
                v-model:value="form.password"
                autocomplete="current-password"
                placeholder="请输入密码"
              >
                <template #prefix><LockOutlined /></template>
              </a-input-password>
            </a-form-item>

            <div class="form-options">
              <a-checkbox v-model:checked="rememberAccount">记住账号</a-checkbox>
              <button class="text-button" type="button" @click="showPasswordHelp">
                忘记密码？
              </button>
            </div>

            <a-button
              class="login-button"
              type="primary"
              html-type="submit"
              :loading="loading"
            >
              登录
            </a-button>
          </a-form>

          <div class="security-note">
            <SafetyCertificateOutlined />
            <div>
              <strong>企业级安全认证</strong>
              <span>登录信息经加密通道传输</span>
            </div>
          </div>
        </div>

        <footer class="login-footer">工业风机 IoT 管理平台</footer>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import {
  ApiOutlined,
  DashboardOutlined,
  GlobalOutlined,
  LineChartOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { markRaw, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const REMEMBERED_USERNAME_KEY = 'fan_iot_remembered_username'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const savedUsername = localStorage.getItem(REMEMBERED_USERNAME_KEY) || ''
const rememberAccount = ref(Boolean(savedUsername))

const form = reactive({
  username: savedUsername,
  password: ''
})

const features = [
  {
    title: '设备连接',
    description: '稳定接入 · 统一管理',
    icon: markRaw(ApiOutlined)
  },
  {
    title: '实时监控',
    description: '全面感知 · 实时数据',
    icon: markRaw(DashboardOutlined)
  },
  {
    title: '智能分析',
    description: '趋势洞察 · 智能预警',
    icon: markRaw(LineChartOutlined)
  },
  {
    title: '安全可靠',
    description: '权限管控 · 数据可信',
    icon: markRaw(SafetyCertificateOutlined)
  }
]

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  loading.value = true
  try {
    await authStore.login(form)
    if (rememberAccount.value) {
      localStorage.setItem(REMEMBERED_USERNAME_KEY, form.username)
    } else {
      localStorage.removeItem(REMEMBERED_USERNAME_KEY)
    }
    message.success('登录成功')
    await router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}

function showPasswordHelp() {
  message.info('请联系系统管理员重置密码')
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100vh;
  place-items: stretch;
  padding: 0;
  overflow: hidden;
  background: #04182b;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(480px, 36vw, 590px);
  width: 100%;
  min-height: 100vh;
  background: transparent;
}

.login-visual {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background: transparent;
}

.login-hero {
  position: absolute;
  z-index: 0;
  top: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transform: translateX(clamp(-240px, -16vw, -150px));
}

.visual-shade {
  position: absolute;
  z-index: 0;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(2, 17, 32, 0.22), rgba(2, 17, 32, 0.08) 48%, rgba(2, 17, 32, 0.88)),
    linear-gradient(90deg, rgba(2, 17, 32, 0.82), rgba(2, 17, 32, 0.08) 68%);
}

.visual-copy {
  position: absolute;
  z-index: 1;
  top: clamp(78px, 14vh, 148px);
  left: clamp(42px, 5.2vw, 84px);
  max-width: 650px;
  color: #ffffff;
}

.visual-eyebrow {
  margin: 0 0 18px;
  color: #63c1ff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
}

.visual-copy h1 {
  margin: 0;
  font-size: clamp(38px, 3.3vw, 56px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
  text-shadow: 0 3px 16px rgba(0, 0, 0, 0.28);
}

.visual-subtitle {
  margin: 22px 0 28px;
  color: rgba(240, 247, 255, 0.9);
  font-size: clamp(17px, 1.35vw, 22px);
  line-height: 1.6;
}

.visual-accent {
  display: block;
  width: 68px;
  height: 3px;
  background: #169bff;
}

.feature-grid {
  position: absolute;
  z-index: 1;
  right: clamp(30px, 4vw, 64px);
  bottom: clamp(40px, 7vh, 76px);
  left: clamp(30px, 4vw, 64px);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  color: #ffffff;
}

.feature-item {
  display: grid;
  grid-template-rows: 42px auto;
  justify-items: center;
  gap: 13px;
  padding: 0 16px;
  text-align: center;
}

.feature-item + .feature-item {
  border-left: 1px solid rgba(139, 188, 230, 0.16);
}

.feature-icon {
  color: #1ba5ff;
  font-size: 38px;
}

.feature-item strong,
.feature-item span {
  display: block;
}

.feature-item strong {
  font-size: 16px;
  font-weight: 600;
}

.feature-item span {
  margin-top: 7px;
  color: rgba(218, 233, 247, 0.72);
  font-size: 12px;
  line-height: 1.5;
}

.login-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin: clamp(16px, 2vw, 28px) clamp(16px, 2vw, 28px) clamp(16px, 2vw, 28px) 0;
  padding: clamp(40px, 4vw, 66px) clamp(44px, 5vw, 76px) 32px;
  border-radius: 10px;
  background: #ffffff;
  color: #182230;
  box-shadow: 0 24px 70px rgba(2, 16, 29, 0.34);
}

.language-indicator {
  display: flex;
  align-items: center;
  align-self: flex-end;
  gap: 9px;
  color: #667085;
  font-size: 14px;
}

.language-indicator :deep(svg) {
  font-size: 17px;
}

.login-content {
  width: 100%;
  max-width: 440px;
  margin: auto;
}

.login-heading {
  margin-bottom: 34px;
}

.mobile-brand {
  display: none;
}

.login-heading h2 {
  margin: 0;
  color: #17202d;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0;
}

.login-heading p {
  margin: 12px 0 0;
  color: #667085;
  font-size: 17px;
}

.login-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.ant-input-affix-wrapper) {
  min-height: 56px;
  padding: 0 17px;
  border: 1px solid #d7dee8;
  border-radius: 6px;
  background: #ffffff;
  color-scheme: light;
  box-shadow: none;
}

.login-form :deep(.ant-input-affix-wrapper:hover) {
  border-color: #aebdce;
}

.login-form :deep(.ant-input-affix-wrapper-focused) {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.1);
}

.login-form :deep(.ant-input-prefix) {
  margin-right: 10px;
  color: #667085;
  font-size: 18px;
}

.login-form :deep(.ant-input) {
  background: #ffffff;
  color: #182230;
  caret-color: #182230;
  color-scheme: light;
  font-size: 15px;
}

.login-form :deep(.ant-input:-webkit-autofill),
.login-form :deep(.ant-input:-webkit-autofill:hover),
.login-form :deep(.ant-input:-webkit-autofill:focus),
.login-form :deep(.ant-input:-webkit-autofill:active) {
  -webkit-text-fill-color: #182230;
  caret-color: #182230;
  -webkit-box-shadow: 0 0 0 1000px #ffffff inset;
  box-shadow: 0 0 0 1000px #ffffff inset;
  transition: background-color 9999s ease-out 0s;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 24px;
}

.form-options :deep(.ant-checkbox-wrapper) {
  color: #475467;
  font-size: 14px;
}

.text-button {
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  font-size: 14px;
}

.text-button:hover {
  color: #0958d9;
}

.login-button {
  width: 100%;
  min-height: 56px;
  border: 0;
  border-radius: 6px;
  background: #1677ff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.2);
}

.login-button:hover,
.login-button:focus {
  background: #0958d9;
}

.security-note {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-top: 34px;
  padding-top: 28px;
  border-top: 1px solid #e7ecf2;
  color: #1677ff;
}

.security-note > :deep(svg) {
  font-size: 30px;
}

.security-note strong,
.security-note span {
  display: block;
}

.security-note strong {
  color: #344054;
  font-size: 14px;
  font-weight: 600;
}

.security-note span {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.login-footer {
  margin-top: auto;
  padding-top: 30px;
  color: #98a2b3;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: minmax(0, 1fr) 480px;
  }

  .feature-item {
    padding: 0 8px;
  }

  .feature-item span {
    display: none;
  }
}

@media (max-width: 820px) {
  .login-page {
    padding: 0;
    background: #ffffff;
  }

  .login-hero,
  .visual-shade {
    display: none;
  }

  .login-shell {
    display: block;
    width: 100%;
    min-height: 100vh;
    border-radius: 0;
    box-shadow: none;
  }

  .login-visual {
    display: none;
  }

  .login-panel {
    min-height: 100vh;
    margin: 0;
    padding: 30px clamp(24px, 8vw, 54px);
    border-radius: 0;
    box-shadow: none;
  }

  .login-content {
    margin: auto;
  }

  .mobile-brand {
    display: block;
    margin-bottom: 16px;
    color: #1677ff;
    font-size: 14px;
    font-weight: 600;
  }

  .login-heading h2 {
    font-size: 30px;
  }
}

@media (max-height: 720px) and (min-width: 821px) {
  .login-shell {
    min-height: 100vh;
  }

  .visual-copy {
    top: 60px;
  }

  .feature-grid {
    bottom: 34px;
  }

  .login-panel {
    padding-top: 30px;
    padding-bottom: 22px;
  }

  .login-heading {
    margin-bottom: 24px;
  }

  .security-note {
    margin-top: 24px;
    padding-top: 22px;
  }
}
</style>
