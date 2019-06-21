package org.duracloud.common.sns;

import java.util.ArrayList;
import java.util.List;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQSubscriptionManager implements SubscriptionManager {
    private Logger log = LoggerFactory.getLogger(RabbitMQSubscriptionManager.class);

    private Channel mqChannel;
    private String mqHost;
    private String queueName;
    private String queueUrl;
    private String mqUsername;
    private String mqPassword;
    private String exchangeName;
    private boolean initialized = false;
    private List<MessageListener> messageListeners = new ArrayList<>();


    public RabbitMQSubscriptionManager(String host, String exchange, String username, String password,  String queueName) {
        mqHost = host;
        exchangeName = exchange;
        mqUsername = username;
        mqPassword = password;
        this.queueName = queueName;
    }

    @Override
    public void addListener(MessageListener listener)  {
        this.messageListeners.add(listener);
    }

    @Override
    public void connect() {
        if (initialized) {
            throw new DuraCloudRuntimeException("this manager is already connected");
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(mqUsername);
            factory.setPassword(mqPassword);
            factory.setVirtualHost("/");
            factory.setHost(mqHost);
            factory.setPort(5672);
            Connection conn = factory.newConnection();
            mqChannel = conn.createChannel();
            queueUrl = "RabbitMQ-" + conn.getAddress();

            try {
                //Queue Exists
                mqChannel.queueBind(queueName, exchangeName, queueName);
            }
            catch (Exception ex) {
                // Create Queue
                mqChannel.queueDeclare(queueName, true, false, false, null);
                mqChannel.queueBind(queueName, exchangeName, queueName);
            }

        } catch (Exception ex) {
            log.error("failed to estabilish connection to RabbitMQ with queue name {} and URL {} because {}", queueName, queueUrl, ex.getMessage());
            throw new DuraCloudRuntimeException(ex);
        }

    }

    private void dispatch(String message) {
        log.debug("dispatching message {}", message);
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessage(message);
            } catch (Exception ex) {
                log.error("failed to dispatch message " + message
                          + " to "
                          + listener
                          + "due to "
                          + ex.getMessage(),
                          ex);
            }
        }
    }

    @Override
    public void disconnect() {

    }
}
