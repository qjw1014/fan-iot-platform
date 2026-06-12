<template>
  <el-dialog v-model="visible" title="设备位置" width="920px" class="device-location-dialog" destroy-on-close>
    <div class="location-layout">
      <div class="map-area">
        <iframe
          v-if="hasCoordinate"
          :src="mapUrl"
          class="map-frame"
          title="设备地图"
          loading="lazy"
        />
        <el-empty v-else description="暂无定位坐标，请先维护安装位置或获取盒子基站定位" />
      </div>

      <aside class="location-panel">
        <div class="device-title">{{ location.deviceName || device?.deviceName }}</div>
        <div class="location-meta">
          <span>定位来源</span>
          <el-tag :type="location.locationSource === 'lbs' ? 'warning' : 'primary'" effect="dark">
            {{ sourceLabel }}
          </el-tag>
        </div>
        <div class="location-meta">
          <span>最后定位</span>
          <strong>{{ formatDateTime(location.lastLocationTime) }}</strong>
        </div>

        <el-form label-position="top">
          <div class="coordinate-grid">
            <el-form-item label="经度">
              <el-input-number
                v-model="form.longitude"
                :min="-180"
                :max="180"
                :precision="7"
                :step="0.000001"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="纬度">
              <el-input-number
                v-model="form.latitude"
                :min="-90"
                :max="90"
                :precision="7"
                :step="0.000001"
                controls-position="right"
              />
            </el-form-item>
          </div>
          <el-form-item label="安装位置">
            <el-input v-model="form.installLocation" placeholder="例如：一号车间东侧风机房" />
          </el-form-item>
          <el-form-item label="详细地址">
            <el-input v-model="form.address" placeholder="请输入设备安装地址" />
          </el-form-item>
        </el-form>

        <el-alert
          v-if="location.locationSource === 'lbs'"
          title="当前显示盒子基站定位，精度通常低于人工安装位置"
          type="warning"
          :closable="false"
          show-icon
        />
      </aside>
    </div>

    <template #footer>
      <el-button v-if="hasCoordinate" :icon="Position" @click="openExternalMap">在百度地图中打开</el-button>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="saving" @click="saveLocation">保存人工位置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { deviceApi } from '@/api/business'
import type { Device, DeviceLocation } from '@/types/business'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  modelValue: boolean
  device: Device | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const location = reactive<Partial<DeviceLocation>>({})
const form = reactive<Partial<DeviceLocation>>({})

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const hasCoordinate = computed(() => location.longitude != null && location.latitude != null)
const sourceLabel = computed(() => {
  if (!hasCoordinate.value) return '未定位'
  return location.locationSource === 'lbs' ? '盒子基站定位' : '人工安装位置'
})
const mapUrl = computed(() => {
  if (!hasCoordinate.value) return ''
  const title = encodeURIComponent(location.deviceName || props.device?.deviceName || '设备位置')
  const content = encodeURIComponent(location.address || location.installLocation || '')
  return `https://api.map.baidu.com/marker?location=${location.latitude},${location.longitude}&title=${title}&content=${content}&output=html&src=fan-iot-platform`
})

watch(
  () => [props.modelValue, props.device?.deviceId],
  async ([open]) => {
    if (!open || !props.device) return
    const data = await deviceApi.location(props.device.deviceId)
    Object.assign(location, data)
    Object.assign(form, {
      longitude: data.longitude,
      latitude: data.latitude,
      installLocation: data.installLocation || '',
      address: data.address || ''
    })
  }
)

async function saveLocation() {
  if (!props.device) return
  if ((form.longitude == null) !== (form.latitude == null)) {
    ElMessage.warning('经度和纬度需要同时填写')
    return
  }
  saving.value = true
  try {
    const data = await deviceApi.updateLocation(props.device.deviceId, form)
    Object.assign(location, data)
    ElMessage.success('设备位置已保存')
    emit('saved')
  } finally {
    saving.value = false
  }
}

function openExternalMap() {
  window.open(mapUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.location-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.8fr);
  gap: 16px;
}
.map-area {
  display: grid;
  min-height: 480px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #07111f;
  place-items: stretch;
}
.map-frame {
  width: 100%;
  height: 100%;
  min-height: 480px;
  border: 0;
}
.location-panel {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(8, 20, 36, 0.72);
}
.device-title {
  margin-bottom: 16px;
  color: var(--text);
  font-size: 18px;
  font-weight: 700;
}
.location-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  color: var(--muted);
}
.location-meta strong {
  color: var(--text);
  font-size: 13px;
  font-weight: 500;
  text-align: right;
}
.coordinate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}
.coordinate-grid :deep(.el-input-number) {
  width: 100%;
}
@media (max-width: 760px) {
  :global(.device-location-dialog) {
    width: calc(100vw - 24px) !important;
    margin-top: 12px !important;
  }
  .location-layout {
    grid-template-columns: 1fr;
  }
  .coordinate-grid {
    grid-template-columns: 1fr;
  }
  .map-area,
  .map-frame {
    min-height: 300px;
  }
}
</style>
