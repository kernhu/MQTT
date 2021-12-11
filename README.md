# MQTT

MQTT5 for Android which develops base
on [eclipse/paho.mqtt v5](https://github.com/eclipse/paho.mqtt.java)
and [eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android) ,It fits android
O/P/Q/R and solves any ANR.  
MQTT5 基于eclipse/paho.mqtt.android开发，MQTT升级为mqtt-v5版本,适配android 高版本系统，并且解决一些ANR问题；适用于物联网、iot设备；

## How to use it?

1. add the permissions and register the service in the manifest

```html

<!-- permission for MQTT -->
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<!-- permission for MQTT -->

<application>
    
    <!-- service for MQTT -->
    <service android:name="org.cion.eclipse.mqtt5.service.MqttService" />

</application>

```

2. config and init mqtt.

```java  
  
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
         
```

3. publish message.

```java 
      Mqtt5.with(getApplication()).publishMessage("your message);   
```

3. recycle mqtt when activity destroy.

```java 
     Mqtt5.with(getApplication()).recycle();
```

4. listener and callback intro.

```

    /**
     * listen mqtt connect status
     */
    MqttActionListener mqttActionListener = new MqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.i("sos", "onSuccess>>>" + asyncActionToken.getMessageId());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.i("sos", "onFailure>>>" + asyncActionToken.getMessageId());
        }

    };

    /**
     * listen mqtt connect status and receive mqtt message
     */
    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            binding.status.setText("Status: " + "connectionLost");
            Log.i("sos", "connectionLost>>>" + (cause == null));
        }

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            binding.status.setText("Status: " + "disconnected");
            Log.i("sos", "disconnected>>>" + disconnectResponse.getReasonString());
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            Log.i("sos", "mqttErrorOccurred>>>" + exception.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            Log.i("sos", "messageArrived---topic: " + topic + ", msg: " + new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            Log.i("sos", "deliveryComplete>>>" + token.getMessageId());
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.i("sos", "connectComplete>>>" + serverURI);
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            Log.i("sos", "authPacketArrived>>>" + reasonCode + ";;;;" + properties.getReasonString());
        }
    };

    /**
     * mqtt error trace
     */
    MqttTraceHandler traceHandler = new MqttTraceHandler() {
        @Override
        public void traceDebug(String tag, String message) {
            Log.i("sos", "traceDebug>>>" + tag + ";;;;" + message);
        }

        @Override
        public void traceError(String tag, String message) {
            Log.i("sos", "traceError>>>" + tag + ";;;;" + message);
        }

        @Override
        public void traceException(String tag, String message, Exception e) {
            Log.i("sos", "traceException>>>" + tag + ";;;;" + message);
        }
    };

```

## Contribute

Welcome to submit and solve bugs for it.
Email: vsky580@gmail.com  
Facebook: https://www.facebook.com/kern.hu.580   
QQ群：43447852  

## Thanks

[eclipse/paho/mqtt](https://github.com/eclipse/paho.mqtt.java)   
[eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android)  