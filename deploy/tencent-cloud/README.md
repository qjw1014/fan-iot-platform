# 腾讯云部署

适用于 Ubuntu 22.04 LTS 轻量应用服务器。

对公网开放：

- `80/tcp`：平台网页
- `1883/tcp`：D200 MQTT
- `22/tcp`：SSH，建议只允许管理员 IP

腾讯云轻量应用服务器的防火墙需要手动添加 `TCP:1883` 入站规则，
否则 D200 无法通过公网连接 MQTT Broker。

PostgreSQL、Redis 和 Spring Boot 仅在 Docker 内部网络访问。

部署目录默认为 `/opt/fan-iot-platform`：

```bash
cd /opt/fan-iot-platform
docker compose --env-file deploy/tencent-cloud/.env \
  -f deploy/tencent-cloud/docker-compose.yml up -d --build
```

查看状态：

```bash
docker compose --env-file deploy/tencent-cloud/.env \
  -f deploy/tencent-cloud/docker-compose.yml ps
```

查看后端日志：

```bash
docker compose --env-file deploy/tencent-cloud/.env \
  -f deploy/tencent-cloud/docker-compose.yml logs -f backend
```

## 高德基站定位

在 `deploy/tencent-cloud/.env` 配置高德 Web 服务 Key：

```dotenv
LBS_ENABLED=true
LBS_PROVIDER=amap
LBS_BASE_URL=https://restapi.amap.com/v5/position/IoT
LBS_MAP_BASE_URL=https://restapi.amap.com/v3/staticmap
LBS_API_KEY=your_amap_web_service_key
LBS_DEFAULT_MCC=460
LBS_DEFAULT_MNC=3
LBS_NETWORK=GSM
LBS_SIGNAL=-99
LBS_REFRESH_INTERVAL_MINUTES=30
```

`.env` 已被 Git 忽略，不要把真实 Key 写入仓库。修改后需要重新创建后端容器：

```bash
docker compose --env-file deploy/tencent-cloud/.env \
  -f deploy/tencent-cloud/docker-compose.yml up -d --build --force-recreate backend frontend
```
