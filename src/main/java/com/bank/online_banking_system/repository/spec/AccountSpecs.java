package com.bank.online_banking_system.repository.spec;

import com.bank.online_banking_system.entity.BankAccount;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class AccountSpecs {

    private AccountSpecs() {
    }

    /** Matches account number, UPI ID, or the holder's username. */
    public static Specification<BankAccount> matching(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<Object, Object> user = root.join("user");
            return cb.or(
                    cb.like(cb.lower(root.get("accountNumber")), like),
                    cb.like(cb.lower(root.get("upiId")), like),
                    cb.like(cb.lower(user.get("username")), like));
        };
    }
}
