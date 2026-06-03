param(
    [ValidateSet("Http", "Mqtt", "Both")]
    [string]$Mode = "Both",
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "please_change_admin_password",
    [string]$MqttContainer = "fan-iot-mqtt",
    [int]$IntervalSeconds = 5,
    [int]$Iterations = 0
)

$ErrorActionPreference = "Stop"

$customerId = "CUST-DEMO"
$projectId = "PROJ-DEMO"
$gatewayId = "BOX001"
$gatewaySn = "BOX001-SN"
$deviceId = "FAN001"
$mqttUsername = "box001"
$mqttPassword = "box001_password"

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
    if ([string]::IsNullOrWhiteSpace($response.Content)) {
        return $null
    }
    return $response.Content | ConvertFrom-Json
}

function Try-Step {
    param([scriptblock]$Action)
    try {
        & $Action | Out-Null
    } catch {
        Write-Host "Skip or exists: $($_.Exception.Message)"
    }
}

function New-Point {
    param([int]$Index)
    $alarmCycle = $Index % 12
    $temperature = if ($alarmCycle -eq 8) { 84.5 } else { [Math]::Round(58 + (Get-Random -Minimum 0 -Maximum 1800) / 100, 2) }
    $vibration = if ($alarmCycle -eq 10) { 5.8 } else { [Math]::Round(1.2 + (Get-Random -Minimum 0 -Maximum 260) / 100, 2) }
    $status = if ($temperature -gt 80 -or $vibration -gt 5) { "warning" } else { "running" }
    $alarmCode = if ($temperature -gt 80) { "TEMP_HIGH" } elseif ($vibration -gt 5) { "VIB_HIGH" } else { $null }
    $rpm = Get-Random -Minimum 1360 -Maximum 1560
    $current = [Math]::Round(10.5 + (Get-Random -Minimum 0 -Maximum 420) / 100, 2)
    $voltage = [Math]::Round(378 + (Get-Random -Minimum 0 -Maximum 70) / 10, 1)
    $power = [Math]::Round(($current * $voltage * 1.732 * 0.82) / 1000, 2)
    return @{
        device_id = $deviceId
        timestamp = (Get-Date).ToString("o")
        rpm = $rpm
        current = $current
        voltage = $voltage
        power = $power
        frequency = 50
        pressure = [Math]::Round(2.0 + (Get-Random -Minimum 0 -Maximum 90) / 100, 2)
        airflow = Get-Random -Minimum 5800 -Maximum 6600
        temperature = $temperature
        motor_temperature = $temperature
        bearing_temperature = [Math]::Round($temperature - 3 + (Get-Random -Minimum 0 -Maximum 120) / 100, 2)
        vibration = $vibration
        status = $status
        alarm_code = $alarmCode
    }
}

function New-Payload {
    param([int]$Index)
    return @{
        gateway_id = $gatewayId
        gateway_sn = $gatewaySn
        mqtt_username = $mqttUsername
        mqtt_password = $mqttPassword
        data = @((New-Point -Index $Index))
    }
}

Write-Host "Login and prepare BOX001/FAN001 demo records..."
$login = Invoke-Json POST "$BaseUrl/api/auth/login" $null @{
    username = $AdminUsername
    password = $AdminPassword
}
$token = $login.data.token

Try-Step { Invoke-Json POST "$BaseUrl/api/customers" $token @{
    customerId = $customerId
    customerName = "Demo Customer"
    contactPerson = "Tester"
    contactPhone = "13800000000"
    remark = "Simulation without real IoT gateway"
} }

Try-Step { Invoke-Json POST "$BaseUrl/api/projects" $token @{
    projectId = $projectId
    customerId = $customerId
    projectName = "Demo Project"
    location = "Demo Workshop"
    remark = "Simulation without real IoT gateway"
} }

Try-Step { Invoke-Json POST "$BaseUrl/api/gateways" $token @{
    gatewayId = $gatewayId
    gatewaySn = $gatewaySn
    gatewayName = "Demo Gateway BOX001"
    gatewayModel = "SIM-BOX"
    customerId = $customerId
    projectId = $projectId
    activationStatus = "inactive"
    onlineStatus = "offline"
    longitude = 121.4737010
    latitude = 31.2304160
    province = "Shanghai"
    city = "Shanghai"
    district = "Huangpu"
    address = "Demo Address"
} }

Try-Step { Invoke-Json POST "$BaseUrl/api/devices" $token @{
    deviceId = $deviceId
    gatewayId = $gatewayId
    customerId = $customerId
    projectId = $projectId
    deviceName = "Demo Fan FAN001"
    deviceModel = "SIM-FAN"
    installLocation = "Demo Fan Room"
    longitude = 121.4737010
    latitude = 31.2304160
    address = "Demo Address"
    status = "offline"
} }

Invoke-Json POST "$BaseUrl/api/iot/gateway/activate" $null @{
    gateway_id = $gatewayId
    gateway_sn = $gatewaySn
    mqtt_username = $mqttUsername
    mqtt_password = $mqttPassword
} | Out-Null

Write-Host "Ready: gatewayId=$gatewayId deviceId=$deviceId topic=iot/gateway/$gatewayId/telemetry"
Write-Host "Mode=$Mode IntervalSeconds=$IntervalSeconds Iterations=$Iterations"
Write-Host "Open /monitor for realtime data and /history for FAN001 curves. Press Ctrl+C to stop."

$index = 1
while ($true) {
    if ($Iterations -gt 0 -and $index -gt $Iterations) {
        break
    }
    $payload = New-Payload -Index $index
    $point = $payload.data[0]
    if ($Mode -eq "Http" -or $Mode -eq "Both") {
        $result = Invoke-Json POST "$BaseUrl/api/iot/telemetry" $null $payload
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] HTTP ok rpm=$($point.rpm) temp=$($point.motor_temperature) vib=$($point.vibration) status=$($point.status) saved=$($result.data.saved_count)"
    }
    if ($Mode -eq "Mqtt" -or $Mode -eq "Both") {
        $json = $payload | ConvertTo-Json -Depth 20 -Compress
        $json | docker exec -i $MqttContainer mosquitto_pub -h localhost -p 1883 -t "iot/gateway/$gatewayId/telemetry" -l
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] MQTT ok topic=iot/gateway/$gatewayId/telemetry rpm=$($point.rpm) temp=$($point.motor_temperature) vib=$($point.vibration) status=$($point.status)"
    }
    $index++
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "Simulation finished."
