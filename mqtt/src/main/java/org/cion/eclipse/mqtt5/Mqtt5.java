package org.cion.eclipse.mqtt5;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import org.cion.eclipse.mqtt5.client.IMqttDeliveryToken;
import org.cion.eclipse.mqtt5.client.IMqttToken;
import org.cion.eclipse.mqtt5.client.MqttActionListener;
import org.cion.eclipse.mqtt5.client.MqttCallback;
import org.cion.eclipse.mqtt5.client.MqttConnectionOptions;
import org.cion.eclipse.mqtt5.common.MqttException;
import org.cion.eclipse.mqtt5.common.MqttMessage;
import org.cion.eclipse.mqtt5.extend.MqttMessageQueue;
import org.cion.eclipse.mqtt5.service.MqttAndroidClient;
import org.cion.eclipse.mqtt5.service.MqttTraceHandler;
import org.cion.eclipse.mqtt5.utils.DeviceUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: kern
 * @date: 2021/11/25
 * @Description: MQTT
 */
public class Mqtt5 implements MqttActionListener {

    private static final String TAG = Mqtt5.class.getName();
    private static volatile Mqtt5 mqtt5;
    private MqttAndroidClient mqttAndroidClient;

    private Application application;
    private String serverURI;
    private String clientId;
    private String publishTopic;
    private String subscribeTopic;
    private boolean automaticReconnection;
    private MqttConnectionOptions connectionOptions;
    private MqttActionListener actionListener;
    private MqttCallback callback;
    private MqttTraceHandler traceHandler;
    private ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());
    private MqttMessageQueue messageQueue = new MqttMessageQueue();

    public static Mqtt5 with(Application application) {
        synchronized (Mqtt5.class) {
            if (mqtt5 == null) {
                mqtt5 = new Mqtt5(application);
            }
        }
        return mqtt5;
    }

    public Mqtt5(Application application) {
        this.application = application;
    }

    public Mqtt5 setServerURI(String serverURI) {
        this.serverURI = serverURI;
        return this;
    }

    public Mqtt5 setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public Mqtt5 setPublishTopic(String publishTopic) {
        this.publishTopic = publishTopic;
        return this;
    }

    public Mqtt5 setSubscribeTopic(String subscribeTopic) {
        this.subscribeTopic = subscribeTopic;
        return this;
    }

    public Mqtt5 setAutomaticReconnection(boolean automaticReconnection) {
        this.automaticReconnection = automaticReconnection;
        return this;
    }

    public Mqtt5 setConnectionOptions(MqttConnectionOptions connectionOptions) {
        this.connectionOptions = connectionOptions;
        return this;
    }

    public Mqtt5 setCallback(MqttCallback callback) {
        this.callback = callback;
        return this;
    }

    public Mqtt5 setActionListener(MqttActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public Mqtt5 setTraceHandler(MqttTraceHandler traceHandler) {
        this.traceHandler = traceHandler;
        return this;
    }

    public MqttAndroidClient getMqttAndroidClient() {
        if (mqttAndroidClient == null) {
            throw new NullPointerException("MQTT5 error: MqttAndroidClient is null, Mqtt5 must be initialized.");
        }
        return mqttAndroidClient;
    }

    /**
     * *****
     * initialize MQTT
     ********/
    public Mqtt5 initialize() {

        if (application == null) {
            throw new NullPointerException("MQTT5 error: application can't be null");
        }
        if (TextUtils.isEmpty(serverURI)) {
            throw new NullPointerException("MQTT5 error: server url can't be null");
        }
        if (connectionOptions == null) {
            throw new NullPointerException("MQTT5 error: MqttConnectionOptions can't be null");
        }

        if (clientId == null) {
            clientId = DeviceUtils.getUniqueId(application);
        }

        if (mqttAndroidClient == null) {
            mqttAndroidClient = new MqttAndroidClient(application, serverURI, clientId);
            mqttAndroidClient.setCallback(callback);
            mqttAndroidClient.setTraceCallback(traceHandler);
        }

        return this;
    }

    /**
     * MQTT connect if or not
     *
     * @return true is connecting
     */
    public boolean isConnect() {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    /**
     * connect MQTT
     */
    public void connect() {

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mqttAndroidClient != null && !mqttAndroidClient.isConnected()) {
                    try {
                        mqttAndroidClient.connect(connectionOptions, null, Mqtt5.this);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    /**
     * subscribe topic
     *
     * @param subTopic
     * @param listener
     */
    public void subscribeTopic(String subTopic, MqttActionListener listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mqttAndroidClient != null) {
                        mqttAndroidClient.subscribe(subTopic, 0, null, listener);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * publish message
     *
     * @param message an object of {@link MqttMessage}
     */
    public void publishMessage(MqttMessage message) {
        if (mqttAndroidClient == null) {
            throw new SecurityException("you need init mqtt before publish message");
        }
        if (mqttAndroidClient.isConnected()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        IMqttDeliveryToken token = mqttAndroidClient.publish(publishTopic, message);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                mqttAndroidClient.disconnect();
                mqttAndroidClient.unsubscribe(subscribeTopic);
                mqttAndroidClient.unregisterResources();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                messageQueue.push(message);
                initialize();
                connect();
            }
        }
    }

    /**
     * publish message
     *
     * @param payload a string
     */
    public void publishMessage(String payload) {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        message.setQos(0);
        publishMessage(message);
    }

    /**
     * recycle the MQTT
     */
    public void recycle() {
        try {
            mqttAndroidClient.disconnect();
            mqttAndroidClient.unsubscribe(subscribeTopic);
            mqttAndroidClient.unregisterResources();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            messageQueue = null;
            mqttAndroidClient = null;
            application = null;
            mqtt5 = null;
        }
    }

    /**********************************************************************************************/
    /**
     * MQTT Action Listener
     * 1、 when connect successful,then subscribe；
     * 2、 when connect failure,then try connect again；
     */

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        Log.e(TAG, "MQTT onSuccess");
        /**
         * subscribe
         * */
        subscribeTopic(publishTopic, actionListener);

        /**
         * if it has message in message queue, publish all them when MQTT connect success;
         * */
        if (messageQueue.getCount() != 0) {
            for (int i = 0; i < messageQueue.getCount(); i++) {
                MqttMessage message = messageQueue.poll();
                publishMessage(message);
            }
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

        if (actionListener != null) {
            actionListener.onFailure(asyncActionToken, exception);
        }
        /**
         * try connect again
         * */
        if (automaticReconnection) {
            connect();
        }
    }

}
