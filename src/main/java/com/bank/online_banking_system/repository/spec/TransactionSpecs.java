package com.bank.online_banking_system.repository.spec;

import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.enums.TransactionStatus;
import com.bank.online_banking_system.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Composable filters. Each returns null when its parameter is absent, and Spring Data drops null
 * specifications — which keeps the "optional filter" logic out of the service.
 */
public final class TransactionSpecs {

    private TransactionSpecs() {
    }

    public static Specification<Transaction> ownedBy(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> ofType(TransactionType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> withStatus(TransactionStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> createdFrom(Instant from) {
        return from == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    /** Exclusive upper bound; the service passes the start of the day after {@code to}. */
    public static Specification<Transaction> createdBefore(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    /** Free-text search over reference, description and both account numbers. */
    public static Specification<Transaction> matching(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("transactionReference")), like),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("senderAccountNumber"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("receiverAccountNumber"), "")), like));
    }
}
