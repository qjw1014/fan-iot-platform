# 工业风机物联网云平台

## 腾讯云部署

生产环境推荐使用 Ubuntu 22.04 LTS、4 核 4GB 内存及以上配置。腾讯云部署文件位于
`deploy/tencent-cloud`，默认只开放网页 `80/tcp` 和 D200 MQTT `1883/tcp`，
PostgreSQL、Redis、Spring Boot 均不直接暴露公网。详细命令见
`deploy/tencent-cloud/README.md`。

工业风机物联网私有云平台，面向云服务器部署。现场侧使用 D200 串口 DTU 通过 RS232 / RS485 / TTL 连接设备或仪表，D200 通过 4G 和 MQTT 上传 JSON 数据，云平台不直接连接现场设备。

## 技术栈

- 后端：Spring Boot 3、Java 17、Maven
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、ECharts
- 数据库：PostgreSQL
- 缓存：Redis
- MQTT Broker：Mosquitto 或 EMQX
- 部署：Docker Compose

## D200 接入说明

### 设备地图与定位

- 设备管理页可点击“位置”打开地图弹窗，维护设备安装地址和经纬度。
- 设备自身未维护坐标时，平台自动显示所属 D200 盒子的定位坐标。
- 定位优先级为：设备人工安装位置 > D200 盒子基站定位。
- 首页设备地图仅展示已有真实坐标的设备，不生成模拟坐标。
- D200 基站定位通过 `AT+LBS` 返回 LAC/CID，需要接入第三方基站数据库换算经纬度后再写入平台；设备本身不会直接返回 GPS 坐标。

### D200 基站定位自动流程

平台支持以下定位报文格式，MQTT Topic 仍为 `iot/d200/{gatewaySn}/up`：

```json
{
  "imei": "861450085262124",
  "lbs": {
    "mcc": 460,
    "mnc": 3,
    "cid": 144426439,
    "lac": 21269
  }
}
```

也支持厂家 AT 指令原始返回值：

```json
{
  "imei": "861450085262124",
  "lbs": "+LBS:144426439,21269"
}
```

处理流程：

1. 按 Topic 中的 SN 或报文 IMEI 匹配盒子。
2. 调用配置的基站定位服务，将 MCC/MNC/LAC/CID 转换为经纬度。
3. 写入 `d200_lbs_location_logs`，成功和失败均可审计。
4. 更新盒子的 LBS 参数、坐标、精度、地址和最后定位时间。
5. 已存在完整人工坐标时只保存 LBS 查询结果，不覆盖人工位置。
6. 未维护设备坐标时，设备地图自动继承盒子的基站位置。

HTTP 联调接口：

```text
POST /api/iot/location/lbs
```

```json
{
  "gatewaySn": "00100326052100060534",
  "mcc": 460,
  "mnc": 3,
  "cid": 144426439,
  "lac": 21269
}
```

开发环境可使用 Cellocation：

```dotenv
LBS_ENABLED=true
LBS_PROVIDER=cellocation
LBS_BASE_URL=http://api.cellocation.com:84/cell/
LBS_DEFAULT_MCC=460
LBS_DEFAULT_MNC=3
```

生产环境推荐使用高德智能硬件定位，并在高德开放平台申请 Web 服务 Key：

```dotenv
LBS_ENABLED=true
LBS_PROVIDER=amap
LBS_BASE_URL=https://restapi.amap.com/v5/position/IoT
LBS_API_KEY=your_amap_web_service_key
LBS_DEFAULT_MCC=460
LBS_DEFAULT_MNC=3
LBS_NETWORK=GSM
LBS_SIGNAL=-99
```

`MNC=3` 仅适用于当前测试配置，正式部署应按实际 SIM 运营商填写。定位供应商不可用时，平台会记录失败日志，并保留原坐标。

D200 最终按“串口 DTU + 4G MQTT 上报”接入：

- D200 无网口，不采集 Modbus TCP。
- D200 只通过 RS232 / RS485 / TTL 串口连接现场侧。
- D200 支持标准 MQTT 私有服务器，服务器需要公网 IP。
- D200 支持 MQTT 3.1 / 3.1.1。
- D200 支持 Client ID、用户名、密码。
- D200 不支持 TLS / SSL，本项目默认 `tls_enabled=false`。
- D200 只支持一个发布 Topic 和一个订阅 Topic。
- D200 JSON 格式固定，但 `data` 内键名可以配置。
- D200 可上报 IMEI、ICCID、设备时间。
- D200 不支持 GPS，只支持基站定位。
- D200 离线缓存约 20 条数据，平台允许延迟到达数据按设备时间入库。
- D200 可通过 SN 添加到私有云平台；远程配置任务先记录在平台，底层协议确认后再接入实际下发。

## MQTT 参数配置

Broker 可使用 EMQX 或 Mosquitto：

