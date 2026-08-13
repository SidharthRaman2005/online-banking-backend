package com.bank.online_banking_system.repository.spec;

import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.enums.AccountStatus;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecs {

    private UserSpecs() {
    }

    public static Specification<User> matching(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("email")), like));
    }

    public static Specification<User> withStatus(AccountStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
