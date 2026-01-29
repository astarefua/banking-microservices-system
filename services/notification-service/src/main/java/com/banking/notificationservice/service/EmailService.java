package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendEmail(String recipient, String subject, String message) {
        // In a real application, this would use JavaMailSender to send actual emails
        // For now, we'll just log it
        log.info("===========================================");
        log.info("SENDING EMAIL");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("===========================================");

        // Simulate email sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendSMS(String phoneNumber, String message) {
        // In a real application, this would use Twilio or similar service
        log.info("===========================================");
        log.info("SENDING SMS");
        log.info("To: {}", phoneNumber);
        log.info("Message: {}", message);
        log.info("===========================================");

        // Simulate SMS sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}