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
    private String auditQueueHost;

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

    public String getAuditQueueHost() {
        return auditQueueHost;
    }

    public void setAuditQueueHost(String auditQueueHost) {
        this.auditQueueHost = auditQueueHost;
    }

}
