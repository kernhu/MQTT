/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.cion.eclipse.mqtt5.common.packet;

import org.cion.eclipse.mqtt5.common.MqttException;
import org.cion.eclipse.mqtt5.common.MqttMessage;
import org.cion.eclipse.mqtt5.common.MqttSubscription;
import org.cion.eclipse.mqtt5.common.packet.util.CountingInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MqttSubscribe extends MqttPersistableWireMessage {

    private String[] names;
    private int[] qos;
    private int count;

    private static final Byte[] validProperties = {MqttProperties.SUBSCRIPTION_IDENTIFIER,
            MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE,
            MqttProperties.USER_DEFINED_PAIR_IDENTIFIER};

    // Fields
    private MqttProperties properties;
    private MqttSubscription[] subscriptions;

    /**
     * Constructor for an on the Wire MQTT Subscribe message
     *
     * @param data - The variable header and payload bytes.
     * @throws IOException   - if an exception occurs when decoding an input stream
     * @throws MqttException - If an exception occurs decoding this packet
     */
    public MqttSubscribe(byte[] data) throws IOException, MqttException {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        this.properties = new MqttProperties(validProperties);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        CountingInputStream counter = new CountingInputStream(bais);
        DataInputStream inputStream = new DataInputStream(counter);
        msgId = inputStream.readUnsignedShort();

        this.properties.decodeProperties(inputStream);

        ArrayList<MqttSubscription> subscriptionList = new ArrayList<>();
        // Whilst we are reading data
        while (counter.getCounter() < data.length) {
            String topic = MqttDataTypes.decodeUTF8(inputStream);
            byte subscriptionOptions = inputStream.readByte();
            subscriptionList.add(decodeSubscription(topic, subscriptionOptions));
        }
        subscriptions = subscriptionList.toArray(new MqttSubscription[subscriptionList.size()]);
        inputStream.close();
    }

    /**
     * Constructor for an on the Wire MQTT Subscribe message
     *
     * @param subscriptions - An Array of {@link MqttSubscription} subscriptions.
     * @param properties    - The {@link MqttProperties} for the packet.
     */
    public MqttSubscribe(MqttSubscription[] subscriptions, MqttProperties properties) {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        this.subscriptions = subscriptions;
        if (properties != null) {
            this.properties = properties;
        } else {
            this.properties = new MqttProperties();
        }
        this.properties.setValidProperties(validProperties);
    }

    /**
     * Constructor for an on the Wire MQTT Subscribe message
     *
     * @param subscription - An {@link MqttSubscription}
     * @param properties   - The {@link MqttProperties} for the packet.
     */
    public MqttSubscribe(MqttSubscription subscription, MqttProperties properties) {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        this.subscriptions = new MqttSubscription[]{subscription};
        this.properties = properties;
        this.properties.setValidProperties(validProperties);
    }

    /**
     * Constructor for an on the wire MQTT subscribe message
     *
     * @param info the info byte
     * @param data the data byte array
     * @throws IOException if an exception occurs whilst reading the input stream
     */
    public MqttSubscribe(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();

        count = 0;
        names = new String[10];
        qos = new int[10];
        boolean end = false;
        while (!end) {
            try {
                names[count] = decodeUTF8(dis);
                qos[count++] = dis.readByte();
            } catch (Exception e) {
                end = true;
            }
        }
        dis.close();
    }

    /**
     * Constructor for an on the wire MQTT subscribe message
     *
     * @param names - one or more topics to subscribe to
     * @param qos   - the max QoS that each each topic will be subscribed at
     */
    public MqttSubscribe(String[] names, int[] qos) {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        if (names == null || qos == null) {
            throw new IllegalArgumentException();
        }

        this.names = names.clone();
        this.qos = qos.clone();
        if (this.names.length != this.qos.length) {
            throw new IllegalArgumentException();
        }

        this.count = names.length;

        for (int qo : qos) {
            MqttMessage.validateQos(qo);
        }
    }

    @Override
    protected byte[] getVariableHeader() throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(baos);

            // Encode the Message ID
            outputStream.writeShort(msgId);

            // Write Identifier / Value Fields
            byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
            outputStream.write(identifierValueFieldsByteArray);

            outputStream.flush();
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new MqttException(ioe);
        }
    }

    @Override
    public byte[] getPayload() throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(baos);

            if (subscriptions != null) {
                for (MqttSubscription subscription : subscriptions) {
                    outputStream.write(encodeSubscription(subscription));
                }
            }
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    encodeUTF8(outputStream, names[i]);
                    outputStream.writeByte(qos[i]);
                }
            }

            outputStream.flush();
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new MqttException(ioe);
        }
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    /**
     * Encodes an {@link MqttSubscription} into it's on-the-wire representation.
     * Assumes that the Subscription topic is valid.
     *
     * @param subscription - The {@link MqttSubscription} to encode.
     * @return A byte array containing the encoded subscription.
     * @throws MqttException
     */
    private byte[] encodeSubscription(MqttSubscription subscription) throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(baos);

            MqttDataTypes.encodeUTF8(outputStream, subscription.getTopic());

            // Encode Subscription QoS
            byte subscriptionOptions = (byte) subscription.getQos();

            // Encode NoLocal Option
            if (subscription.isNoLocal()) {
                subscriptionOptions |= 0x04;
            }

            // Encode Retain As Published Option
            if (subscription.isRetainAsPublished()) {
                subscriptionOptions |= 0x08;
            }

            // Encode Retain Handling Level
            subscriptionOptions |= (subscription.getRetainHandling() << 4);

            outputStream.write(subscriptionOptions);

            outputStream.flush();
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new MqttException(ioe);
        }
    }

    private MqttSubscription decodeSubscription(String topic, byte subscriptionOptions) {
        MqttSubscription subscription = new MqttSubscription(topic);
        subscription.setQos(subscriptionOptions & 0x03);
        subscription.setNoLocal((subscriptionOptions & 0x04) != 0);
        subscription.setRetainAsPublished((subscriptionOptions & 0x08) != 0);
        subscription.setRetainHandling((subscriptionOptions >> 4) & 0x03);
        return subscription;
    }

    @Override
    protected byte getMessageInfo() {
        return (byte) (2 | (duplicate ? 8 : 0));
    }

    public MqttSubscription[] getSubscriptions() {
        return subscriptions;
    }

    @Override
    public MqttProperties getProperties() {
        return this.properties;
    }

    /**
     * @return string representation of this subscribe packet
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append(" names:[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(names[i]).append("\"");
        }
        sb.append("] qos:[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(qos[i]);
        }
        sb.append("]");

        return sb.toString();
    }

}
