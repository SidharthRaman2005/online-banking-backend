package com.bank.online_banking_system.service;

import com.bank.online_banking_system.dto.response.LoanInfoResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class LoanService {

    public LoanInfoResponse getLoanInfo() {
        LoanInfoResponse info = new LoanInfoResponse();
        info.setOverview("Our loans are tailored to help customers meet life goals — home, car, personal needs, and business growth. Competitive rates and flexible tenures.");

        info.setTypes(Arrays.asList("Home Loan", "Personal Loan", "Auto Loan", "Education Loan", "Business Loan"));

        info.setEligibility(Arrays.asList(
                "Must be 21 years or older",
                "Minimum monthly income as per loan type",
                "Valid government ID and proof of address",
                "No active major defaults with credit bureaus"
        ));

        info.setDocuments(Arrays.asList(
                "Identity proof (Passport / Aadhar / PAN)",
                "Address proof (Utility bill / Rental agreement)",
                "Income proof (Salary slips / ITR)",
                "Bank statements for last 6 months"
        ));

        LoanInfoResponse.InterestRate r1 = new LoanInfoResponse.InterestRate("Home Loan", "7.05% - 8.50% p.a.");
        LoanInfoResponse.InterestRate r2 = new LoanInfoResponse.InterestRate("Personal Loan", "10.99% - 19.99% p.a.");
        LoanInfoResponse.InterestRate r3 = new LoanInfoResponse.InterestRate("Auto Loan", "8.50% - 12.00% p.a.");
        LoanInfoResponse.InterestRate r4 = new LoanInfoResponse.InterestRate("Education Loan", "6.50% - 12.00% p.a.");
        LoanInfoResponse.InterestRate r5 = new LoanInfoResponse.InterestRate("Business Loan", "9.50% - 15.00% p.a.");

        info.setRates(Arrays.asList(r1, r2, r3, r4, r5));

        return info;
    }
}
