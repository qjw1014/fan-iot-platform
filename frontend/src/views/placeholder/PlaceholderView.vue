<template>
  <section class="placeholder panel">
    <div class="panel-title">{{ route.meta.title }}</div>
    <div class="placeholder-body">
      <a-empty description="当前阶段仅完成前端基础框架" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.placeholder {
  min-height: calc(100vh - 126px);
  padding: 18px;
}

.placeholder-body {
  display: grid;
  place-items: center;
  min-height: 360px;
}
</style>
