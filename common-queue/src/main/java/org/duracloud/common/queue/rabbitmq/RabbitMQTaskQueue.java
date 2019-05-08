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
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
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


    public enum MsgProp {
        MSG_ID, RECEIPT_HANDLE;
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
    public RabbitMQTaskQueue(String queueName) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setVirtualHost("/");
            factory.setHost("localhost");
            factory.setPort(5627);
            Connection conn = factory.newConnection();
            this.mqChannel = conn.createChannel();
            this.mqChannel.queueBind(queueName, "", queueName);
            this.queueName = queueName;
        } catch (Exception ex) {
            log.error("failed to estabilish connection to RabbitMQ ", queueName, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }
    }

    public RabbitMQTaskQueue(Channel rabbitMQChannel, String queueName) {
        try {
            this.mqChannel = rabbitMQChannel;
            this.mqChannel.queueBind(queueName, "", queueName);
            this.queueName = queueName;
        }catch (Exception ex) {
            log.error("failed to estabilish connection to RabbitMQ ", queueName, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }
    }

    @Override
    public String getName() {
        return this.queueName;
    }


    protected Task marshallTask(Message msg) {
        Properties props = new Properties();
        Task task = null;
        try {
            props.load(new StringReader(msg.getBody()));

            if (props.containsKey(Task.KEY_TYPE)) {
                task = new Task();
                for (final String key : props.stringPropertyNames()) {
                    if (key.equals(Task.KEY_TYPE)) {
                        task.setType(Task.Type.valueOf(props.getProperty(key)));
                    } else {
                        task.addProperty(key, props.getProperty(key));
                    }
                }
                task.addProperty(MsgProp.MSG_ID.name(), msg.getMessageId());
                task.addProperty(MsgProp.RECEIPT_HANDLE.name(), msg.getReceiptHandle());
            } else {
                log.error("RabbitMQ message from queue: " + queueName + ", does not contain a 'task type'");
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
                    mqChannel.basicPublish("", queueName, null, messageBodyBytes);
                    return null;
                }
            });

            log.info("SQS message successfully placed {} on queue - queue: {}",
                     task, queueName);

        } catch (Exception ex) {
            log.error("failed to place {} on {} due to {}", task, queueName, ex.getMessage());
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
        ReceiveMessageResult result = sqsClient.receiveMessage(
            new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(maxTasks)
                .withAttributeNames("SentTimestamp", "ApproximateReceiveCount"));
        if (result.getMessages() != null && result.getMessages().size() > 0) {
            Set<Task> tasks = new HashSet<>();
            for (Message msg : result.getMessages()) {

                // The Amazon docs claim this attribute is 'returned as an integer
                // representing the epoch time in milliseconds.'
                // http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/Query_QueryReceiveMessage.html
                try {
                    Long sentTime = Long.parseLong(msg.getAttributes().get("SentTimestamp"));
                    Long preworkQueueTime = System.currentTimeMillis() - sentTime;
                    log.info("SQS message received - queue: {}, queueUrl: {}, msgId: {}," +
                             " preworkQueueTime: {}, receiveCount: {}"
                        , queueName, queueUrl, msg.getMessageId()
                        , DurationFormatUtils.formatDuration(preworkQueueTime, "HH:mm:ss,SSS")
                        , msg.getAttributes().get("ApproximateReceiveCount"));
                } catch (NumberFormatException nfe) {
                    log.error("Error converting 'SentTimestamp' SQS message" +
                              " attribute to Long, messageId: " +
                              msg.getMessageId(), nfe);
                }

                Task task = marshallTask(msg);
                task.setVisibilityTimeout(visibilityTimeout);
                tasks.add(task);
            }

            return tasks;
        } else {
            throw new TimeoutException("No tasks available from queue: " +
                                       queueName + ", queueUrl: " + queueUrl);
        }
    }

    @Override
    public Task take() throws TimeoutException {
        return take(1).iterator().next();
    }

    @Override
    public void extendVisibilityTimeout(Task task) throws TaskNotFoundException {
    }

    @Override
    public void deleteTask(Task task) throws TaskNotFoundException {
        try {
            sqsClient.deleteMessage(new DeleteMessageRequest()
                                        .withQueueUrl(queueUrl)
                                        .withReceiptHandle(
                                            task.getProperty(MsgProp.RECEIPT_HANDLE.name())));
            log.info("successfully deleted {}", task);

        } catch (ReceiptHandleIsInvalidException rhe) {
            log.error("failed to delete task " + task + ": " + rhe.getMessage(), rhe);
            throw new TaskNotFoundException(rhe);
        }
    }

    @Override
    public void deleteTasks(Set<Task> tasks) throws TaskException {
        if (tasks.size() > 10) {
            throw new IllegalArgumentException("task set must contain 10 or fewer tasks");
        }

        try {

            List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>(tasks.size());

            for (Task task : tasks) {
                DeleteMessageBatchRequestEntry entry =
                    new DeleteMessageBatchRequestEntry().withId(task.getProperty(MsgProp.MSG_ID.name()))
                                                        .withReceiptHandle(
                                                            task.getProperty(MsgProp.RECEIPT_HANDLE.name()));
                entries.add(entry);
            }

            DeleteMessageBatchRequest request = new DeleteMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(entries);
            DeleteMessageBatchResult result = sqsClient.deleteMessageBatch(request);
            List<BatchResultErrorEntry> failed = result.getFailed();
            if (failed != null && failed.size() > 0) {
                for (BatchResultErrorEntry error : failed) {
                    log.info("failed to delete message: " + error);
                }
            }

            for (DeleteMessageBatchResultEntry entry : result.getSuccessful()) {
                log.info("successfully deleted {}", entry);
            }

        } catch (AmazonServiceException se) {
            log.error("failed to batch delete tasks " + tasks + ": " + se.getMessage(), se);

            throw new TaskException(se);
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
            deleteTask(task);
        } catch (TaskNotFoundException e) {
            log.error("unable to delete " + task + " ignoring - requeuing anyway");
        }

        put(task);
        log.warn("requeued {} after {} failed attempts.", task, attempts);

    }

    @Override
    public Integer size() {
        GetQueueAttributesResult result = queryQueueAttributes(QueueAttributeName.ApproximateNumberOfMessages);
        String sizeStr = result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessages.name());
        Integer size = Integer.parseInt(sizeStr);
        return size;
    }

    @Override
    public Integer sizeIncludingInvisibleAndDelayed() {
        GetQueueAttributesResult result =
            queryQueueAttributes(QueueAttributeName.ApproximateNumberOfMessages,
                                 QueueAttributeName.ApproximateNumberOfMessagesNotVisible,
                                 QueueAttributeName.ApproximateNumberOfMessagesDelayed);
        Map<String, String> attributes = result.getAttributes();
        int size = 0;
        for (String attrKey : attributes.keySet()) {
            String value = attributes.get(attrKey);
            log.debug("retrieved attribute: {}={}", attrKey, value);
            int intValue = Integer.parseInt(value);
            size += intValue;
        }
        log.debug("calculated size: {}", size);
        return size;
    }

    private Integer getVisibilityTimeout() {
        GetQueueAttributesResult result = queryQueueAttributes(QueueAttributeName.VisibilityTimeout);
        String visStr = result.getAttributes().get(QueueAttributeName.VisibilityTimeout.name());
        Integer visibilityTimeout = Integer.parseInt(visStr);
        return visibilityTimeout;
    }

    private String getQueueUrl() {
        return sqsClient.getQueueUrl(
            new GetQueueUrlRequest().withQueueName(queueName)).getQueueUrl();
    }

    private GetQueueAttributesResult queryQueueAttributes(QueueAttributeName... attrNames) {
        return sqsClient.getQueueAttributes(new GetQueueAttributesRequest()
                                                .withQueueUrl(queueUrl)
                                                .withAttributeNames(attrNames));
    }

}
