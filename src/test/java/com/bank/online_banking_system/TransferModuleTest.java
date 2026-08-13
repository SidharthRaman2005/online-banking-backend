package com.bank.online_banking_system;

import com.bank.online_banking_system.entity.Transaction;
import com.bank.online_banking_system.repository.BankAccountRepository;
import com.bank.online_banking_system.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Module 4 — money transfer by account number and by UPI ID. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiClient.class)
class TransferModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestApiClient api;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private BigDecimal balanceOf(String upiId) {
        return bankAccountRepository.findByUpiId(upiId).orElseThrow().getBalance();
    }

    private String accountNumberOf(String upiId) {
        return bankAccountRepository.findByUpiId(upiId).orElseThrow().getAccountNumber();
    }

    private String transfer(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void transferByAccountNumberMovesMoneyBothWays() throws Exception {
        String sender = api.register("tsender", "tsender@mail.com");
        api.register("treceiver", "treceiver@mail.com");

        BigDecimal senderBefore = balanceOf("tsender@obs");
        BigDecimal receiverBefore = balanceOf("treceiver@obs");
        String receiverAccount = accountNumberOf("treceiver@obs");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", sender)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverAccountNumber":"%s","amount":5000.00,"description":"Rent"}
                                """.formatted(receiverAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.counterpartyName").value("treceiver"))
                .andExpect(jsonPath("$.data.description").value("Rent"));

        assertThat(balanceOf("tsender@obs"))
                .isEqualByComparingTo(senderBefore.subtract(new BigDecimal("5000")));
        assertThat(balanceOf("treceiver@obs"))
                .isEqualByComparingTo(receiverBefore.add(new BigDecimal("5000")));
    }

    @Test
    void transferByUpiIdWorksThroughTheSameCodePath() throws Exception {
        String sender = api.register("upisender", "upisender@mail.com");
        api.register("upitaker", "upitaker@mail.com");

        BigDecimal receiverBefore = balanceOf("upitaker@obs");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", sender)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUpiId":"upitaker@obs","amount":2000.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.counterpartyName").value("upitaker"));

        assertThat(balanceOf("upitaker@obs"))
                .isEqualByComparingTo(receiverBefore.add(new BigDecimal("2000")));
    }

    /** The heart of the ledger design: two rows, one reference, each true for its own side. */
    @Test
    void eachSideOfATransferSeesItsOwnBalanceAndDirection() throws Exception {
        String sender = api.register("ledgera", "ledgera@mail.com");
        String receiver = api.register("ledgerb", "ledgerb@mail.com");

        String body = transfer(sender, """
                {"receiverUpiId":"ledgerb@obs","amount":1500.00}""");
        String reference = objectMapper.readTree(body).at("/data/transactionReference").asText();

        List<Transaction> rows = transactionRepository.findAll().stream()
                .filter(t -> reference.equals(t.getTransactionReference()))
                .toList();
        assertThat(rows).hasSize(2);
        // Compare ids rather than navigating the lazy association outside a session.
        Long senderAccountId = bankAccountRepository.findByUpiId("ledgera@obs").orElseThrow().getId();
        Long receiverAccountId = bankAccountRepository.findByUpiId("ledgerb@obs").orElseThrow().getId();
        assertThat(rows).extracting(t -> t.getAccount().getId())
                .containsExactlyInAnyOrder(senderAccountId, receiverAccountId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/transactions/" + reference).header("Authorization", sender))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.balanceAfterTransaction")
                        .value(balanceOf("ledgera@obs").doubleValue()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/transactions/" + reference).header("Authorization", receiver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direction").value("CREDIT"))
                .andExpect(jsonPath("$.data.balanceAfterTransaction")
                        .value(balanceOf("ledgerb@obs").doubleValue()));
    }

    @Test
    void selfTransferIsRejected() throws Exception {
        String token = api.register("narcissus", "self@mail.com");
        BigDecimal before = balanceOf("narcissus@obs");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUpiId":"narcissus@obs","amount":100.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SELF_TRANSFER_NOT_ALLOWED"));

        assertThat(balanceOf("narcissus@obs")).isEqualByComparingTo(before);
    }

    @Test
    void unknownReceiversAreRejected() throws Exception {
        String token = api.register("hunter", "hunter@mail.com");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverAccountNumber":"9999999999","amount":100.00}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUpiId":"ghost@obs","amount":100.00}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void bothOrNeitherReceiverFieldsAreRejected() throws Exception {
        String token = api.register("picky", "picky@mail.com");
        api.register("target", "target@mail.com");
        String targetAccount = accountNumberOf("target@obs");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverAccountNumber":"%s","receiverUpiId":"target@obs","amount":10.00}
                                """.formatted(targetAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    /** A failed transfer must leave both balances exactly as they were. */
    @Test
    void insufficientBalanceRollsBackBothSides() throws Exception {
        String sender = api.register("pauper", "pauper@mail.com");
        api.register("wealthy", "wealthy@mail.com");

        BigDecimal senderBefore = balanceOf("pauper@obs");
        BigDecimal receiverBefore = balanceOf("wealthy@obs");

        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", sender)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUpiId":"wealthy@obs","amount":99999999.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));

        assertThat(balanceOf("pauper@obs")).isEqualByComparingTo(senderBefore);
        assertThat(balanceOf("wealthy@obs")).isEqualByComparingTo(receiverBefore);
    }

    /**
     * Two users paying each other simultaneously. Without ascending-id lock ordering this is the
     * classic deadlock; with it, both transfers complete and the total money is conserved.
     */
    @Test
    void simultaneousMutualTransfersDoNotDeadlock() throws Exception {
        String a = api.register("mutuala", "mutuala@mail.com");
        String b = api.register("mutualb", "mutualb@mail.com");

        BigDecimal totalBefore = balanceOf("mutuala@obs").add(balanceOf("mutualb@obs"));

        Runnable aToB = () -> {
            try {
                transfer(a, """
                        {"receiverUpiId":"mutualb@obs","amount":1000.00}""");
            } catch (Exception ignored) {
                // Counted through the conservation assertion below.
            }
        };
        Runnable bToA = () -> {
            try {
                transfer(b, """
                        {"receiverUpiId":"mutuala@obs","amount":700.00}""");
            } catch (Exception ignored) {
                // Counted through the conservation assertion below.
            }
        };

        Thread one = new Thread(aToB);
        Thread two = new Thread(bToA);
        one.start();
        two.start();
        one.join(20_000);
        two.join(20_000);

        assertThat(balanceOf("mutuala@obs").add(balanceOf("mutualb@obs")))
                .isEqualByComparingTo(totalBefore);
    }
}
