package org.cion.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.cion.eclipse.mqtt5.Mqtt5;
import org.cion.eclipse.mqtt5.client.IMqttToken;
import org.cion.eclipse.mqtt5.client.MqttActionListener;
import org.cion.eclipse.mqtt5.client.MqttCallback;
import org.cion.eclipse.mqtt5.client.MqttConnectionOptionsBuilder;
import org.cion.eclipse.mqtt5.client.MqttDisconnectResponse;
import org.cion.eclipse.mqtt5.common.MqttException;
import org.cion.eclipse.mqtt5.common.MqttMessage;
import org.cion.eclipse.mqtt5.common.packet.MqttProperties;
import org.cion.eclipse.mqtt5.service.MqttTraceHandler;

/**
 * @author: kern
 * @date: 2021/11/24
 * @Description: java类作用描述
 */
public class MyMqttService extends Service {

    public final String TAG = MyMqttService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        initMqtt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Mqtt5.with(getApplication()).recycle();
    }

    /**
     * config and init MQTT
     */
    private void initMqtt() {

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
//                        .automaticReconnect(true)
//                        .automaticReconnectDelay(10, 10)
//                        .will(PUBLISH_TOPIC, new MqttMessage(message.getBytes(), qos.intValue(), retained.booleanValue(), new MqttProperties()))
                        .build())
                .setActionListener(mqttActionListener)
                .setTraceHandler(traceHandler)
                .setCallback(mqttCallback)
                .initialize()
                .connect();

    }

    MqttActionListener mqttActionListener = new MqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.e("sos", "onSuccess>>>" + asyncActionToken.getMessageId());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e("sos", "onFailure>>>" + asyncActionToken.getMessageId() + ";;;;" + exception.getMessage());
        }

    };

    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.e("sos", "connectionLost>>>" + cause.getMessage());
        }

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            Log.e("sos", "disconnected>>>" + disconnectResponse.getReasonString());
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            Log.e("sos", "mqttErrorOccurred>>>" + exception.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i("sos", "topic: " + topic + ", msg: " + new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            Log.e("sos", "deliveryComplete>>>" + token.getMessageId());
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.e("sos", "connectComplete>>>" + serverURI);
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            Log.e("sos", "authPacketArrived>>>" + reasonCode + ";;;;" + properties.getReasonString());
        }
    };

    MqttTraceHandler traceHandler = new MqttTraceHandler() {
        @Override
        public void traceDebug(String tag, String message) {
            Log.e("sos", "traceDebug>>>" + tag + ";;;;" + message);
        }

        @Override
        public void traceError(String tag, String message) {
            Log.e("sos", "traceError>>>" + tag + ";;;;" + message);
        }

        @Override
        public void traceException(String tag, String message, Exception e) {
            Log.e("sos", "traceException>>>" + tag + ";;;;" + message);
        }
    };

}
