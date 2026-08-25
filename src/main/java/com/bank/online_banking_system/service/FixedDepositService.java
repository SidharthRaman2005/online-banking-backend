package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.request.CreateFdRequest;
import com.bank.online_banking_system.dto.response.FdResponse;
import com.bank.online_banking_system.dto.response.FdTermsResponse;
import com.bank.online_banking_system.entity.FixedDeposit;
import com.bank.online_banking_system.entity.User;
import com.bank.online_banking_system.repository.FixedDepositRepository;
import com.bank.online_banking_system.repository.UserRepository;
import com.bank.online_banking_system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FixedDepositService {

    private final FixedDepositRepository fixedDepositRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public FdTermsResponse getTerms() {
        List<FdTermsResponse.TermRate> terms = List.of(
                new FdTermsResponse.TermRate(6, "5.50% p.a.", 5.5),
                new FdTermsResponse.TermRate(12, "6.00% p.a.", 6.0),
                new FdTermsResponse.TermRate(24, "6.50% p.a.", 6.5),
                new FdTermsResponse.TermRate(36, "7.00% p.a.", 7.0)
        );
        return new FdTermsResponse(terms);
    }

    @Transactional
    public FdResponse createFd(CreateFdRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        // Withdraw from the user's primary account using TransactionService
        com.bank.online_banking_system.dto.request.AmountRequest amtReq = new com.bank.online_banking_system.dto.request.AmountRequest();
        amtReq.setAmount(request.getAmount());
        amtReq.setDescription("Fixed deposit placement");
        transactionService.withdraw(userId, amtReq);

        // Build FD record
        FixedDeposit fd = FixedDeposit.builder()
                .user(user)
                .account(user.getBankAccount())
                .amount(request.getAmount())
                .termMonths(request.getTermMonths())
                .interestRate(getRateForTerm(request.getTermMonths()))
                .startAt(LocalDateTime.now())
                .maturityAt(LocalDateTime.now().plusMonths(request.getTermMonths()))
                .status("ACTIVE")
                .build();

        FixedDeposit saved = fixedDepositRepository.save(fd);

        FdResponse resp = new FdResponse();
        resp.setId(saved.getId());
        resp.setAmount(saved.getAmount());
        resp.setTermMonths(saved.getTermMonths());
        resp.setInterestRate(saved.getInterestRate());
        resp.setStartAt(saved.getStartAt());
        resp.setMaturityAt(saved.getMaturityAt());
        resp.setStatus(saved.getStatus());
        // compute maturity amount: amount * (1 + rate% * months/12)
        BigDecimal maturity = saved.getAmount().multiply(
            BigDecimal.valueOf(1).add(
                BigDecimal.valueOf(saved.getInterestRate()).multiply(BigDecimal.valueOf(saved.getTermMonths()))
                    .divide(BigDecimal.valueOf(1200), 6, java.math.RoundingMode.HALF_UP)
            )
        ).setScale(2, java.math.RoundingMode.HALF_UP);
        resp.setMaturityAmount(maturity);
        // Notify the user about the FD creation
        try {
            notificationService.notify(user, "₹" + request.getAmount() + " has been transferred from your savings to a Fixed Deposit.");
        } catch (Exception ignored) {
        }

        return resp;
    }

    @Transactional(readOnly = true)
    public java.util.List<FdResponse> listFds() {
        Long userId = SecurityUtils.getCurrentUserId();
        java.util.List<FixedDeposit> list = fixedDepositRepository.findByUserIdOrderByStartAtDesc(userId);
        return list.stream().map(fd -> {
            FdResponse r = new FdResponse();
            r.setId(fd.getId());
            r.setAmount(fd.getAmount());
            r.setTermMonths(fd.getTermMonths());
            r.setInterestRate(fd.getInterestRate());
            r.setStartAt(fd.getStartAt());
            r.setMaturityAt(fd.getMaturityAt());
            r.setStatus(fd.getStatus());
            BigDecimal maturity = fd.getAmount().multiply(
                    BigDecimal.valueOf(1).add(
                            BigDecimal.valueOf(fd.getInterestRate()).multiply(BigDecimal.valueOf(fd.getTermMonths()))
                                    .divide(BigDecimal.valueOf(1200), 6, java.math.RoundingMode.HALF_UP)
                    )
            ).setScale(2, java.math.RoundingMode.HALF_UP);
            r.setMaturityAmount(maturity);
            return r;
        }).toList();
    }

    private double getRateForTerm(int months) {
        return switch (months) {
            case 6 -> 5.5;
            case 12 -> 6.0;
            case 24 -> 6.5;
            case 36 -> 7.0;
            default -> 6.0;
        };
    }
}
