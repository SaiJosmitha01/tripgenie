package com.tripgenie.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(UUID userId, String subject, String message) {
        log.info("Simulated email to userId={} subject=\"{}\" message=\"{}\"", userId, subject, message);
    }
}
