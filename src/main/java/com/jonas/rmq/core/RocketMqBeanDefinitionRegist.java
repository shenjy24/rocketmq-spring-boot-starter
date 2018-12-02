/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.jonas.rmq.core;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * RocketMqBeanDefinitionRegist
 */
public class RocketMqBeanDefinitionRegist implements ImportBeanDefinitionRegistrar {

    /**
     * 【请在此输入描述文字】
     * <p>
     * (non-Javadoc)
     *
     * @see ImportBeanDefinitionRegistrar#registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
        // Create a ConsumerBeanAutoConfiguration
        if (!registry.containsBeanDefinition(MqConsumerBeanAutoConfiguration.class.getName())) {
            BeanDefinitionBuilder autoConfigDef = BeanDefinitionBuilder.rootBeanDefinition
                    (MqConsumerBeanAutoConfiguration.class);
            registry.registerBeanDefinition(MqConsumerBeanAutoConfiguration.class.getName(), autoConfigDef
                    .getBeanDefinition());
        }
    }

}
