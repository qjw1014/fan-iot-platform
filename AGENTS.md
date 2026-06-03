# AGENTS.md

# Industrial Fan IoT Platform

## Project Overview

本项目为工业风机物联网私有云平台。

系统部署于云服务器。

本平台负责：

* 物联网盒子管理
* 盒子激活
* 客户管理
* 项目管理
* 设备管理
* 数据接收
* 数据存储
* 实时监控
* 历史数据查询
* 告警管理
* 数据导出
* AI开放接口

本平台不负责：

* PLC开发
* 变频器开发
* Modbus TCP点位配置
* 现场采集逻辑
* AI训练
* 大模型开发

---

# System Architecture

Industrial Fan / PLC / Inverter

↓

IoT Gateway / Edge Box

↓

MQTT or HTTP

↓

Private Cloud Platform

↓

PostgreSQL

↓

AI Open API

↓

Customer AI / LLM Platform

---

# Project Scope

物联网盒子已经具备采集能力。

盒子通过 Modbus TCP 获取现场设备数据。

Modbus TCP 配置工作由设备厂商或代理技术人员完成。

本系统不涉及：

* 寄存器配置
* 点位配置
* 功能码配置
* PLC地址配置

本系统只负责接收盒子上传的数据。

---

# Tech Stack

## Frontend

* Vue3
* TypeScript
* Vite
* Pinia
* Vue Router
* Element Plus
* ECharts

## Backend

* Spring Boot 3
* Java 17
* Maven

## Database

* PostgreSQL

## Cache

* Redis

## Communication

* MQTT
* HTTP REST API

## Documentation

* Swagger/OpenAPI

## Deployment

* Docker
* Docker Compose

---

# Frontend Language

所有前端界面使用简体中文。

禁止默认生成英文后台。

菜单：

* 首页仪表盘
* 客户管理
* 项目管理
* 盒子管理
* 设备管理
* 实时监控
* 历史数据
* 告警中心
* AI接口管理
* 用户管理
* 系统设置

日期格式：

yyyy-MM-dd HH:mm:ss

---

# UI Design

采用工业科技风。

要求：

* 深色主题
* 蓝色科技风
* 苹果工业控制风格
* 支持大屏展示
* 支持响应式布局

首页展示：

左侧：

* 在线盒子数量
* 离线盒子数量
* 在线设备数量
* 告警数量

中间：

* 实时设备状态
* 实时转速
* 实时温度
* 实时振动
* 实时功率

右侧：

* 最新告警
* 在线设备列表

底部：

* 温度趋势图
* 功率趋势图
* 振动趋势图

---

# Database Design

必须包含以下表：

users

roles

customers

projects

gateways

devices

device_telemetry

alarms

ai_api_keys

ai_api_call_logs

system_logs

---

# Customer

客户信息：

* customer_id
* customer_name
* contact_person
* contact_phone
* remark

---

# Project

项目信息：

* project_id
* customer_id
* project_name
* location
* remark

---

# Gateway

盒子管理：

* gateway_id
* gateway_sn
* gateway_name
* gateway_model
* imei
* sim_card_no
* customer_id
* project_id
* activation_status
* mqtt_username
* mqtt_password
* api_key
* online_status
* firmware_version
* last_seen_at
* created_at

---

# Device

设备管理：

* device_id
* gateway_id
* customer_id
* project_id
* device_name
* device_model
* install_location
* status
* created_at

---

# Telemetry

设备数据：

* device_id
* timestamp
* rpm
* current
* voltage
* power
* frequency
* pressure
* airflow
* motor_temperature
* bearing_temperature
* vibration
* status
* alarm_code

---

# Gateway Activation

平台需要支持：

* 新增盒子
* 激活盒子
* 停用盒子
* 查看在线状态
* 查看最后通信时间

激活后自动生成：

* MQTT用户名
* MQTT密码
* API Key

用于盒子接入私有云。

---

# Data Upload

支持 MQTT 与 HTTP。

MQTT Topic：

iot/gateway/{gatewayId}/telemetry

iot/gateway/{gatewayId}/alarm

iot/gateway/{gatewayId}/status

HTTP API：

POST /api/iot/telemetry

POST /api/iot/alarm

POST /api/iot/status

---

# Telemetry Example

{
"gatewayId": "BOX001",
"deviceId": "FAN001",
"timestamp": "2026-06-03T10:00:00Z",
"data": {
"rpm": 1450,
"current": 12.6,
"voltage": 380,
"power": 5.8,
"frequency": 50,
"pressure": 520,
"airflow": 3200,
"motorTemperature": 62,
"bearingTemperature": 58,
"vibration": 1.8,
"status": "running",
"alarmCode": null
}
}

---

# Alarm System

支持：

* 温度超限
* 振动超限
* 电流超限
* 设备离线
* 盒子离线
* 通讯中断

等级：

* normal
* warning
* critical

---

# AI Open API

系统必须提供标准化 AI 数据接口。

用于第三方 AI 系统、大模型平台调用。

本系统不负责：

* AI训练
* 模型推理
* AI结果分析

接口：

GET /api/ai/realtime/{deviceId}

GET /api/ai/history

POST /api/ai/export

支持：

* JSON
* CSV

要求：

* API Key认证
* 调用日志记录
* 接口限流
* 不允许直接访问数据库

---

# Security

必须支持：

* JWT认证
* RBAC权限管理
* API Key认证

禁止：

* 硬编码密码
* 硬编码数据库配置
* 明文密钥

必须：

* application.yml
* .env.example

---

# Coding Rules

必须采用：

Controller

Service

Repository

DTO

VO

统一异常处理

统一返回对象

分页查询

Swagger注释

日志记录

---

# Build Rules

每次开发完成后必须：

1. 编译后端
2. 编译前端
3. 修复编译错误
4. 更新 README
5. 输出修改文件列表

不要只输出示例代码。

优先生成可运行代码。

---

# Development Strategy

Phase 1

数据库设计

Docker Compose

项目目录结构

README

Phase 2

Spring Boot基础框架

JWT

Swagger

PostgreSQL

Redis

Phase 3

Vue3基础框架

登录页

首页仪表盘

菜单框架

Phase 4

客户管理

项目管理

盒子管理

设备管理

Phase 5

MQTT接入

HTTP接入

数据入库

Phase 6

实时监控

历史数据

告警系统

Phase 7

AI开放接口

导出功能

API Key管理

Phase 8

系统优化

权限管理

日志管理

Docker部署
# Location Module

平台必须支持盒子和设备定位信息。

定位信息用于：

- 首页地图展示
- 客户项目设备分布
- 设备安装位置管理
- AI接口返回设备位置上下文

字段：

- latitude
- longitude
- address
- province
- city
- district
- last_location_time

盒子和设备都可以维护定位信息。

前端需要支持：

- 盒子定位
- 设备安装位置
- 首页设备分布地图
- 告警设备地图定位

AI开放接口返回设备数据时，应包含设备位置相关字段。