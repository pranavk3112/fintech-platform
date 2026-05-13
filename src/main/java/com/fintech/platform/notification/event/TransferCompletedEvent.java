package com.fintech.platform.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class TransferCompletedEvent extends ApplicationEvent {

    private final Long transactionId;
    private final String senderEmail;
    private final String receiverEmail;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime occurredAt;

    public TransferCompletedEvent(Object source,
                                  Long transactionId,
                                  String senderEmail,
                                  String receiverEmail,
                                  BigDecimal amount,
                                  BigDecimal balanceAfter) {
        super(source);
        this.transactionId = transactionId;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.occurredAt = LocalDateTime.now();
    }
}