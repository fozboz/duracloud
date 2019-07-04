/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.notification;

import java.util.HashMap;
import java.util.Map;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shibo Liu
 * Date: 07/03/19
 */
public class SpringNotificationFactory implements NotificationFactory {

    private static final Logger log = LoggerFactory.getLogger(
        SpringNotificationFactory.class);

    private JavaMailSenderImpl emailService;
    private Map<String, Emailer> emailerMap = new HashMap<String, Emailer>();

    @Override
    public void initialize(String host, Integer port, String username, String password) {
        emailService = new JavaMailSenderImpl();
        emailService.setProtocol("smtp");
        emailService.setHost(host);
        emailService.setPort(port);
        emailService.setUsername(username);
        emailService.setPassword(password);
        log.debug("initialize email service with Sprint email client connected to {}, Port: {}, User: {}.", host, port, username);
    }

    @Override
    public Emailer getEmailer(String fromAddress) {
        if (null == fromAddress ||
            !EmailValidator.getInstance().isValid(fromAddress)) {
            String msg = "fromAddress not valid notification: " + fromAddress;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (null == emailService) {
            String msg = "Emailer service !initialized.";
            log.error(msg);
            throw new DuraCloudRuntimeException(msg);
        }

        Emailer emailer = emailerMap.get(fromAddress);
        if (null == emailer) {
            emailer = new SpringEmailer(emailService, fromAddress);
            emailerMap.put(fromAddress, emailer);
        }

        return emailer;
    }

}
