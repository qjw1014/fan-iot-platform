# 数据库 ER 图说明

本文档为 Phase 1 数据库设计说明，严格按 `AGENTS.md` 当前版本整理。

现场 Modbus TCP 由物联网盒子采集，云端不配置寄存器、不配置点位、不直连现场设备。云端只接收盒子通过 MQTT 或 HTTP REST API 上传的数据。

## ER 图

```mermaid
erDiagram
    customers ||--o{ projects : "拥有"
    customers ||--o{ gateways : "拥有"
    customers ||--o{ devices : "拥有"
    projects ||--o{ gateways : "部署"
    projects ||--o{ devices : "部署"

    roles ||--o{ user_roles : "分配"
    users ||--o{ user_roles : "拥有"
    users ||--o{ alarms : "确认告警"
    users ||--o{ ai_api_keys : "创建"
    users ||--o{ system_logs : "产生"

    gateways ||--o{ devices : "管理"
    gateways ||--o{ device_telemetry : "上传"
    devices ||--o{ device_telemetry : "产生遥测"
    devices ||--o{ alarms : "触发告警"
    devices ||--o{ system_logs : "关联日志"
    device_telemetry ||--o| alarms : "生成告警"
    ai_api_keys ||--o{ ai_api_call_logs : "调用接口"

    customers {
        bigint id PK
        varchar customer_id UK
        varchar customer_name
        varchar contact_person
        varchar contact_phone
        varchar remark
    }

    projects {
        bigint id PK
        varchar project_id UK
        varchar customer_id FK
        varchar project_name
        varchar location
        varchar remark
    }

    gateways {
        bigint id PK
        varchar gateway_id UK
        varchar gateway_sn UK
        varchar gateway_name
        varchar gateway_model
        varchar imei
        varchar sim_card_no
        varchar customer_id FK
        varchar project_id FK
        varchar activation_status
        varchar mqtt_username
        varchar mqtt_password_hash
        varchar api_key_hash
        varchar online_status
        varchar firmware_version
        timestamptz last_seen_at
    }

    devices {
        bigint id PK
        varchar device_id UK
        varchar gateway_id FK
        varchar customer_id FK
        varchar project_id FK
        varchar device_name
        varchar device_model
        varchar install_location
        varchar status
        timestamptz created_at
    }

    device_telemetry {
        bigint id PK
        varchar gateway_id FK
        varchar device_id FK
        timestamptz timestamp
        numeric rpm
        numeric current
        numeric voltage
        numeric power
        numeric frequency
        numeric pressure
        numeric airflow
        numeric motor_temperature
        numeric bearing_temperature
        numeric vibration
        varchar status
        varchar alarm_code
    }
```

## 设计重点

- `customers`、`projects` 用于客户和项目归属管理。
- `gateways` 是物联网盒子管理主体，激活后生成 MQTT 用户名、MQTT 密码哈希、API Key 哈希。
- `devices` 是网关下挂设备，不表示云端直连设备。
- `device_telemetry` 保存盒子通过 MQTT/HTTP 上传的标准风机遥测数据。
- 平台不保存 Modbus TCP 点位配置表，不执行现场采集逻辑。
