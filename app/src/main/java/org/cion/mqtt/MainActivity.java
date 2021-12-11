package org.cion.mqtt;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

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
import org.cion.mqtt.databinding.ActivityMainBinding;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author kern
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String[] permission = {Manifest.permission.READ_PRECISE_PHONE_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_NETWORK_STATE};
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private ActivityMainBinding binding;
    private String serverURI;
    private String clientId;
    private String publishTopic;
    private String subscribeTopic;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bindEvent();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Mqtt5.with(getApplication()).recycle();
    }

    private void bindEvent() {
        binding.initialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPrams()) {
                    checkPermission();
                } else {
                    Snackbar.make(binding.getRoot(), "please, input the value", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        binding.publishBtn.setOnClickListener(this);
    }

    private boolean checkPrams() {

        serverURI = binding.serverURI.getText().toString().trim();
        clientId = binding.clientId.getText().toString().trim();
        publishTopic = binding.publishTopic.getText().toString().trim();
        subscribeTopic = binding.subscribeTopic.getText().toString().trim();
        username = binding.username.getText().toString().trim();
        password = binding.password.getText().toString().trim();

        if (TextUtils.isEmpty(serverURI)) {
            return false;
        }
        if (TextUtils.isEmpty(clientId)) {
            return false;
        }
        if (TextUtils.isEmpty(publishTopic)) {
            return false;
        }
        if (TextUtils.isEmpty(subscribeTopic)) {
            return false;
        }
        if (TextUtils.isEmpty(username)) {
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        return true;
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(this, permission, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initMqtt();
    }

    @Override
    public void onClick(View v) {
        //executor.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
        if (!TextUtils.isEmpty(binding.message.getText())) {
            Mqtt5.with(getApplication()).publishMessage(binding.message.getText().toString());
        } else {
            Snackbar.make(binding.getRoot(), "please input message", Snackbar.LENGTH_SHORT).show();
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Mqtt5.with(getApplication()).publishMessage("hello, i'm from cion MQTT >>> " + System.currentTimeMillis());
        }
    };

    /**
     *
     */
    private void initMqtt() {

        Mqtt5.with(getApplication())
                .setServerURI(binding.serverURI.getText().toString())
                .setClientId(binding.clientId.getText().toString())
                .setPublishTopic(binding.publishTopic.getText().toString())
                .setSubscribeTopic(binding.subscribeTopic.getText().toString())
                .setConnectionOptions(new MqttConnectionOptionsBuilder()
                        .username(binding.username.getText().toString())
                        .password(binding.password.getText().toString().getBytes())
                        .keepAliveInterval(15)
                        .connectionTimeout(10)
                        .sessionExpiryInterval(60 * 1000L)
                        .cleanStart(true)
                        .maximumPacketSize(20 * 1024L)
                        .automaticReconnect(true)
                        .automaticReconnectDelay(10, 10)
                        .build())
                .setActionListener(mqttActionListener)
                .setTraceHandler(traceHandler)
                .setCallback(mqttCallback)
                .initialize()
                .connect();
    }

    /**
     * listen mqtt connect action
     */
    MqttActionListener mqttActionListener = new MqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            binding.status.setText("Status: " + "connected");
            Log.i("sos", "onSuccess>>>" + asyncActionToken.getMessageId());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            binding.status.setText("Status: " + "disconnected");
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
            String count = binding.subscribeMessage.getText() + "\n" + new String(message.getPayload());
            binding.subscribeMessage.setText(count);
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

}
