/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.jonas.rmq.core;

import com.jonas.rmq.annotation.RocketMqConsumer;
import com.jonas.rmq.annotation.RocketMqListener;
import com.jonas.rmq.enums.ConsumerTypeEnum;
import com.jonas.rmq.support.converter.StringMessageBodyConverter;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 消费者自动配置
 */
public class MqConsumerBeanAutoConfiguration
        implements ApplicationContextAware, ApplicationListener {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.rocketmq.namesrvAddr}")
    private String namesrvAddr;

    @Value("${spring.rocketmq.consumeThreadMax:20}")
    private Integer consumeThreadMax;

    @Value("${spring.rocketmq.consumeThreadMin:10}")
    private Integer consumeThreadMin;

    private ApplicationContext appCtx;

    protected final CopyOnWriteArraySet<DefaultMQPushConsumer> pushConsumers = new CopyOnWriteArraySet<DefaultMQPushConsumer>();

    protected final CopyOnWriteArraySet<DefaultMQPullConsumer> pullConsumers = new CopyOnWriteArraySet<DefaultMQPullConsumer>();


    private Map<String, Boolean> wiredBeans = new HashMap<String, Boolean>();


    private final StringMessageBodyConverter messageBodyConverter = new StringMessageBodyConverter();


    public static final ReflectionUtils.MethodFilter CONSUMER_METHOD_FILTER = new ReflectionUtils.MethodFilter() {
        @Override
        public boolean matches(Method method) {
            return null != AnnotationUtils.findAnnotation(method, RocketMqListener.class);
        }

    };

    /**
     * set context
     * <p>
     * (non-Javadoc)
     *
     * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        this.appCtx = appCtx;

    }

    @Override
    public void onApplicationEvent(ApplicationEvent ev) {
        if (ev instanceof ContextRefreshedEvent) {
            ApplicationContext ctx = ((ContextRefreshedEvent) ev).getApplicationContext();
            if (ctx != appCtx) {
                return;
            }
            refreshedEvent(ctx);
        } else if (ev instanceof ContextClosedEvent) {
            closeEvent();
        }

    }

    private void closeEvent() {
        for (DefaultMQPushConsumer consumer : this.pushConsumers) {
            consumer.shutdown();
        }
        this.pushConsumers.clear();
    }


    /**
     * 刷新事件处理
     *
     * @param ctx
     * @author liuyi 2017年4月26日
     */
    private void refreshedEvent(ApplicationContext ctx) {
        for (String beanName : ctx.getBeanDefinitionNames()) {
            Set<Method> methods = new HashSet<Method>();
            Class<?> type = ctx.getType(beanName);
            if (type == null) {
                continue;
            }

            RocketMqConsumer rocketMqConsumer = AnnotationUtils.findAnnotation(type,
                    RocketMqConsumer.class);
            if (null == rocketMqConsumer) {
                wiredBeans.put(beanName, Boolean.FALSE);
                continue;
            }

            if (wiredBeans.containsKey(beanName)) {
                continue;
            }
            ConsumerTypeEnum consumerType = rocketMqConsumer.consumerType();

            try {
                methods.addAll(findHandlerMethods(type, CONSUMER_METHOD_FILTER));
                wireBean(ctx.getBean(beanName), methods, consumerType);
            } catch (Exception e) {
                logger.error("init rocketmq error:" + e.getMessage());
            }

            wiredBeans.put(beanName, Boolean.TRUE);
        }

    }

    public void wireBean(final Object bean, final Set<Method> methods, ConsumerTypeEnum consumerType) throws MQClientException {
        if (methods.isEmpty()) {
            return;
        }
        CopyOnWriteArraySet<DefaultMQPushConsumer> pushConsumersBean = new CopyOnWriteArraySet<DefaultMQPushConsumer>();
        RocketMqListener rocketMqListener;
        for (final Method method : methods) {
            // scanAnnotation method
            rocketMqListener = AnnotationUtils.findAnnotation(method, RocketMqListener.class);
            if (rocketMqListener != null) {
                // set listener group
                String groupName = rocketMqListener.group();
                if (StringUtils.isEmpty(groupName)) {
                    logger.error("未找到消费者对应的订阅组");
                }
                //create push consumer
                if (consumerType.equals(ConsumerTypeEnum.push)) {
                    DefaultMQPushConsumer pushConsumer = new DefaultMQPushConsumer(groupName);
                    pushConsumer.setConsumeFromWhere(rocketMqListener.fromWhere());
                    pushConsumer.subscribe(rocketMqListener.topic(), rocketMqListener.tags());
                    pushConsumer.setNamesrvAddr(namesrvAddr);
                    if (rocketMqListener.instanceName().trim().isEmpty()) {
                        pushConsumer.setInstanceName(rocketMqListener.topic() + "_" + groupName + "_" + method.getName());
                    }
                    pushConsumer.setVipChannelEnabled(false);
                    pushConsumer.setMessageModel(rocketMqListener.messageModel());
                    pushConsumer.setConsumeThreadMax(consumeThreadMax);
                    pushConsumer.setConsumeThreadMin(consumeThreadMin);
                    if (rocketMqListener.pushListenerType().getName().
                            equals(MessageListenerConcurrently.class.getName())) {

                        pushConsumer.registerMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                                            ConsumeConcurrentlyContext context) {
                                //不进行批量获取操作 所以暂会取一条
                                MessageExt messageExt = msgs.get(0);
                                String strMsgBody = "";
                                String msgId = "";
                                try {
                                    strMsgBody = messageBodyConverter.fromByteArray(messageExt.getBody());
                                    msgId = messageExt.getMsgId();
                                } catch (MQClientException e) {
                                    logger.error("messageBodyConverter error:" + e.getMessage());
                                }
                                ReflectionUtils.invokeMethod(method, bean, msgId, strMsgBody);
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }
                        });
                    } else if (rocketMqListener.pushListenerType().getName().
                            equals(MessageListenerOrderly.class.getName())) {
                        pushConsumer.registerMessageListener(
                                new MessageListenerOrderly() {

                                    @Override
                                    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                                               ConsumeOrderlyContext paramConsumeOrderlyContext) {
                                        //不进行批量获取操作 所以暂会取一条
                                        MessageExt messageExt = msgs.get(0);
                                        String strMsgBody = "";
                                        String msgId = "";
                                        try {
                                            strMsgBody = messageBodyConverter.fromByteArray(messageExt.getBody());
                                            msgId = messageExt.getMsgId();
                                        } catch (MQClientException e) {
                                            logger.error("messageBodyConverter error:" + e.getMessage());
                                        }
                                        ReflectionUtils.invokeMethod(method, bean, msgId, strMsgBody);
                                        return ConsumeOrderlyStatus.SUCCESS;
                                    }
                                }
                        );
                    }
                    //
                    pushConsumers.add(pushConsumer);
                    pushConsumersBean.add(pushConsumer);
                }
            }
        }
        pushConsumersBean.forEach(action -> {
            try {
                action.start();
            } catch (Exception e) {
                logger.error("消费者启动失败:" + action.getConsumerGroup() + ":" + e.getMessage());
            }
        });
    }


    private static Set<Method> findHandlerMethods(Class<?> handlerType,
                                                  final ReflectionUtils.MethodFilter handlerMethodFilter) {
        final Set<Method> handlerMethods = new LinkedHashSet<Method>();

        if (handlerType == null) {
            return handlerMethods;
        }

        Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
        Class<?> specificHandlerType = null;
        if (!Proxy.isProxyClass(handlerType)) {
            handlerTypes.add(handlerType);
            specificHandlerType = handlerType;
        }
        handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
        for (Class<?> currentHandlerType : handlerTypes) {
            final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
            ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(Method method) {
                    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                    Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
                    if (handlerMethodFilter.matches(specificMethod)
                            && (bridgedMethod == specificMethod || !handlerMethodFilter.matches(bridgedMethod))) {
                        handlerMethods.add(specificMethod);
                    }
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }
        return handlerMethods;
    }


}
