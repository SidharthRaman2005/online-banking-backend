package com.bank.online_banking_system.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class LoanInfoResponse {
    private String overview;
    private List<String> types;
    private List<String> eligibility;
    private List<String> documents;
    private List<InterestRate> rates;

    @Data
    public static class InterestRate {
        private String loanType;
        private String interest;

        public InterestRate(String loanType, String interest) {
            this.loanType = loanType;
            this.interest = interest;
        }
    }
}
