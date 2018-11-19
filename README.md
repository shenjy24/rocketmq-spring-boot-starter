spring-boot-starter-rocketmq的集成使用

# 使用
1. 引入依赖
```xml
    <dependency>
        <groupId>com.yushi</groupId>
        <artifactId>spring-boot-starter-rocketmq</artifactId>
        <version>1.0.8</version>
    </dependency>
```
2. application.properties
```
#rocketmq配置
spring.rocketmq.namesrvAddr=192.168.51.80:9876;192.168.51.81:9876
```

3. 生产者
// topic手动在console创建.
```java
@Service
public class RocketMQService {

    @Autowired
    private RocketMQTemplate rmq;
    
    MessageBuilder messageBuilder = MessageBuilder
        .withTopic(MqTopicAndGroup.TOPIC_SEND_EMAIL) // 消息主题，枚举
        .withBody("${messageBody}") //JSON字符串
        .withKey("${key}");
    
    try {
       // 同步发送消息
       rmq.setProducerGroup(MqTopicAndGroup.GROUP_SEND_EMAIL);
       rmq.send(messageBuilder);
    } catch (Exception e) {
        GwsLogger.error(e, "发送集群消息出现异常");
    }
}
```

4. 消费者,这里模拟了两个消费监听者
```java
@RocketMqConsumer
public class RocketMqTestListenHandler {
    @RocketMqListener(topic = MqTopicAndGroup.TOPIC_SEND_EMAIL,
        group = MqTopicAndGroup.GROUP_SEND_EMAIL)
    public void onReceive1(String msgId, String msg) {
        // 监听1
        GwsLogger.info("消息msgId={},msgBody是:{}:", msgId, msg);
    }

    @RocketMqListener(topic = MqTopicAndGroup.TOPIC_SEND_EMAIL,
        group = MqTopicAndGroup.GROUP_SEND_EMAIL)
    public void onReceive2(String msgId, String msg) {
        // 监听2
        GwsLogger.info("消息msgId={},msgBody是:{}:", msgId, msg);
    }
}
```
# 扩展功能

   
