/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.cion.eclipse.mqtt5.client;

import org.cion.eclipse.mqtt5.client.internal.ClientComms;
import org.cion.eclipse.mqtt5.common.MqttException;
import org.cion.eclipse.mqtt5.common.MqttMessage;
import org.cion.eclipse.mqtt5.common.MqttPersistenceException;
import org.cion.eclipse.mqtt5.common.packet.MqttProperties;
import org.cion.eclipse.mqtt5.common.packet.MqttPublish;
import org.cion.eclipse.mqtt5.common.util.Strings;

import java.io.UnsupportedEncodingException;

/**
 * Represents a topic destination, used for publish/subscribe messaging.
 */
public class MqttTopic {

    // topic name and topic filter length range defined in the spec
    private static final int MIN_TOPIC_LEN = 1;
    private static final int MAX_TOPIC_LEN = 65535;
    private static final char NUL = '\u0000';
    /**
     * The forward slash (/) is used to separate each level within a topic tree and
     * provide a hierarchical structure to the topic space. The use of the topic
     * level separator is significant when the two wildcard characters are
     * encountered in topics specified by subscribers.
     */
    public static final String TOPIC_LEVEL_SEPARATOR = "/";

    /**
     * Multi-level wildcard The number sign (#) is a wildcard character that matches
     * any number of levels within a topic.
     */
    public static final String MULTI_LEVEL_WILDCARD = "#";

    /**
     * Single-level wildcard The plus sign (+) is a wildcard character that matches
     * only one topic level.
     */
    public static final String SINGLE_LEVEL_WILDCARD = "+";
    /**
     * Multi-level wildcard pattern(/#)
     */
    public static final String MULTI_LEVEL_WILDCARD_PATTERN = TOPIC_LEVEL_SEPARATOR + MULTI_LEVEL_WILDCARD;
    /**
     * Topic wildcards (#+)
     */
    public static final String TOPIC_WILDCARDS = MULTI_LEVEL_WILDCARD + SINGLE_LEVEL_WILDCARD;

    private ClientComms comms;
    private String name;

    /**
     * @param name  The Name of the topic
     * @param comms The {@link ClientComms}
     */
    public MqttTopic(String name, ClientComms comms) {
        this.comms = comms;
        this.name = name;
    }

    /**
     * Publishes a message on the topic. This is a convenience method, which will
     * create a new {@link MqttMessage} object with a byte array payload and the
     * specified QoS, and then publish it. All other values in the message will be
     * set to the defaults.
     *
     * @param payload  the byte array to use as the payload
     * @param qos      the Quality of Service. Valid values are 0, 1 or 2.
     * @param retained whether or not this message should be retained by the server.
     * @return {@link MqttToken}
     * @throws MqttException            If an error occurs publishing the message
     * @throws MqttPersistenceException If an error occurs persisting the message
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @see #publish(MqttMessage)
     * @see MqttMessage#setQos(int)
     * @see MqttMessage#setRetained(boolean)
     */
    public MqttToken publish(byte[] payload, int qos, boolean retained)
            throws MqttException, MqttPersistenceException {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        return this.publish(message);
    }

    /**
     * Publishes the specified message to this topic, but does not wait for delivery
     * of the message to complete. The returned {@link MqttToken token} can
     * be used to track the delivery status of the message. Once this method has
     * returned cleanly, the message has been accepted for publication by the
     * client. Message delivery will be completed in the background when a
     * connection is available.
     *
     * @param message the message to publish
     * @return an MqttToken for tracking the delivery of the message
     * @throws MqttException            if an error occurs publishing the message
     * @throws MqttPersistenceException if an error occurs persisting the message
     */
    public MqttToken publish(MqttMessage message) throws MqttException, MqttPersistenceException {
        MqttToken token = new MqttToken(comms.getClient().getClientId());
        token.internalTok.setDeliveryToken(true);
        token.setMessage(message);
        comms.sendNoWait(createPublish(message, new MqttProperties()), token);
        token.internalTok.waitUntilSent();
        return token;
    }

