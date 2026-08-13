package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.TransferRequest;
import com.bank.online_banking_system.dto.response.TransactionResponse;
import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.AccountStatus;
import com.bank.online_banking_system.enums.TransactionType;
import com.bank.online_banking_system.exception.AccountInactiveException;
import com.bank.online_banking_system.exception.AccountNotFoundException;
import com.bank.online_banking_system.exception.InsufficientBalanceException;
import com.bank.online_banking_system.exception.SelfTransferException;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Internal money transfer, by account number or by UPI ID. Both routes share this one code path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final ReferenceGenerator referenceGenerator;
    private final NotificationService notificationService;

    /**
     * Atomic by construction: the debit, the credit and both ledger rows are written inside one
     * database transaction. If anything throws, the whole thing rolls back and neither balance moves.
     */
    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse transfer(Long userId, TransferRequest request) {
        BigDecimal amount = TransactionService.normalise(request.getAmount());

        Long senderId = accountService.requireAccountOf(userId).getId();
        Long receiverId = resolveReceiver(request).getId();

        if (senderId.equals(receiverId)) {
            throw new SelfTransferException("You cannot transfer money to your own account");
        }

        // Lock both rows in ascending id order. Two users paying each other at the same instant
        // would otherwise each hold the lock the other needs, and the database would deadlock.
        Long firstId = Math.min(senderId, receiverId);
        Long secondId = Math.max(senderId, receiverId);
        BankAccount first = lock(firstId);
        BankAccount second = lock(secondId);

        BankAccount sender = senderId.equals(firstId) ? first : second;
        BankAccount receiver = receiverId.equals(firstId) ? first : second;

        requireActive(sender, "Your account is not active");
        requireActive(receiver, "The receiving account is not active");

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for this transfer");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        bankAccountRepository.save(sender);
        bankAccountRepository.save(receiver);

        String reference = referenceGenerator.generate();
        String description = TransactionService.defaultDescription(request.getDescription(), "Transfer");

        // Two rows, one reference: each side records the balance of its own account.
        Transaction senderRow = transactionService.record(sender, TransactionType.TRANSFER, amount,
                sender.getAccountNumber(), receiver.getAccountNumber(), description, reference);
        transactionService.record(receiver, TransactionType.TRANSFER, amount,
                sender.getAccountNumber(), receiver.getAccountNumber(), description, reference);

        notificationService.notify(sender.getUser(), "%s transferred successfully to %s."
                .formatted(TransactionService.money(amount), receiver.getUser().getUsername()));
        notificationService.notify(receiver.getUser(), "%s received from %s."
                .formatted(TransactionService.money(amount), sender.getUser().getUsername()));

        log.info("Transfer {} of {} from {} to {}", reference, amount,
                sender.getAccountNumber(), receiver.getAccountNumber());

        return transactionMapper.toResponse(senderRow, sender);
    }

    private BankAccount resolveReceiver(TransferRequest request) {
        if (request.getReceiverAccountNumber() != null && !request.getReceiverAccountNumber().isBlank()) {
            return accountService.requireByAccountNumber(request.getReceiverAccountNumber().trim());
        }
        return accountService.requireByUpiId(request.getReceiverUpiId().trim());
    }

    private BankAccount lock(Long accountId) {
        return bankAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private void requireActive(BankAccount account, String message) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(message);
        }
    }
}
