package com.yushi.support.converter;

import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class StringMessageBodyConverter implements MessageBodyConverter<String> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] toByteArray(String body) throws MQClientException {
        if (body == null) {
            return null;
        }
        try {
            return body.getBytes("utf-8");
        } catch (Exception e) {
            logger.error("rocketmq消息格式格式化错误", e);
        }
        return null;
    }

    @Override
    public String fromByteArray(byte[] bs) throws MQClientException {
        try {
            return new String(bs, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("rocketmq消息格式转换错误", e);
        }
        return null;
    }
}
