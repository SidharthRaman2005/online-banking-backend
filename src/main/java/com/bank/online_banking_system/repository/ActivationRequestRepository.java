package com.bank.online_banking_system.repository;

import com.bank.online_banking_system.entity.ActivationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivationRequestRepository extends JpaRepository<ActivationRequest, Long> {
    List<ActivationRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
