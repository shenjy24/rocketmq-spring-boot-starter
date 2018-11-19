/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.yushi.configuration;

import com.yushi.annotation.EnableRocketMq;
import com.yushi.core.RocketMQTemplate;
import com.yushi.support.converter.MessageBodyConverter;
import com.yushi.support.converter.StringMessageBodyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMq配置
 */
@Configuration
@EnableRocketMq
public class RocketMqConfiguration {

    @Value("${spring.rocketmq.namesrvAddr}")
    private String namesrvAddr;

    private String instanceName = "yxm_producer";

    @Autowired
    @Qualifier("messageBodyConverter")
    private MessageBodyConverter<?> messageBodyConverter;

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setNamesrvAddr(namesrvAddr);
        rocketMQTemplate.setInstanceName(instanceName);
        rocketMQTemplate.setMessageBodyConverter(messageBodyConverter);
        return rocketMQTemplate;
    }

    @Bean(name = "messageBodyConverter")
    public MessageBodyConverter<?> messageBodyConverter() {
        return new StringMessageBodyConverter();
    }
}
