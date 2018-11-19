/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.yushi.annotation;

import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.lang.annotation.*;

/**
 * MQ监听器注解.
 * 用在消费端的方法上，用来处理同一topic中不同的tag类型的消息
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RocketMqListener {
    //消息主题
    String topic() default "";

    //it only support or operation such as "tag1 || tag2 || tag3"
    String tags() default "";

    //MessageListenerConcurrently MessageListenerOrderly
    Class<?> pushListenerType() default MessageListenerConcurrently.class;

    //not null
    String group() default "";

    ConsumeFromWhere fromWhere() default ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;

    MessageModel messageModel() default MessageModel.CLUSTERING;

    String instanceName() default "";
}
