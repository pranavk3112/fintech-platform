package com.fintech.platform.notification.service;

import com.fintech.platform.notification.event.TransferCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Async
    @EventListener
    public void handleTransferCompleted(TransferCompletedEvent event) {
        log.info("=== NOTIFICATION SERVICE ===");
        log.info("Processing notification for TransactionId: {}",
                event.getTransactionId());

        sendDebitNotification(event);
        sendCreditNotification(event);

        log.info("Notifications processed for TransactionId: {}",
                event.getTransactionId());
    }

    private void sendDebitNotification(TransferCompletedEvent event) {
        // In production: integrate with email provider (SendGrid, AWS SES)
        // or SMS provider (Twilio, AWS SNS)
        log.info("DEBIT NOTIFICATION → To: {} | Amount: ₹{} debited | Balance: ₹{}",
                event.getSenderEmail(),
                event.getAmount(),
                event.getBalanceAfter());
    }

    private void sendCreditNotification(TransferCompletedEvent event) {
        log.info("CREDIT NOTIFICATION → To: {} | Amount: ₹{} credited",
                event.getReceiverEmail(),
                event.getAmount());
    }
}