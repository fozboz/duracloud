/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.common.queue.rabbitmq;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.queue.TaskException;
import org.duracloud.common.queue.TaskNotFoundException;
import org.duracloud.common.queue.TaskQueue;
import org.duracloud.common.queue.TimeoutException;
import org.duracloud.common.queue.task.Task;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.retry.Retrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQSTaskQueue acts as the interface for interacting with an Amazon
 * Simple Queue Service (SQS) queue.
 * This class provides a way to interact with a remote SQS Queue, it
 * emulates the functionality of a queue.
 *
 * @author Erik Paulsson
 * Date: 10/21/13
 */
public class RabbitMQTaskQueue implements TaskQueue {
    private static Logger log = LoggerFactory.getLogger(RabbitMQTaskQueue.class);

    private Channel mqChannel;
    private String queueName;
    private Integer visibilityTimeout;  // in seconds
    private Integer unAcknowlededMesageCount = 0;
    private String queueUrl;
    private String exchangeName;

    public enum MsgProp {
        DELIVERY_TAG, ROUTING_KEY, EXCHANGE
    }

    /**
     * Creates a SQSTaskQueue that serves as a handle to interacting with a
     * remote Amazon SQS Queue.
     * The AmazonSQSClient will search for Amazon credentials on the system as
     * described here:
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
     *
     * Moreover, it is possible to set the region to use via the AWS_REGION
     * environment variable or one of the other methods described here:
     * http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
     */
    public RabbitMQTaskQueue(String exchangeName, String queueName) {
        try {
            this.exchangeName = exchangeName;
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setVirtualHost("/");
            factory.setHost("localhost");
            factory.setPort(5672);
            Connection conn = factory.newConnection();
            mqChannel = conn.createChannel();
            mqChannel.queueBind(queueName, exchangeName, queueName);
            queueUrl = "RabbitMQ-" + conn.getAddress();
            this.queueName = queueName;
        } catch (Exception ex) {
            log.error("failed to estabilish connection to RabbitMQ with queue name {} and URL {} because {}", queueName, queueUrl, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }
    }

    public RabbitMQTaskQueue(Connection conn, String queueName) {
        try {
            mqChannel = conn.createChannel();
            mqChannel.queueBind(queueName, exchangeName, queueName);
            queueUrl = "RabbitMQ-" + conn.getAddress();
            this.queueName = queueName;
        } catch (Exception ex) {
            log.error("failed to estabilish connection to RabbitMQ with queue name {} and URL {} because {}", queueName, queueUrl, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }
    }

    @Override
    public String getName() {
        return this.queueName;
    }


    protected Task marshallTask(byte[] msgBody, long deliveryTag, String routingKey, String exhcange) {
        Properties props = new Properties();
        Task task = null;
        String msg = Arrays.toString(msgBody);
        try {
            props.load(new StringReader(msg));

            if (props.containsKey(Task.KEY_TYPE)) {
                task = new Task();
                for (final String key : props.stringPropertyNames()) {
                    if (key.equals(Task.KEY_TYPE)) {
                        task.setType(Task.Type.valueOf(props.getProperty(key)));
                    } else {
                        task.addProperty(key, props.getProperty(key));
                    }
                }
                task.addProperty(MsgProp.DELIVERY_TAG.name(), String.valueOf(deliveryTag));
                task.addProperty(MsgProp.ROUTING_KEY.name(), routingKey);
                task.addProperty(MsgProp.EXCHANGE.name(), exhcange);
            } else {
                log.error("RabbitMQ message from queue: " + queueName + " at " + queueUrl +  ", does not contain a 'task type'");
            }
        } catch (IOException ioe) {
            log.error("Error creating Task", ioe);
        }
        return task;
    }

    protected String unmarshallTask(Task task) {
        Properties props = new Properties();
        props.setProperty(Task.KEY_TYPE, task.getType().name());
        for (String key : task.getProperties().keySet()) {
            String value = task.getProperty(key);
            if (null != value) {
                props.setProperty(key, value);
            }
        }
        StringWriter sw = new StringWriter();
        String msgBody = null;
        try {
            props.store(sw, null);
            msgBody = sw.toString();
        } catch (IOException ioe) {
            log.error("Error unmarshalling Task, queue: " + queueName +
                      ", msgBody: " + msgBody, ioe);
        }
        return msgBody;
    }


    @Override
    public void put(final Task task) {
        try {
            String queueName = this.queueName;
            final String msgBody = unmarshallTask(task);
            byte[] messageBodyBytes = msgBody.getBytes();
            new Retrier(4, 10000, 2).execute(new Retriable() {
                @Override
                public Object retry() throws Exception {
                    mqChannel.basicPublish(exchangeName, queueName, null, messageBodyBytes);
                    return null;
                }
            });
            unAcknowlededMesageCount++;
            log.info("RabbitMQ message successfully placed {} on queue - queue: {}",
                     task, queueName);

        } catch (Exception ex) {
            log.error("failed to place {} on {} at {} due to {}", task, queueName, queueUrl, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }
    }

    /**
     * Convenience method that calls put(Set<Task>)
     *
     * @param tasks
     */
    @Override
    public void put(Task... tasks) {
        for (Task task : tasks) {
            this.put(task);
        }
    }

    /**
     * Puts multiple tasks on the queue using batch puts.  The tasks argument
     * can contain more than 10 Tasks, in that case there will be multiple SQS
     * batch send requests made each containing up to 10 messages.
     *
     * @param tasks
     */
    @Override
    public void put(Set<Task> tasks) {
        for (Task task : tasks) {
            this.put(task);
        }
    }

    @Override
    public Set<Task> take(int maxTasks) throws TimeoutException {
        Integer size = size();
        if (size > 0) {
            if(size < maxTasks){
                size = maxTasks;
            }
            Set<Task> tasks = new HashSet<>();
            try {
                for (int i = 0; i < size; i++) {
                    Task task = take();
                    tasks.add(task);
                }
                return tasks;
            } catch (Exception e){
                for (Task task : tasks) {
                    requeue(task);
                }
                throw new TimeoutException("Failed to get at least one message from queue: " +
                                           queueName + ", queueUrl: " + queueUrl);
            }
        } else {
            throw new TimeoutException("No tasks available from queue: " +
                                       queueName + ", queueUrl: " + queueUrl);
        }
    }

    @Override
    public Task take() throws TimeoutException {
        try {
            boolean autoAck = false;
            GetResponse response = mqChannel.basicGet(queueName, autoAck);
            if (response == null) {
                throw new TimeoutException("No tasks available from queue: " +
                                           queueName + ", queueUrl: " + queueUrl);
            } else {
                AMQP.BasicProperties properties = response.getProps();
                Envelope envelope = response.getEnvelope();
                byte[] body = response.getBody();
                String routingKey = envelope.getRoutingKey();
                String exchange = envelope.getExchange();
                long deliveryTag = envelope.getDeliveryTag();
                Long sentTime = properties.getTimestamp().getTime();
                Long preworkQueueTime = System.currentTimeMillis() - sentTime;
                log.info("RabbitMQ message received - queue: {}, queueUrl: {}, deliveryTag: {}, preworkQueueTime: {}"
                    , queueName, queueUrl, deliveryTag
                    , DurationFormatUtils.formatDuration(preworkQueueTime, "HH:mm:ss,SSS"));
                Task task = marshallTask(body, deliveryTag, routingKey, exchange);
                return task;
            }
        } catch (Exception ex) {
            log.error("failed to take task from {} due to {}", queueName, ex.getMessage());
            throw new TimeoutException("No tasks available from queue: " +
                                       queueName);
        }
    }

    @Override
    public void extendVisibilityTimeout(Task task) throws TaskNotFoundException {
    }

    @Override
    public void deleteTask(Task task) throws TaskNotFoundException {
        try {
            mqChannel.basicAck(Long.parseLong(task.getProperty(MsgProp.DELIVERY_TAG.name())), false);
            log.info("successfully deleted {}", task);
            unAcknowlededMesageCount--;

        } catch (Exception e) {
            log.error("failed to delete task " + task + ": " + e.getMessage(), e);
            throw new TaskNotFoundException(e);
        }
    }

    @Override
    public void deleteTasks(Set<Task> tasks) throws TaskException {
        if (tasks.size() > 10) {
            throw new IllegalArgumentException("task set must contain 10 or fewer tasks");
        }

        try {
            for (Task task : tasks) {
                deleteTask(task);
            }

        } catch (Exception e) {
            log.error("failed to batch delete tasks " + tasks + ": " + e.getMessage(), e);

            throw new TaskException(e);
        }

    }

    /* (non-Javadoc)
     * @see org.duracloud.queue.TaskQueue#requeue(org.duracloud.queue.task.Task)
     */
    @Override
    public void requeue(Task task) {
        int attempts = task.getAttempts();
        task.incrementAttempts();
        try {
            mqChannel.basicReject(Long.parseLong(task.getProperty(MsgProp.DELIVERY_TAG.name())), true);
            unAcknowlededMesageCount--;
        } catch (Exception e) {
            log.error("unable to reject message {}, re-put message instead ", task);
            put(task);
        }

        log.warn("requeued {} after {} failed attempts.", task, attempts);

    }

    @Override
    public Integer size() {
        try {
            Long sizeLong =  mqChannel.messageCount(queueName);
            return sizeLong.intValue();
        }catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer sizeIncludingInvisibleAndDelayed() {
        return size() + unAcknowlededMesageCount;
    }

    private Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    private String getQueueUrl() {
        return queueUrl;
    }


}
