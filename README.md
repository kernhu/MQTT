# MQTT
MQTT5 for Android which develops base on [eclipse/paho.mqtt v5](https://github.com/eclipse/paho.mqtt.java)  and [eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android) ,It fits android O/P/Q/R and solves any ANR.  
MQTT5 基于eclipse/paho.mqtt.android开发，MQTT升级为mqtt-v5版本,适配android 高版本系统，并且解决一些ANR问题；适用于物联网、iot设备；

## How to use it?
1. config and init it.  

#```
       Mqtt5.with(getApplication())  
                .setServerURI("your serverURI")  
                .setClientId("your clientId")  
                .setPublishTopic("your publishTopic")  
                .setSubscribeTopic("your subscribeTopic")  
                .setConnectionOptions(new MqttConnectionOptionsBuilder()  
                        .username("your userName")  
                        .password("your password".getBytes())  
                        .keepAliveInterval(15)  
                        .connectionTimeout(10)  
                        .sessionExpiryInterval(60 * 1000L)  
                        .cleanStart(true)  
                        .maximumPacketSize(20 * 1024L)  
                        .build())  
                        .setActionListener(mqttActionListener)  
                        .setTraceHandler(traceHandler)  
                        .setCallback(mqttCallback)  
                        .initialize()  
                        .connect();  
#```
   
2. publish message.  

#```
      Mqtt5.with(getApplication()).publishMessage("your message);   
#```
   
3. recycle mqtt when activity destroy.  

#```
     Mqtt5.with(getApplication()).recycle();
#```
   
## Contribute

Welcome to submit and solve bugs for it.

## Thanks
[eclipse/paho/mqtt](https://github.com/eclipse/paho.mqtt.java)   
[eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android)  