package com.jonas.rmq.configuration;

import com.jonas.rmq.annotation.EnableRocketMq;
import com.jonas.rmq.core.RocketMQTemplate;
import com.jonas.rmq.support.converter.MessageBodyConverter;
import com.jonas.rmq.support.converter.StringMessageBodyConverter;
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
