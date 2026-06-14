import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type {
  Alarm,
  AiHistory,
  AiRealtime,
  Customer,
  D200ConfigTask,
  D200FieldMapping,
  D200RawPayload,
  Device,
  DeviceLocation,
  Gateway,
  LbsLocationResult,
  PageResponse,
  Project,
  RealtimeMetric,
  RealtimeOverview,
  Role,
  SystemLog,
  TelemetryHistoryPoint,
  UserAccount
} from '@/types/business'

export interface PageQuery {
  keyword?: string
  customerId?: string
  projectId?: string
  gatewayId?: string
  status?: string
  alarmLevel?: string
  deviceId?: string
  start?: string
  end?: string
  limit?: number
  page?: number
  size?: number
}

function cleanParams(params: object) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== undefined && value !== null)
  )
}

async function getData<T>(promise: Promise<{ data: ApiResponse<T> }>) {
  const response = await promise
  return response.data.data
}

export const customerApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<Customer>>>('/api/customers', { params: cleanParams(params) })),
  create: (data: Partial<Customer>) => getData(request.post<ApiResponse<Customer>>('/api/customers', data)),
  update: (id: number, data: Partial<Customer>) => getData(request.put<ApiResponse<Customer>>(`/api/customers/${id}`, data)),
  remove: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/customers/${id}`))
}

export const projectApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<Project>>>('/api/projects', { params: cleanParams(params) })),
  options: (params: Pick<PageQuery, 'customerId'> = {}) =>
    getData(request.get<ApiResponse<Project[]>>('/api/projects/options', { params: cleanParams(params) })),
  create: (data: Partial<Project>) => getData(request.post<ApiResponse<Project>>('/api/projects', data)),
  update: (id: number, data: Partial<Project>) => getData(request.put<ApiResponse<Project>>(`/api/projects/${id}`, data)),
  remove: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/projects/${id}`))
}

export const gatewayApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<Gateway>>>('/api/gateways', { params: cleanParams(params) })),
  options: (params: Pick<PageQuery, 'customerId' | 'projectId'> = {}) =>
    getData(request.get<ApiResponse<Gateway[]>>('/api/gateways/options', { params: cleanParams(params) })),
  create: (data: Partial<Gateway>) => getData(request.post<ApiResponse<Gateway>>('/api/gateways', data)),
  update: (id: number, data: Partial<Gateway>) => getData(request.put<ApiResponse<Gateway>>(`/api/gateways/${id}`, data)),
  remove: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/gateways/${id}`))
}

export const deviceApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<Device>>>('/api/devices', { params: cleanParams(params) })),
  create: (data: Partial<Device>) => getData(request.post<ApiResponse<Device>>('/api/devices', data)),
  update: (id: number, data: Partial<Device>) => getData(request.put<ApiResponse<Device>>(`/api/devices/${id}`, data)),
  remove: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/devices/${id}`)),
  location: (deviceId: string) => getData(request.get<ApiResponse<DeviceLocation>>(`/api/devices/${deviceId}/location`)),
  updateLocation: (deviceId: string, data: Partial<DeviceLocation>) =>
    getData(request.put<ApiResponse<DeviceLocation>>(`/api/devices/${deviceId}/location`, data))
}

export const monitorApi = {
  overview: () => getData(request.get<ApiResponse<RealtimeOverview>>('/api/monitor/overview')),
  latest: (params: Pick<PageQuery, 'deviceId' | 'limit'> = {}) =>
    getData(request.get<ApiResponse<RealtimeMetric[]>>('/api/monitor/latest', { params: cleanParams(params) })),
  history: (params: Required<Pick<PageQuery, 'deviceId'>> & Pick<PageQuery, 'start' | 'end' | 'limit'>) =>
    getData(request.get<ApiResponse<TelemetryHistoryPoint[]>>('/api/monitor/history', { params: cleanParams(params) }))
}

