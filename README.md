# 工业风机物联网私有云平台

当前完成范围：Phase 8，并补充无真实物联网盒子时的模拟测试能力。

平台部署在云服务器，负责物联网盒子管理、客户管理、项目管理、设备管理、数据接收、实时监控、历史数据、告警管理、定位管理、AI开放接口、用户角色管理和系统日志审计。

现场设备由物联网盒子通过 Modbus TCP 采集，云平台不直接连接现场设备，只接收盒子通过 MQTT 或 HTTP 上传的数据。

## 技术栈

- 前端：Vue3、TypeScript、Vite、Pinia、Vue Router、Element Plus、ECharts
- 后端：Spring Boot 3、Java 17、Maven
- 数据库：PostgreSQL
- 缓存：Redis
- 通信：MQTT、HTTP REST API
- 文档：Swagger/OpenAPI
- 部署：Docker、Docker Compose

## 本地启动

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

访问地址：

```text
前端：http://localhost:5173
后端：http://localhost:8080
Swagger：http://localhost:8080/swagger-ui.html
```

默认登录：

```text
账号：admin
密码：please_change_admin_password
```

## 无真实盒子时如何测试

已提供固定模拟对象：

```text
gatewayId = BOX001
deviceId  = FAN001
MQTT Topic = iot/gateway/BOX001/telemetry
```

脚本会自动准备模拟客户、项目、盒子和设备，并激活 BOX001。随后每 5 秒上传一次风机数据，字段包含：

```text
rpm、current、voltage、power、temperature、motorTemperature、vibration、status、alarmCode
```

同时支持 HTTP 和 MQTT 两种模拟方式：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Both
```

只测试 HTTP：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Http
```

只测试 MQTT：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Mqtt
```

运行固定次数后退出，例如上传 6 轮：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/simulate-box001-fan001.ps1 -Mode Both -Iterations 6
```

验证数据入库：

```powershell
docker exec fan-iot-postgres psql -U fan_iot -d fan_iot_platform -c "select gateway_id, device_id, rpm, motor_temperature, vibration, status, received_at from device_telemetry where gateway_id='BOX001' and device_id='FAN001' order by id desc limit 10;"
```

前端验证：

- 打开“实时监控”，可看到 `FAN001` 最新数据持续变化。
- 打开“历史数据”，选择 `FAN001`，可查看温度、振动、功率、转速曲线。
- 打开“告警中心”，可查看温度超限和振动超限告警。

告警测试规则：

- `motorTemperature > 80` 生成温度超限告警。
- `vibration > 5` 生成振动超限告警。

## 核心接口

IoT 数据接入：

```text
POST /api/iot/telemetry
MQTT iot/gateway/{gatewayId}/telemetry
```

实时监控与历史数据：

```text
GET /api/monitor/overview
GET /api/monitor/latest
GET /api/monitor/history
```

AI 开放接口：

```text
GET  /api/ai/realtime/{deviceId}
GET  /api/ai/history
POST /api/ai/export
```

用户与日志：

```text
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
GET    /api/users/roles
GET    /api/system/logs
```

## 构建验证

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

Docker Compose：

```powershell
docker compose up -d --build
docker compose ps
```
