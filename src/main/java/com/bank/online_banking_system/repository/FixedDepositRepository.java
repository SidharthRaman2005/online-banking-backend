package com.bank.online_banking_system.repository;

import com.bank.online_banking_system.entity.FixedDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
	java.util.List<FixedDeposit> findByUserIdOrderByStartAtDesc(Long userId);
}