export const alarmApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<Alarm>>>('/api/alarms', { params: cleanParams(params) })),
  acknowledge: (id: number) => getData(request.post<ApiResponse<void>>(`/api/alarms/${id}/acknowledge`)),
  close: (id: number) => getData(request.post<ApiResponse<void>>(`/api/alarms/${id}/close`))
}

export const aiApi = {
  realtime: (deviceId: string, apiKey: string) =>
    getData(request.get<ApiResponse<AiRealtime>>(`/api/ai/realtime/${deviceId}`, { headers: { 'X-API-Key': apiKey } })),
  history: (params: Required<Pick<PageQuery, 'deviceId'>> & Pick<PageQuery, 'start' | 'end' | 'limit'>, apiKey: string) =>
    getData(request.get<ApiResponse<AiHistory>>('/api/ai/history', { params: cleanParams(params), headers: { 'X-API-Key': apiKey } })),
  exportData: (data: { deviceId: string; format: 'json' | 'csv'; start?: string; end?: string; limit?: number }, apiKey: string) =>
    request.post('/api/ai/export', data, { headers: { 'X-API-Key': apiKey }, responseType: data.format === 'csv' ? 'blob' : 'json' })
}

export const userApi = {
  page: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<UserAccount>>>('/api/users', { params: cleanParams(params) })),
  roles: () => getData(request.get<ApiResponse<Role[]>>('/api/users/roles')),
  create: (data: Partial<UserAccount>) => getData(request.post<ApiResponse<UserAccount>>('/api/users', data)),
  update: (id: number, data: Partial<UserAccount>) => getData(request.put<ApiResponse<UserAccount>>(`/api/users/${id}`, data)),
  remove: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/users/${id}`))
}

export const systemLogApi = {
  page: (params: PageQuery & { logLevel?: string; module?: string }) =>
    getData(request.get<ApiResponse<PageResponse<SystemLog>>>('/api/system/logs', { params: cleanParams(params) }))
}

export const d200Api = {
  rawPayloads: (params: PageQuery & { gatewaySn?: string; processed?: boolean }) =>
    getData(request.get<ApiResponse<PageResponse<D200RawPayload>>>('/api/d200/raw-payloads', { params: cleanParams(params) })),
  mappings: (params: PageQuery) =>
    getData(request.get<ApiResponse<PageResponse<D200FieldMapping>>>('/api/d200/field-mappings', { params: cleanParams(params) })),
  createMapping: (data: Partial<D200FieldMapping>) =>
    getData(request.post<ApiResponse<D200FieldMapping>>('/api/d200/field-mappings', data)),
  updateMapping: (id: number, data: Partial<D200FieldMapping>) =>
    getData(request.put<ApiResponse<D200FieldMapping>>(`/api/d200/field-mappings/${id}`, data)),
  removeMapping: (id: number) => getData(request.delete<ApiResponse<void>>(`/api/d200/field-mappings/${id}`)),
  configTasks: (params: PageQuery & { gatewaySn?: string; status?: string }) =>
    getData(request.get<ApiResponse<PageResponse<D200ConfigTask>>>('/api/d200/config-tasks', { params: cleanParams(params) })),
  createConfigTask: (data: Partial<D200ConfigTask>) =>
    getData(request.post<ApiResponse<D200ConfigTask>>('/api/d200/config-tasks', data)),
  markConfigTask: (id: number, status: string, errorMessage = '') =>
    getData(request.post<ApiResponse<void>>(`/api/d200/config-tasks/${id}/status`, null, { params: cleanParams({ status, errorMessage }) }))
}

export const lbsApi = {
  locate: (data: { gatewaySn?: string; imei?: string; mcc?: number; mnc?: number; lac: number; cid: number }) =>
    getData(request.post<ApiResponse<LbsLocationResult>>('/api/iot/location/lbs', data)),
  map: (params: { longitude: number; latitude: number; zoom?: number; width?: number; height?: number }) =>
    request.get<Blob>('/api/iot/location/map', {
      params: cleanParams(params),
      responseType: 'blob'
    })
}
