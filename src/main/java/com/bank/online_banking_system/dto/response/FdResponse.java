package com.bank.online_banking_system.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class FdResponse {
    private Long id;
    private BigDecimal amount;
    private int termMonths;
    private double interestRate;
    private LocalDateTime startAt;
    private LocalDateTime maturityAt;
    private String status;
    private BigDecimal maturityAmount;
}