    /**
     * Returns the name of the queue or topic.
     *
     * @return the name of this destination.
     */
    public String getName() {
        return name;
    }

    /**
     * Create a PUBLISH packet from the specified message.
     */
    private MqttPublish createPublish(MqttMessage message, MqttProperties properties) {
        return new MqttPublish(this.getName(), message, properties);
    }

    /**
     * Returns a string representation of this topic.
     *
     * @return a string representation of this topic.
     */
    @Override
    public String toString() {
        return getName();
    }


    /**
     * Validate the topic name or topic filter
     *
     * @param topicString     topic name or filter
     * @param wildcardAllowed true if validate topic filter, false otherwise
     * @throws IllegalArgumentException if the topic is invalid
     */
    public static void validate(String topicString, boolean wildcardAllowed) throws IllegalArgumentException {
        int topicLen = 0;
        try {
            topicLen = topicString.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }

        // Spec: length check
        // - All Topic Names and Topic Filters MUST be at least one character
        // long
        // - Topic Names and Topic Filters are UTF-8 encoded strings, they MUST
        // NOT encode to more than 65535 bytes
        if (topicLen < MIN_TOPIC_LEN || topicLen > MAX_TOPIC_LEN) {
            throw new IllegalArgumentException(String.format("Invalid topic length, should be in range[%d, %d]!",
                    new Object[]{Integer.valueOf(MIN_TOPIC_LEN), Integer.valueOf(MAX_TOPIC_LEN)}));
        }

        // *******************************************************************************
        // 1) This is a topic filter string that can contain wildcard characters
        // *******************************************************************************
        if (wildcardAllowed) {
            // Only # or +
            if (Strings.equalsAny(topicString, new String[]{MULTI_LEVEL_WILDCARD, SINGLE_LEVEL_WILDCARD})) {
                return;
            }

            // 1) Check multi-level wildcard
            // Rule:
            // The multi-level wildcard can be specified only on its own or next
            // to the topic level separator character.

            // - Can only contains one multi-level wildcard character
            // - The multi-level wildcard must be the last character used within
            // the topic tree
            if (Strings.countMatches(topicString, MULTI_LEVEL_WILDCARD) > 1
                    || (topicString.contains(MULTI_LEVEL_WILDCARD)
                    && !topicString.endsWith(MULTI_LEVEL_WILDCARD_PATTERN))) {
                throw new IllegalArgumentException(
                        "Invalid usage of multi-level wildcard in topic string: " + topicString);
            }

            // 2) Check single-level wildcard
            // Rule:
            // The single-level wildcard can be used at any level in the topic
            // tree, and in conjunction with the
            // multilevel wildcard. It must be used next to the topic level
            // separator, except when it is specified on
            // its own.
            validateSingleLevelWildcard(topicString);

            return;
        }

        // *******************************************************************************
        // 2) This is a topic name string that MUST NOT contains any wildcard characters
        // *******************************************************************************
        if (Strings.containsAny(topicString, TOPIC_WILDCARDS)) {
            throw new IllegalArgumentException("The topic name MUST NOT contain any wildcard characters (#+)");
        }
    }

    private static void validateSingleLevelWildcard(String topicString) {
        char singleLevelWildcardChar = SINGLE_LEVEL_WILDCARD.charAt(0);
        char topicLevelSeparatorChar = TOPIC_LEVEL_SEPARATOR.charAt(0);

        char[] chars = topicString.toCharArray();
        int length = chars.length;
        char prev = NUL, next = NUL;
        for (int i = 0; i < length; i++) {
            prev = (i - 1 >= 0) ? chars[i - 1] : NUL;
            next = (i + 1 < length) ? chars[i + 1] : NUL;

            if (chars[i] == singleLevelWildcardChar) {
                // prev and next can be only '/' or none
                if (prev != topicLevelSeparatorChar && prev != NUL || next != topicLevelSeparatorChar && next != NUL) {
                    throw new IllegalArgumentException(
                            String.format("Invalid usage of single-level wildcard in topic string '%s'!",
                                    new Object[]{topicString}));

                }
            }
        }
    }
}
