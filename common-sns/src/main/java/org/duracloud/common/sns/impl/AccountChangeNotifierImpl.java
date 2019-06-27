/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.common.sns.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.duracloud.account.db.model.GlobalProperties;
import org.duracloud.account.db.repo.GlobalPropertiesRepo;
import org.duracloud.common.event.AccountChangeEvent;
import org.duracloud.common.event.AccountChangeEvent.EventType;
import org.duracloud.common.sns.AccountChangeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Bernstein
 */
@Component("accountChangeNotifier")
public class AccountChangeNotifierImpl implements AccountChangeNotifier {

    private AmazonSNS snsClient;

    private Channel rabbitMqChannel;

    private String rabbitmqExchange;

    private String notifierType;

    private GlobalPropertiesRepo globalPropertiesRepo;

    private static Logger log = LoggerFactory.getLogger(AccountChangeNotifierImpl.class);

    /**
     * @param globalPropertiesRepo
     */
    @Autowired
    public AccountChangeNotifierImpl(GlobalPropertiesRepo globalPropertiesRepo) {
        this.globalPropertiesRepo = globalPropertiesRepo;
        GlobalProperties props = globalPropertiesRepo.findAll().get(0);
        notifierType = props.getNotifierType();
        if(notifierType == "AWS") {
            this.snsClient = AmazonSNSClientBuilder.defaultClient();
        }else if (notifierType == "RABBITMQ"){
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(props.getRabbitmqUsername());
            factory.setPassword(props.getRabbitmqUsername());
            factory.setVirtualHost("/");
            factory.setHost(props.getRabbitmqHost());
            factory.setPort(5672);
            try {
                Connection conn = factory.newConnection();
                rabbitMqChannel = conn.createChannel();
            }catch (Exception e){
                log.error("Failed to connect to RabbitMQ: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void accountChanged(String account) {
        publish(AccountChangeEvent.EventType.ACCOUNT_CHANGED, account);
    }

    private void publish(EventType eventType, String account) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("unable to resolve unknown host: " + e.getMessage(), e);
            host = "unknown";
        }

        AccountChangeEvent event = new AccountChangeEvent(eventType, account, host);

        try {
            log.debug("publishing event={}", event);
            if(notifierType == "AWS") {
                GlobalProperties props = globalPropertiesRepo.findAll().get(0);
                this.snsClient.publish(props.getInstanceNotificationTopicArn(),
                                       AccountChangeEvent.serialize(event));
            }else if (notifierType == "RABBITMQ") {
                rabbitMqChannel.basicPublish(rabbitmqExchange, "", null, AccountChangeEvent.serialize(event).getBytes());
            }
            log.info("published event={}", event);
        } catch (Exception e) {
            log.error("Failed to publish event: " + event + " : " + e.getMessage(), e);
        }
    }

    @Override
    public void storageProvidersChanged(String accountId) {
        publish(EventType.STORAGE_PROVIDERS_CHANGED,
                accountId);
    }

    @Override
    public void userStoreChanged(String accountId) {
        publish(EventType.USERS_CHANGED,
                accountId);
    }

    @Override
    public void rootUsersChanged() {
        publish(EventType.ALL_ACCOUNTS_CHANGED, null);
    }

    @Override
    public void storageProviderCacheOnNodeChanged(String account) {
        publish(EventType.STORAGE_PROVIDER_CACHE_ON_NODE_CHANGED, account);
    }
}
