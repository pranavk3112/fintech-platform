package com.fintech.platform.transaction.service;

import com.fintech.platform.transaction.entity.Transaction;
import com.fintech.platform.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRecorder {

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction savePendingTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);
        log.info("Pending transaction recorded. Id: {}", saved.getId());
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransaction(Long transactionId, String reason) {
        transactionRepository.findById(transactionId).ifPresent(t -> {
            t.setStatus(Transaction.TransactionStatus.FAILED);
            t.setFailureReason(reason);
            t.setBalanceAfterSource(t.getBalanceBeforeSource());
            transactionRepository.save(t);
            log.info("Failed transaction recorded. Id: {}", transactionId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCompletedTransaction(Long transactionId,
                                         java.math.BigDecimal balanceAfter) {
        transactionRepository.findById(transactionId).ifPresent(t -> {
            t.setStatus(Transaction.TransactionStatus.COMPLETED);
            t.setBalanceAfterSource(balanceAfter);
            transactionRepository.save(t);
            log.info("Completed transaction recorded. Id: {}", transactionId);
        });
    }
}