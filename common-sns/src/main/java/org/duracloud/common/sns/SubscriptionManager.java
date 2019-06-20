package org.duracloud.common.sns;

public interface SubscriptionManager {

    public void addListener(MessageListener listener);

    public void connect();

    public void disconnect();
}
