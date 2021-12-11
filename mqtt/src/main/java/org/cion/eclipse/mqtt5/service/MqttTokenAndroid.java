/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.cion.eclipse.mqtt5.service;

import org.cion.eclipse.mqtt5.client.IMqttAsyncClient;
import org.cion.eclipse.mqtt5.client.IMqttToken;
import org.cion.eclipse.mqtt5.client.MqttActionListener;
import org.cion.eclipse.mqtt5.common.MqttException;
import org.cion.eclipse.mqtt5.common.MqttMessage;
import org.cion.eclipse.mqtt5.common.MqttSecurityException;
import org.cion.eclipse.mqtt5.common.packet.MqttProperties;
import org.cion.eclipse.mqtt5.common.packet.MqttWireMessage;

/**
 * <p>
 * Implementation of the IMqttToken interface for use from within the
 * MqttAndroidClient implementation
 */

class MqttTokenAndroid implements IMqttToken {

    private MqttActionListener listener;

    private volatile boolean isComplete;

    private volatile MqttException lastException;

    private Object waitObject = new Object();

    private MqttAndroidClient client;

    private Object userContext;

    private String[] topics;

    private IMqttToken delegate; // specifically for getMessageId

    private MqttException pendingException;

    /**
     * Standard constructor
     *
     * @param client      used to pass MqttAndroidClient object
     * @param userContext used to pass context
     * @param listener    optional listener that will be notified when the action completes. Use null if not required.
     */
    MqttTokenAndroid(MqttAndroidClient client,
                     Object userContext, MqttActionListener listener) {
        this(client, userContext, listener, null);
    }

    /**
     * Constructor for use with subscribe operations
     *
     * @param client      used to pass MqttAndroidClient object
     * @param userContext used to pass context
     * @param listener    optional listener that will be notified when the action completes. Use null if not required.
     * @param topics      topics to subscribe to, which can include wildcards.
     */
    MqttTokenAndroid(MqttAndroidClient client,
                     Object userContext, MqttActionListener listener, String[] topics) {
        this.client = client;
        this.userContext = userContext;
        this.listener = listener;
        this.topics = topics;
    }

    /**
     * @see
     */
    @Override
    public void waitForCompletion() throws MqttException, MqttSecurityException {
        synchronized (waitObject) {
            try {
                waitObject.wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        if (pendingException != null) {
            throw pendingException;
        }
    }

    /**
     * @see
     */
    @Override
    public void waitForCompletion(long timeout) throws MqttException,
            MqttSecurityException {
        synchronized (waitObject) {
            try {
                waitObject.wait(timeout);
            } catch (InterruptedException e) {
                // do nothing
            }
            if (!isComplete) {
                throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
            }
            if (pendingException != null) {
                throw pendingException;
            }
        }
    }

    /**
     * notify successful completion of the operation
     */
    void notifyComplete() {
        synchronized (waitObject) {
            isComplete = true;
            waitObject.notifyAll();
            if (listener != null) {
                listener.onSuccess(this);
            }
        }
    }

    /**
     * notify unsuccessful completion of the operation
     */
    void notifyFailure(Throwable exception) {
        synchronized (waitObject) {
            isComplete = true;
            if (exception instanceof MqttException) {
                pendingException = (MqttException) exception;
            } else {
                pendingException = new MqttException(exception);
            }
            waitObject.notifyAll();
            if (exception instanceof MqttException) {
                lastException = (MqttException) exception;
            }
            if (listener != null) {
                listener.onFailure(this, exception);
            }
        }

    }

    /**
     * @see
     */
    @Override
    public boolean isComplete() {
        return isComplete;
    }

    void setComplete(boolean complete) {
        isComplete = complete;
    }

    /**
     * @see
     */
    @Override
    public MqttException getException() {
        return lastException;
    }

    void setException(MqttException exception) {
        lastException = exception;
    }

    /**
     * @see
     */
    @Override
    public IMqttAsyncClient getClient() {
        return client;
    }

    /**
     * @see
     */
    @Override
    public void setActionCallback(MqttActionListener listener) {
        this.listener = listener;
    }

    /**
     * @see
     */
    @Override
    public MqttActionListener getActionCallback() {
        return listener;
    }

    /**
     * @see
     */
    @Override
    public String[] getTopics() {
        return topics;
    }

    /**
     * @see
     */
    @Override
    public void setUserContext(Object userContext) {
        this.userContext = userContext;

    }

    /**
     * @see
     */
    @Override
    public Object getUserContext() {
        return userContext;
    }

    void setDelegate(IMqttToken delegate) {
        this.delegate = delegate;
    }

    /**
     * @see
     */
    @Override
    public int getMessageId() {
        return (delegate != null) ? delegate.getMessageId() : 0;
    }

    @Override
    public MqttWireMessage getResponse() {
        return delegate.getResponse();
    }

    /***********************************************************************************************/
    @Override
    public MqttProperties getResponseProperties() {
        return delegate.getResponseProperties();
    }

    @Override
    public MqttMessage getMessage() throws MqttException {
        return delegate.getMessage();
    }

    @Override
    public MqttWireMessage getRequestMessage() {
        return delegate.getRequestMessage();
    }

    @Override
    public MqttProperties getRequestProperties() {
        return delegate.getRequestProperties();
    }

    @Override
    public boolean getSessionPresent() {
        return delegate.getSessionPresent();
    }

    @Override
    public int[] getGrantedQos() {
        return delegate.getGrantedQos();
    }

    @Override
    public int[] getReasonCodes() {
        return delegate.getReasonCodes();
    }

}
