package com.jonas.rmq.support.builder;

import com.jonas.rmq.support.converter.MessageBodyConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;

/**
 * 组装消息
 */
public class MessageBuilder {

    private final String topic;

    private String tag;

    private String key;

    private Object body;

    private byte[] payload;

    private MessageBuilder(String topic) {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("blank topic");
        }
        this.topic = topic;
    }

    public static final MessageBuilder withTopic(String topic) {
        return new MessageBuilder(topic);
    }


    public final MessageBuilder withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public final MessageBuilder withKey(String key) {
        this.key = key;
        return this;
    }

    public final MessageBuilder withKeys(String... keys) {
        StringBuffer sb = new StringBuffer();
        for (String k : keys) {
            sb.append(k);
            sb.append(MessageConst.KEY_SEPARATOR);
        }
        this.key = sb.toString().trim();
        return this;
    }

    public final MessageBuilder withBody(Object obj) {
        if (this.body != null) {
            throw new IllegalArgumentException("Payload is exists.");
        }
        this.body = obj;
        return this;
    }

    public final MessageBuilder withPayload(byte[] payload) {
        if (this.payload != null) {
            throw new IllegalArgumentException("Message body is exists.");
        }
        this.payload = payload;
        return this;
    }

    public Message build() {
        return this.build(null);
    }

    @SuppressWarnings("unchecked")
    public <T> Message build(MessageBodyConverter<T> converter) {
        if (StringUtils.isBlank(this.topic)) {
            throw new IllegalArgumentException("Blank topic");
        }
        if (this.body == null && this.payload == null) {
            throw new IllegalArgumentException("Empty payload");
        }
        byte[] payload = this.payload;
        if (payload == null && converter != null) {
            try {
                payload = converter.toByteArray((T) this.body);
            } catch (MQClientException e) {
                throw new IllegalStateException("Convert message body failed.", e);
            }
        }

        if (payload == null) {
            throw new IllegalArgumentException("Empty payload");
        }

        Message msg = new Message(this.topic, this.tag, this.key, payload);

        return msg;
    }

}
