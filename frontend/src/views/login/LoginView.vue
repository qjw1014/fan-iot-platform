<template>
  <main class="tech-page login-page">
    <section class="login-visual">
      <div class="system-title">
        <span>Industrial Fan IoT Platform</span>
        <h1>工业风机物联网私有云平台</h1>
        <p>盒子接入、设备监控、告警联动、开放接口统一管理</p>
      </div>
      <div class="fan-stage" aria-hidden="true">
        <div class="fan-ring">
          <div class="fan-core"></div>
          <i></i>
          <i></i>
          <i></i>
        </div>
      </div>
    </section>

    <section class="login-panel panel">
      <div class="panel-title">平台登录</div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            type="password"
          />
        </el-form-item>
        <el-button class="login-button" type="primary" :loading="loading" @click="submit">
          登录系统
        </el-button>
      </el-form>
      <p class="login-tip">开发验证账号：admin / please_change_admin_password</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    await router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: center;
  gap: 56px;
  min-height: 100vh;
  padding: 48px clamp(24px, 7vw, 96px);
}

.login-visual {
  display: grid;
  gap: 40px;
}

.system-title span {
  color: #72c3ff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
}

.system-title h1 {
  margin: 14px 0 16px;
  color: #f2f8ff;
  font-size: 46px;
  line-height: 1.15;
}

.system-title p {
  max-width: 560px;
  margin: 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.8;
}

.fan-stage {
  position: relative;
  width: min(520px, 76vw);
  aspect-ratio: 16 / 7;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(47, 155, 255, 0.12), rgba(34, 197, 94, 0.08)),
    rgba(8, 20, 36, 0.64);
}

.fan-stage::before,
.fan-stage::after {
  position: absolute;
  content: "";
}

.fan-stage::before {
  right: 28px;
  bottom: 36px;
  left: 190px;
  height: 34px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
}

.fan-stage::after {
  right: 54px;
  bottom: 70px;
  left: 240px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #55bdff, transparent);
}

.fan-ring {
  position: absolute;
  top: 50%;
  left: 54px;
  width: 132px;
  height: 132px;
  border: 2px solid var(--line-strong);
  border-radius: 50%;
  transform: translateY(-50%);
}

.fan-core {
  position: absolute;
  inset: 46px;
  border-radius: 50%;
  background: #8bd3ff;
  box-shadow: 0 0 24px rgba(93, 190, 255, 0.9);
}

.fan-ring i {
  position: absolute;
  top: 61px;
  left: 64px;
  width: 54px;
  height: 16px;
  border-radius: 100% 8px 100% 8px;
  background: rgba(139, 211, 255, 0.82);
  transform-origin: 0 50%;
}

.fan-ring i:nth-child(3) {
  transform: rotate(120deg);
}

.fan-ring i:nth-child(4) {
  transform: rotate(240deg);
}

.login-panel {
  width: 100%;
  padding: 28px;
}

.login-button {
  width: 100%;
}

.login-tip {
  margin: 16px 0 0;
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .system-title h1 {
    font-size: 34px;
  }
}
</style>
