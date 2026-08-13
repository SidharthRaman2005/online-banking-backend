package com.bank.online_banking_system;

import com.bank.online_banking_system.entity.BankAccount;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module 3: deposit and withdraw.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepositWithdrawTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String tokenFor(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"Secret123"}
                                """.formatted(username, email)))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Secret123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).at("/data/token").asText();
    }

    private BigDecimal balanceOf(String upiId) {
        return bankAccountRepository.findByUpiId(upiId).orElseThrow().getBalance();
    }

    @Test
    void depositIncreasesTheBalanceAndWritesALedgerRow() throws Exception {
        String token = tokenFor("depositor", "deposit@mail.com");
        BigDecimal before = balanceOf("depositor@obs");

        mockMvc.perform(post("/api/transactions/deposit").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10000.00,"description":"Salary"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.direction").value("CREDIT"))
                .andExpect(jsonPath("$.data.amount").value(10000.00))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.description").value("Salary"))
                .andExpect(jsonPath("$.data.transactionReference").value(
                        org.hamcrest.Matchers.matchesPattern("TXN\\d{10}")))
                .andExpect(jsonPath("$.data.balanceAfterTransaction")
                        .value(before.add(new BigDecimal("10000.00")).doubleValue()));

        assertThat(balanceOf("depositor@obs"))
                .isEqualByComparingTo(before.add(new BigDecimal("10000.00")));
    }

    @Test
    void withdrawDecreasesTheBalance() throws Exception {
        String token = tokenFor("withdrawer", "withdraw@mail.com");
        BigDecimal before = balanceOf("withdrawer@obs");

        mockMvc.perform(post("/api/transactions/withdraw").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":2500.50}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.description").value("Withdrawal"))
                .andExpect(jsonPath("$.data.senderAccountNumber").isString())
                .andExpect(jsonPath("$.data.receiverAccountNumber").doesNotExist());

        assertThat(balanceOf("withdrawer@obs"))
                .isEqualByComparingTo(before.subtract(new BigDecimal("2500.50")));
    }

    @Test
    void overdraftIsRejectedAndLeavesTheBalanceUntouched() throws Exception {
        String token = tokenFor("broke", "broke@mail.com");
        BigDecimal before = balanceOf("broke@obs");

        mockMvc.perform(post("/api/transactions/withdraw").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":99999999.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));

        assertThat(balanceOf("broke@obs")).isEqualByComparingTo(before);
    }

    @Test
    void zeroNegativeAndOverPreciseAmountsAreRejected() throws Exception {
        String token = tokenFor("validator", "validate@mail.com");
        BigDecimal before = balanceOf("validator@obs");

        for (String amount : new String[]{"0", "-500", "0.001"}) {
            mockMvc.perform(post("/api/transactions/deposit").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":" + amount + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.amount").exists());
        }

        mockMvc.perform(post("/api/transactions/deposit").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(balanceOf("validator@obs")).isEqualByComparingTo(before);
    }

    @Test
    void depositAndWithdrawShowUpOnTheDashboard() throws Exception {
        String token = tokenFor("dashflow", "dashflow@mail.com");

        mockMvc.perform(post("/api/transactions/deposit").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":5000.00}""")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions/withdraw").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":1200.00}""")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.data.totalExpense").value(1200.00))
                .andExpect(jsonPath("$.data.recentTransactions.length()").value(2))
                // Newest first.
                .andExpect(jsonPath("$.data.recentTransactions[0].type").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.recentTransactions[1].type").value("DEPOSIT"));
    }

    @Test
    void transactionsAreScopedToTheCallersOwnAccount() throws Exception {
        String tokenA = tokenFor("owner", "owner@mail.com");
        tokenFor("bystander", "bystander@mail.com");

        BigDecimal bystanderBefore = balanceOf("bystander@obs");

        mockMvc.perform(post("/api/transactions/deposit").header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":7000.00}""")).andExpect(status().isCreated());

        // The body carries no account identifier, so a deposit can only ever hit the token holder.
        assertThat(balanceOf("bystander@obs")).isEqualByComparingTo(bystanderBefore);
    }

    /**
     * Two simultaneous withdrawals of the entire balance. The pessimistic lock must serialise them
     * so exactly one succeeds — if both did, the account would go negative.
     */
    @Test
    void concurrentFullBalanceWithdrawalsCannotBothSucceed() throws Exception {
        String token = tokenFor("racer", "racer@mail.com");
        BankAccount account = bankAccountRepository.findByUpiId("racer@obs").orElseThrow();
        BigDecimal full = account.getBalance();

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        String body = "{\"amount\":" + full.toPlainString() + "}";

        Runnable attempt = () -> {
            try {
                start.await();
                int statusCode = mockMvc.perform(post("/api/transactions/withdraw")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn().getResponse().getStatus();
                if (statusCode == 201) {
                    created.incrementAndGet();
                }
            } catch (Exception ignored) {
                // A lock timeout or rollback counts as "did not succeed", which is what we assert.
            }
        };

        Thread one = new Thread(attempt);
        Thread two = new Thread(attempt);
        one.start();
        two.start();
        start.countDown();
        one.join(TimeUnit.SECONDS.toMillis(20));
        two.join(TimeUnit.SECONDS.toMillis(20));

        assertThat(created.get()).isEqualTo(1);
        assertThat(balanceOf("racer@obs")).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
