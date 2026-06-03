export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface Customer {
  id: number
  customerId: string
  customerName: string
  contactPerson?: string
  contactPhone?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface Project {
  id: number
  projectId: string
  customerId: string
  customerName?: string
  projectName: string
  location?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface Gateway {
  id: number
  gatewayId: string
  gatewaySn: string
  gatewayName: string
  gatewayModel?: string
  imei?: string
  simCardNo?: string
  customerId?: string
  customerName?: string
  projectId?: string
  projectName?: string
  activationStatus: string
  onlineStatus: string
  mqttUsername?: string
  mqttPassword?: string
  firmwareVersion?: string
  latitude?: number
  longitude?: number
  address?: string
  province?: string
  city?: string
  district?: string
  locationUpdatedAt?: string
  lastSeenAt?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface Device {
  id: number
  deviceId: string
  gatewayId: string
  gatewayName?: string
  customerId?: string
  customerName?: string
  projectId?: string
  projectName?: string
  deviceName: string
  deviceModel?: string
  installLocation?: string
  latitude?: number
  longitude?: number
  address?: string
  status: string
  lastSeenAt?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface DeviceLocation {
  deviceId: string
  deviceName: string
  gatewayId: string
  customerId?: string
  projectId?: string
  installLocation?: string
  latitude?: number
  longitude?: number
  address?: string
}

export interface AiDeviceMeta {
  deviceId: string
  deviceName: string
  gatewayId: string
  customerId?: string
  customerName?: string
  projectId?: string
  projectName?: string
  latitude?: number
  longitude?: number
  address?: string
}

export interface AiRealtime {
  device: AiDeviceMeta
  telemetry: RealtimeMetric | null
}

export interface AiHistory {
  device: AiDeviceMeta
  records: TelemetryHistoryPoint[]
}

export interface RealtimeMetric {
  gatewayId: string
  deviceId: string
  deviceName: string
  status: string
  rpm?: number
  current?: number
  voltage?: number
  power?: number
  frequency?: number
  pressure?: number
  airflow?: number
  motorTemperature?: number
  bearingTemperature?: number
  vibration?: number
  alarmCode?: string
  timestamp?: string
  receivedAt?: string
}

export interface RealtimeOverview {
  onlineGateways: number
  offlineGateways: number
  onlineDevices: number
  offlineDevices: number
  alarmDevices: number
  activeAlarms: number
  latestDevices: RealtimeMetric[]
}

export interface TelemetryHistoryPoint {
  timestamp: string
  rpm?: number
  current?: number
  voltage?: number
  power?: number
  motorTemperature?: number
  bearingTemperature?: number
  vibration?: number
}

export interface Alarm {
  id: number
  alarmId: string
  gatewayId?: string
  deviceId: string
  alarmType: string
  alarmLevel: string
  alarmCode?: string
  alarmMessage: string
  occurredAt: string
  recoveredAt?: string
  acknowledged: boolean
  acknowledgedAt?: string
  status: string
  createdAt?: string
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description?: string
  enabled: boolean
}

export interface UserAccount {
  id: number
  username: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  status: string
  roleCodes: string[]
  roleNames: string[]
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface SystemLog {
  id: number
  logType: string
  logLevel: string
  module?: string
  operation?: string
  message: string
  userId?: number
  createdAt?: string
}