- 开启 1883 端口。
- 支持 MQTT 3.1 / 3.1.1。
- 不启用 TLS。
- 每台 D200 使用独立 `clientId / username / password`。
- 每台 D200 只允许发布自己的上行 Topic。
- 每台 D200 只允许订阅自己的下行 Topic。

Topic 规则：

```text
上行：iot/d200/{gatewaySn}/up
下行：iot/d200/{gatewaySn}/down
```

示例：

```text
iot/d200/BOX001-SN/up
iot/d200/BOX001-SN/down
```

## D200 JSON 报文

D200 上行示例：

```json
{
  "imei": "860061060041515",
  "time": 1681105255,
  "iccid": "89860620220031659407",
  "data": {
    "rpm": 1450,
    "current": 12.3,
    "voltage": 380,
    "power": 7.8,
    "motorTemperature": 72.5,
    "vibration": 2.1,
    "status": "running"
  }
}
```

平台处理流程：

1. MQTT 收到报文后先保存到 `d200_raw_payloads`。
2. 根据 Topic 中的 `gatewaySn`、报文中的 `imei` 匹配 `gateways`。
3. 保存 `device_time` 和 `received_at`。
4. 如果 `data` 内为标准字段，直接写入 `device_telemetry`。
5. 如果 `data` 内为 `k1/k2/k3` 等非标准字段，使用 `d200_field_mappings` 映射。
6. 如果无法映射，不报错，只保存原始报文和处理说明。

## 字段映射说明

平台标准字段：

```text
rpm
current
voltage
power
frequency
pressure
airflow
motorTemperature
bearingTemperature
vibration
status
alarmCode
```

非标准字段示例：

```json
{
  "data": {
    "k1": 1450,
    "k2": 72.5,
    "k3": 2.1
  }
}
```

需要在“D200字段映射”页面配置：

```text
k1 -> rpm
k2 -> motorTemperature
k3 -> vibration
```

可配置倍率、偏移和单位，例如温度寄存器值需要除以 10，可设置 `scale_factor=0.1`。

## 定位说明

D200 不支持 GPS。本平台定位优先级：

1. 后台手动维护的设备/盒子安装地址和经纬度，`location_source=manual`。
2. D200 基站定位结果，`location_source=lbs`。

平台不再依赖 GPS 字段。

## MQTTX 测试

连接参数：

```text
Host: 服务器IP
Port: 1883
Protocol: MQTT 3.1.1
TLS: 关闭
Client ID: BOX001-SN
Username: box001
Password: box001_password
```

发布 Topic：

```text
iot/d200/BOX001-SN/up
```

测试 Payload：

```json
{
  "imei": "860061060041515",
  "iccid": "89860620220031659407",
  "time": 1681105255,
  "data": {
    "rpm": 1450,
    "current": 12.3,
    "voltage": 380,
    "power": 7.8,
    "motorTemperature": 82.5,
    "vibration": 2.1,
    "status": "warning",
    "alarmCode": "TEMP_HIGH"
  }
}
```

## Docker 启动

复制环境变量：

```powershell
Copy-Item .env.example .env
```

启动：

```powershell
docker compose up -d --build
```

访问：

```text
前端：http://localhost:5173
后端：http://localhost:8080
Swagger：http://localhost:8080/swagger-ui/index.html
```

默认账号：

```text
admin
please_change_admin_password
```

## 无真实盒子时如何测试

启动 Docker 后运行模拟脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Both
```

说明：

- HTTP 仍会调用兼容接口 `POST /api/iot/telemetry`。
- MQTT 会发布 D200 格式 JSON 到 `iot/d200/BOX001-SN/up`。
- 平台会先保存原始报文，再写入标准 `device_telemetry`。
- 温度大于 80 会触发温度超限告警。
- 振动大于 5 会触发振动超限告警。

只测试 MQTT：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Mqtt
```

查看页面：

- 盒子管理：D200 接入参数
- D200字段映射：`k1/k2/k3` 到标准字段映射
- D200原始数据：MQTT 原始 JSON
- D200远程配置：远程配置任务状态
- 实时监控：标准字段实时数据
- 历史数据：标准字段曲线
- 告警中心：温度/振动告警

## 构建检查

后端：

```powershell
cd backend
mvn clean package
```

前端：

```powershell
cd frontend
npm run build
```
## 通信在线状态

- 盒子在线状态由平台最近一次收到 MQTT 或 HTTP 上行数据的时间自动维护。
- 收到有效上行数据时，平台更新 `last_seen_at` 并将盒子标记为在线。
- 默认连续 180 秒没有收到上行数据时，后台任务自动将盒子及其设备标记为离线。
- 在线状态不可在盒子编辑页面人工修改。
- `COMMUNICATION_OFFLINE_TIMEOUT_SECONDS` 用于调整离线超时时间。
- `COMMUNICATION_STATUS_CHECK_INTERVAL_MS` 用于调整检测周期。
- D200 应配置周期性数据或心跳上报；仅保持 MQTT TCP 连接但不发送业务报文时，平台仍会在超时后显示离线。
