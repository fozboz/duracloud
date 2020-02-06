/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.storage.domain;

/**
 * @author Bill Branan
 * Date: 3/18/14
 */
public class AuditConfig {

    private String auditQueueName;
    private String auditLogSpaceId;
    private String auditQueueType;
    private String rabbitmqHost;
    private Integer rabbitmqPort;
    private String rabbitmqVhost;
    private String rabbitmqExchange;
    private String rabbitmqUsername;
    private String rabbitmqPassword;
    private String awsAccessKey;
    private String awsSecretKey;
    private String swiftEndpoint;
    private String swiftSignerType;

    public String getAuditQueueName() {

        return auditQueueName;
    }

    public void setAuditQueueName(String auditQueueName) {

        this.auditQueueName = auditQueueName;
    }

    public String getAuditLogSpaceId() {

        return auditLogSpaceId;
    }

    public void setAuditLogSpaceId(String auditLogSpaceId) {

        this.auditLogSpaceId = auditLogSpaceId;
    }

    public String getAuditQueueType() {

        return auditQueueType;
    }

    public void setAuditQueueType(String auditQueueType) {

        this.auditQueueType = auditQueueType;
    }

    public String getRabbitmqHost() {
        return rabbitmqHost;
    }

    public void setRabbitmqHost(String rabbitmqHost) {
        this.rabbitmqHost = rabbitmqHost;
    }

    public Integer getRabbitmqPort() {
        return rabbitmqPort;
    }

    public void setRabbitmqPort(Integer rabbitmqPort) {
        this.rabbitmqPort = rabbitmqPort;
    }

    public String getRabbitmqVhost() {
        return rabbitmqVhost;
    }

    public void setRabbitmqVhost(String rabbitmqVhost) {
        this.rabbitmqVhost = rabbitmqVhost;
    }

    public String getRabbitmqExchange() {
        return rabbitmqExchange;
    }

    public void setRabbitmqExchange(String rabbitmqExchange) {
        this.rabbitmqExchange = rabbitmqExchange;
    }

    public void setRabbitmqUsername(String rabbitmqUsername) {
        this.rabbitmqUsername = rabbitmqUsername;
    }

    public String getRabbitmqPassword() {
        return rabbitmqPassword;
    }

    public void setRabbitmqPassword(String rabbitmqPassword) {
        this.rabbitmqPassword = rabbitmqPassword;
    }

    public String getAwsAccessKey() { return awsAccessKey; }

    public void setAwsAccessKey(String awsAccessKey) { this.awsAccessKey = awsAccessKey; }

    public String getAwsSecretKey() { return awsSecretKey; }

    public void setAwsSecretKey(String awsSecretKey) { this.awsSecretKey = awsSecretKey; }

    public String getSwiftEndpoint() { return swiftEndpoint; }

    public void setSwiftEndpoint(String swiftEndpoint) { this.swiftEndpoint = swiftEndpoint; }

    public String getSwiftSignerType() {
        return swiftSignerType;
    }

    public void setSwiftSignerType(String swiftSignerType) {
        this.swiftSignerType = swiftSignerType;
    }
}
