package com.yushi.rmq.support.converter;

import org.apache.rocketmq.client.exception.MQClientException;

public interface MessageBodyConverter<T> {
    /**
     * Convert a message object to byte array.
     *
     * @param body
     * @return
     * @throws MQClientException
     */
    public byte[] toByteArray(T body) throws MQClientException;


    /**
     * Convert a byte array to message object.
     *
     * @param bs
     * @return
     * @throws MQClientException
     */
    public T fromByteArray(byte[] bs) throws MQClientException;
}

