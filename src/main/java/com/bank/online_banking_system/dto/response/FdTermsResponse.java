package com.bank.online_banking_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FdTermsResponse {
    private List<TermRate> terms;

    @Data
    @AllArgsConstructor
    public static class TermRate {
        private int months;
        private String interest; // human readable
        private double rate; // numeric
    }
}
