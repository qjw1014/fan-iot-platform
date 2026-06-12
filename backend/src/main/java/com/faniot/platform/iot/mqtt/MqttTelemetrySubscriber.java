package com.faniot.platform.iot.mqtt;

import com.faniot.platform.d200.service.D200AdapterService;
import com.faniot.platform.iot.config.MqttProperties;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component
public class MqttTelemetrySubscriber implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetrySubscriber.class);

    private final MqttProperties properties;
    private final D200AdapterService d200AdapterService;
    private volatile boolean running;
    private MqttClient client;

    public MqttTelemetrySubscriber(MqttProperties properties, D200AdapterService d200AdapterService) {
        this.properties = properties;
        this.d200AdapterService = d200AdapterService;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            log.info("MQTT接入未启用");
            return;
        }
        try {
            client = new MqttClient(properties.getBrokerUrl(), properties.getClientId(), new MemoryPersistence());
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    subscribe();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接断开：{}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // This service only subscribes.
                }
            });
            client.connect(connectOptions());
            running = true;
            log.info("MQTT客户端已连接：{}", properties.getBrokerUrl());
        } catch (MqttException ex) {
            running = false;
            log.error("MQTT客户端启动失败", ex);
        }
    }

    @Override
    public void stop() {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException ex) {
                log.warn("MQTT客户端关闭失败：{}", ex.getMessage());
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        if (StringUtils.hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }
        return options;
    }

    private void subscribe() {
        try {
            client.subscribe(properties.getTelemetryTopic(), 1);
            log.info("MQTT已订阅D200上行主题：{}", properties.getTelemetryTopic());
        } catch (MqttException ex) {
            log.error("MQTT订阅失败", ex);
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            d200AdapterService.ingestMqtt(topic, message.getQos(), payload);
            log.debug("D200 MQTT消息处理完成 topic={}", topic);
        } catch (Exception ex) {
            log.error("D200 MQTT消息处理失败 topic={}", topic, ex);
        }
    }
}
