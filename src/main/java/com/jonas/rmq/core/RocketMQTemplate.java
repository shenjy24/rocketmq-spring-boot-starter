package com.jonas.rmq.core;

import com.jonas.rmq.enums.RmqDelayTime;
import com.jonas.rmq.support.builder.MessageBuilder;
import com.jonas.rmq.support.converter.MessageBodyConverter;
import com.jonas.rmq.support.utils.ThreadUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.concurrent.*;


/**
 * RocketMQTemplate
 * 同步发送消息和异步发送消息两种
 */
public class RocketMQTemplate implements DisposableBean {

    private String producerGroup;

    private String namesrvAddr;

    private String instanceName;

    private MessageBodyConverter<?> messageBodyConverter;

    private boolean shareProducer = false;

    private volatile DefaultMQProducer sharedProducer;

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Returns the message body converter.The default is an instance of
     *
     * @return
     */
    public MessageBodyConverter<?> getMessageBodyConverter() {
        return this.messageBodyConverter;
    }

    /**
     * Set message body converter.
     *
     * @param messageBodyConverter
     */
    public void setMessageBodyConverter(MessageBodyConverter<?> messageBodyConverter) {
        if (messageBodyConverter == null) {
            throw new IllegalArgumentException("Null messageBodyConverter");
        }
        this.messageBodyConverter = messageBodyConverter;
    }

    /**
     * returns if share a message producer between topics.It's false by default.
     *
     * @return
     */
    public boolean isShareProducer() {
        return this.shareProducer;
    }

    /**
     * If true, the template will share a message producer between topics.It's
     * false by default.
     *
     * @param producerPerTopic
     */
    public void setShareProducer(boolean producerPerTopic) {
        this.shareProducer = producerPerTopic;
    }

    private final ConcurrentHashMap<String, FutureTask<DefaultMQProducer>> producers =
            new ConcurrentHashMap<String, FutureTask<DefaultMQProducer>>();

