package org.cion.eclipse.mqtt5.extend;

import org.cion.eclipse.mqtt5.common.MqttMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author: kern
 * @date: 2021/12/2
 * @Description: Mqtt message queue used to manager unsent message
 */
public class MqttMessageQueue {

    private Queue<MqttMessage> queue = new ConcurrentLinkedQueue<>();

    /**
     * @param message
     */
    public void push(MqttMessage message) {
        this.queue.offer(message);
    }

    /**
     * @return
     */
    public MqttMessage poll() {
        return this.queue.poll();
    }

    /**
     * @return
     */
    public int getCount() {
        return this.queue.size();
    }

}
