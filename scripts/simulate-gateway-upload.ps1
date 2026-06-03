param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "please_change_admin_password",
    [string]$MqttContainer = "fan-iot-mqtt"
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Token,
        $Body
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $headers
        TimeoutSec = 20
        UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = $Body | ConvertTo-Json -Depth 20
    }

    $response = Invoke-WebRequest @params
    return $response.Content | ConvertFrom-Json
}

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$customerId = "CUST-SIM-$stamp"
$projectId = "PROJ-SIM-$stamp"
$gatewayId = "GW-SIM-$stamp"
$gatewaySn = "SN-SIM-$stamp"
$deviceId = "FAN-SIM-$stamp"
$mqttUsername = "gw_user_$stamp"
$mqttPassword = "gw_pwd_$stamp"

Write-Host "1. Login and get JWT..."
$login = Invoke-Json POST "$BaseUrl/api/auth/login" $null @{
    username = $AdminUsername
    password = $AdminPassword
}
$token = $login.data.token

Write-Host "2. Create customer, project, gateway and device..."
Invoke-Json POST "$BaseUrl/api/customers" $token @{
    customerId = $customerId
    customerName = "Sim Customer $stamp"
    contactPerson = "Tester"
    contactPhone = "13800000000"
} | Out-Null

Invoke-Json POST "$BaseUrl/api/projects" $token @{
    projectId = $projectId
    customerId = $customerId
    projectName = "Sim Project $stamp"
    location = "Test Workshop"
} | Out-Null

Invoke-Json POST "$BaseUrl/api/gateways" $token @{
    gatewayId = $gatewayId
    gatewaySn = $gatewaySn
    gatewayName = "Sim Gateway $stamp"
    gatewayModel = "BOX-MQTT-HTTP"
    customerId = $customerId
    projectId = $projectId
    activationStatus = "inactive"
    onlineStatus = "offline"
} | Out-Null

Invoke-Json POST "$BaseUrl/api/devices" $token @{
    deviceId = $deviceId
    gatewayId = $gatewayId
    customerId = $customerId
    projectId = $projectId
    deviceName = "Sim Fan $stamp"
    deviceModel = "FAN-SIM"
    installLocation = "Test Position"
    status = "offline"
} | Out-Null

Write-Host "3. Activate gateway..."
$activation = Invoke-Json POST "$BaseUrl/api/iot/gateway/activate" $null @{
    gateway_id = $gatewayId
    gateway_sn = $gatewaySn
    mqtt_username = $mqttUsername
    mqtt_password = $mqttPassword
}

Write-Host "   Activated. Telemetry topic: $($activation.data.telemetry_topic)"

$httpPayload = @{
    gateway_id = $gatewayId
    gateway_sn = $gatewaySn
    mqtt_username = $mqttUsername
    mqtt_password = $mqttPassword
    data = @(
        @{
            device_id = $deviceId
            timestamp = (Get-Date).ToString("o")
            rpm = 1480
            current = 12.4
            voltage = 380.5
            power = 18.6
            frequency = 50
            pressure = 2.3
            airflow = 6100
            motor_temperature = 68.4
            bearing_temperature = 72.1
            vibration = 2.18
            status = "running"
            alarm_code = $null
        }
    )
}

Write-Host "4. Upload telemetry by HTTP..."
$httpResult = Invoke-Json POST "$BaseUrl/api/iot/telemetry" $null $httpPayload
Write-Host "   HTTP saved count: $($httpResult.data.saved_count)"

$mqttPayload = @{
    gateway_id = $gatewayId
    gateway_sn = $gatewaySn
    mqtt_username = $mqttUsername
    mqtt_password = $mqttPassword
    data = @(
        @{
            device_id = $deviceId
            timestamp = (Get-Date).ToString("o")
            rpm = 1492
            current = 12.8
            voltage = 381.2
            power = 19.1
            frequency = 50
            pressure = 2.5
            airflow = 6250
            motor_temperature = 69.2
            bearing_temperature = 73.0
            vibration = 2.36
            status = "running"
            alarm_code = $null
        }
    )
} | ConvertTo-Json -Depth 20 -Compress

Write-Host "5. Publish telemetry by MQTT..."
$mqttPayload | docker exec -i $MqttContainer mosquitto_pub -h localhost -p 1883 -t "iot/gateway/$gatewayId/telemetry" -l
Write-Host "   MQTT message published"

Write-Host ""
Write-Host "Simulation complete:"
Write-Host "  customer_id    = $customerId"
Write-Host "  project_id     = $projectId"
Write-Host "  gateway_id     = $gatewayId"
Write-Host "  gateway_sn     = $gatewaySn"
Write-Host "  mqtt_username  = $mqttUsername"
Write-Host "  mqtt_password  = $mqttPassword"
Write-Host "  device_id      = $deviceId"
Write-Host ""
Write-Host "Verify with SQL:"
Write-Host "docker exec fan-iot-postgres psql -U fan_iot -d fan_iot_platform -c `"select gateway_id, device_id, rpm, motor_temperature, vibration, status, received_at from device_telemetry where device_id = '$deviceId' order by id desc;`""