    /**
     * Returns or create a message producer for topic.
     *
     * @param topic
     * @return
     */
    public DefaultMQProducer getOrCreateProducer(final String topic) {
        if (!this.shareProducer) {
            FutureTask<DefaultMQProducer> task = this.producers.get(topic);
            if (task == null) {
                task = new FutureTask<DefaultMQProducer>(new Callable<DefaultMQProducer>() {

                    @Override
                    public DefaultMQProducer call() throws Exception {
                        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
                        producer.setNamesrvAddr(namesrvAddr);
                        producer.setInstanceName(instanceName + "_" + topic);
                        producer.start();
                        return producer;
                    }

                });
                FutureTask<DefaultMQProducer> oldTask = this.producers.putIfAbsent(topic, task);
                if (oldTask != null) {
                    task = oldTask;
                } else {
                    task.run();
                }
            }

            try {
                DefaultMQProducer producer = task.get();
                return producer;
            } catch (ExecutionException e) {
                throw ThreadUtils.launderThrowable(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } else {
            if (this.sharedProducer == null) {
                synchronized (this) {
                    if (this.sharedProducer == null) {
                        sharedProducer = new DefaultMQProducer(producerGroup);
                        sharedProducer.setNamesrvAddr(namesrvAddr);
                        sharedProducer.setInstanceName(instanceName);
                    }
                }
            }
            try {
                this.sharedProducer.start();
            } catch (MQClientException e) {
                e.printStackTrace();
            }
            return this.sharedProducer;
        }
        throw new IllegalStateException("Could not create producer for topic '" + topic + "'");
    }

    /**
     * Send message built by message builder.Returns the sent result.
     *
     * @param builder
     * @return
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public SendResult send(MessageBuilder builder) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        return producer.send(msg);
    }

    /**
     * @param builder
     * @param delayTime
     * @return
     * @throws InterruptedException
     * @throws MQClientException
     * @throws RemotingException
     * @throws MQBrokerException
     */
    public SendResult send(MessageBuilder builder, RmqDelayTime delayTime) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        if (delayTime != null) {
            msg.setDelayTimeLevel(Integer.valueOf(delayTime.getDelayTimeLevel()));
        }
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        return producer.send(msg);
    }


    /**
     * Send message built by message builder.Returns the sent result.
     *
     * @param builder
     * @param timeout
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public SendResult send(MessageBuilder builder, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        return producer.send(msg, timeout);
    }


    /**
     * 异步发送消息.
     *
     * @param builder
     * @param sendCallback
     * @throws MQClientException
     * @throws RemotingException
     * @throws MQBrokerException
     * @throws InterruptedException
     */
    public void asyncSend(MessageBuilder builder, SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        producer.send(msg, sendCallback);
    }

    /**
     * 异步发送消息.
     * 设置消息的发送有效周期
     *
     * @param builder
     * @param timeout
     * @param sendCallback
     * @throws MQClientException
     * @throws RemotingException
     * @throws MQBrokerException
     * @throws InterruptedException
     */
    public void asyncSend(MessageBuilder builder, long timeout, SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        producer.send(msg, sendCallback, timeout);
    }

    /**
     * 异步发送消息.
     * 延迟发送
     *
     * @param builder
     * @param delayTime
     * @param sendCallback
     * @throws MQClientException
     * @throws RemotingException
     * @throws MQBrokerException
     * @throws InterruptedException
     */
    public void asyncSend(MessageBuilder builder, RmqDelayTime delayTime, SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        Message msg = builder.build(this.messageBodyConverter);
        if (delayTime != null) {
            msg.setDelayTimeLevel(Integer.valueOf(delayTime.getDelayTimeLevel()));
        }
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        producer.send(msg, sendCallback);
    }


    /**
     * 顺序发送消息.
     *
     * @param builder
     * @param arg
     * @return
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public SendResult seqSend(MessageBuilder builder, Object arg) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        return producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                Long id = (Long) arg;
                long index = id % mqs.size();
                return mqs.get((int) index);
            }
        }, arg);
    }

    /**
     * 顺序发送消息.
     *
     * @param builder
     * @param arg
     * @param timeout
     * @return
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public SendResult seqSend(MessageBuilder builder, Object arg, long timeout) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        return producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                Long id = (Long) arg;
                long index = id % mqs.size();
                return mqs.get((int) index);
            }
        }, arg, timeout);
    }


    /**
     * 异步顺序发送消息.
     *
     * @param builder
     * @param arg
     * @param sendCallback
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public void asyncSeqSend(MessageBuilder builder, Object arg, SendCallback sendCallback) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                Long id = (Long) arg;
                long index = id % mqs.size();
                return mqs.get((int) index);
            }
        }, arg, sendCallback);
    }

    /**
     * 异步顺序发送消息.
     *
     * @param builder
     * @param arg
     * @param sendCallback
     * @param timeout
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    public void asyncSeqSend(MessageBuilder builder, Object arg, SendCallback sendCallback, long timeout) throws InterruptedException, MQClientException, RemotingException, MQBrokerException {
        Message msg = builder.build(this.messageBodyConverter);
        final String topic = msg.getTopic();
        DefaultMQProducer producer = this.getOrCreateProducer(topic);
        producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                Long id = (Long) arg;
                long index = id % mqs.size();
                return mqs.get((int) index);
            }
        }, arg, sendCallback, timeout);
    }

    @Override
    public void destroy() throws Exception {
        if (this.sharedProducer != null) {
            this.sharedProducer.shutdown();
            this.sharedProducer = null;
        }
        for (FutureTask<DefaultMQProducer> task : this.producers.values()) {
            try {
                DefaultMQProducer producer = task.get(5000, TimeUnit.MILLISECONDS);
                if (producer != null) {
                    producer.shutdown();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        this.producers.clear();

    }

}
