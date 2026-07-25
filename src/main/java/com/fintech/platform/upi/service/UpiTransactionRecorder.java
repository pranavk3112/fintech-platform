package com.fintech.platform.upi.service;

import com.fintech.platform.upi.entity.UpiTransaction;
import com.fintech.platform.upi.repository.UpiTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpiTransactionRecorder {

    private final UpiTransactionRepository upiTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpiTransaction save(UpiTransaction transaction) {
        UpiTransaction saved = upiTransactionRepository.save(transaction);
        log.info("UPI transaction recorded. Id: {} Status: {}",
                saved.getId(), saved.getStatus());
        return saved;
    }
}