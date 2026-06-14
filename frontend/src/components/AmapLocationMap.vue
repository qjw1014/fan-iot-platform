<template>
  <div class="location-map" :style="{ minHeight: `${height}px` }">
    <template v-if="hasCoordinate">
      <img
        v-if="imageUrl"
        :src="imageUrl"
        :alt="`${title || '设备'}定位地图`"
        class="map-image"
      />
      <div v-else class="map-state">
        <a-spin v-if="loading" tip="正在加载高德地图" />
        <a-result
          v-else
          status="warning"
          title="地图加载失败"
          :sub-title="error || '请检查高德 Web 服务 Key 和网络连接'"
        >
          <template #extra>
            <a-button @click="loadMap">重新加载</a-button>
          </template>
        </a-result>
      </div>

      <div class="map-toolbar">
        <a-tooltip title="缩小">
          <a-button :disabled="zoom <= 3" shape="circle" size="small" @click="zoom -= 1">
            <template #icon><MinusOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="放大">
          <a-button :disabled="zoom >= 18" shape="circle" size="small" @click="zoom += 1">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="刷新地图">
          <a-button shape="circle" size="small" :loading="loading" @click="loadMap">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="在高德地图中打开">
          <a-button shape="circle" size="small" @click="openExternalMap">
            <template #icon><ExportOutlined /></template>
          </a-button>
        </a-tooltip>
      </div>

      <div class="map-caption">
        <div>
          <EnvironmentFilled />
          <strong>{{ title || '定位坐标' }}</strong>
        </div>
        <span>{{ address || coordinateText }}</span>
      </div>
    </template>
    <a-empty v-else description="暂无定位坐标" />
  </div>
</template>

<script setup lang="ts">
import {
  EnvironmentFilled,
  ExportOutlined,
  MinusOutlined,
  PlusOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'

import { lbsApi } from '@/api/business'

const props = withDefaults(defineProps<{
  longitude?: number
  latitude?: number
  title?: string
  address?: string
  height?: number
}>(), {
  title: '',
  address: '',
  height: 360
})

const zoom = ref(15)
const loading = ref(false)
const error = ref('')
const imageUrl = ref('')

const hasCoordinate = computed(() =>
  Number.isFinite(props.longitude) && Number.isFinite(props.latitude)
)
const coordinateText = computed(() =>
  hasCoordinate.value ? `${props.longitude}, ${props.latitude}` : ''
)

watch(
  () => [props.longitude, props.latitude, zoom.value],
  () => loadMap(),
  { immediate: true }
)

onBeforeUnmount(revokeImage)

async function loadMap() {
  revokeImage()
  error.value = ''
  if (!hasCoordinate.value) return

  loading.value = true
  try {
    const response = await lbsApi.map({
      longitude: props.longitude as number,
      latitude: props.latitude as number,
      zoom: zoom.value,
      width: 760,
      height: Math.max(props.height, 260)
    })
    imageUrl.value = URL.createObjectURL(response.data)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '高德地图加载失败'
  } finally {
    loading.value = false
  }
}

function revokeImage() {
  if (!imageUrl.value) return
  URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
}

function openExternalMap() {
  if (!hasCoordinate.value) return
  const name = encodeURIComponent(props.title || '设备位置')
  const url = `https://uri.amap.com/marker?position=${props.longitude},${props.latitude}&name=${name}&src=fan-iot-platform&coordinate=gaode&callnative=0`
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.location-map {
  position: relative;
  display: grid;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel-strong);
  place-items: stretch;
}

.map-image {
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
}

.map-state {
  display: grid;
  min-height: inherit;
  padding: 24px;
  place-items: center;
}

.map-toolbar {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.16);
}

.map-caption {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(8, 20, 36, 0.86);
  color: #ffffff;
  backdrop-filter: blur(8px);
}

.map-caption div {
  display: flex;
  align-items: center;
  gap: 7px;
}

.map-caption span {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
