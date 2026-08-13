package com.bank.online_banking_system;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modules 5, 6 and 8 — history and filtering, simulated UPI, notifications. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiClient.class)
class HistoryUpiNotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestApiClient api;

    @Test
    void historyIsPaginatedAndNewestFirst() throws Exception {
        String token = api.register("histuser", "hist@mail.com");
        for (int i = 1; i <= 12; i++) {
            api.deposit(token, String.valueOf(i * 100));
        }

        mockMvc.perform(get("/api/transactions?page=0&size=5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content[0].amount").value(1200.00));

        mockMvc.perform(get("/api/transactions?page=2&size=5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void historyFiltersByTypeAndDateAndSearch() throws Exception {
        String token = api.register("filteruser", "filter@mail.com");
        api.deposit(token, "1000");
        mockMvc.perform(post("/api/transactions/withdraw").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":250.00,"description":"Groceries"}""")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions?type=DEPOSIT").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("DEPOSIT"));

        mockMvc.perform(get("/api/transactions?type=WITHDRAW").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/transactions?type=TRANSFER").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/transactions?search=groceries").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // "to" is inclusive, so today's rows must be inside a today..today window.
        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/transactions?from=" + today + "&to=" + today)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/transactions?type=NONSENSE").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));
    }

    @Test
    void oneUserCannotFetchAnotherUsersTransactionByReference() throws Exception {
        String owner = api.register("refowner", "refowner@mail.com");
        String stranger = api.register("stranger", "stranger@mail.com");

        String body = mockMvc.perform(post("/api/transactions/deposit").header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":900.00}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reference = objectMapper.readTree(body).at("/data/transactionReference").asText();

        mockMvc.perform(get("/api/transactions/" + reference).header("Authorization", owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/" + reference).header("Authorization", stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void upiResolveReturnsTheHolderNameOnly() throws Exception {
        String token = api.register("payer", "payer@mail.com");
        api.register("payee", "payee@mail.com");

        String body = mockMvc.perform(get("/api/upi/resolve?upiId=payee@obs").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountHolderName").value("payee"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andReturn().getResponse().getContentAsString();

        // Confirming a payee must not leak their account number or balance.
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("accountNumber").doesNotContain("balance");

        mockMvc.perform(get("/api/upi/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upiId").value("payer@obs"));

        mockMvc.perform(get("/api/upi/resolve?upiId=not-a-upi-id").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));

        mockMvc.perform(get("/api/upi/resolve?upiId=missing@obs").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void bankingOperationsCreateNotificationsForBothParties() throws Exception {
        String sender = api.register("notifya", "notifya@mail.com");
        String receiver = api.register("notifyb", "notifyb@mail.com");

        api.deposit(sender, "3000");
        mockMvc.perform(post("/api/transactions/transfer").header("Authorization", sender)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"receiverUpiId":"notifyb@obs","amount":500.00}""")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").header("Authorization", sender))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].message").value(
                        org.hamcrest.Matchers.containsString("transferred successfully")))
                .andExpect(jsonPath("$.data[0].read").value(false));

        String receiverBody = mockMvc.perform(get("/api/notifications").header("Authorization", receiver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].message").value(
                        org.hamcrest.Matchers.containsString("received from notifya")))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(receiverBody).at("/data/0/id").asLong();

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", receiver))
                .andExpect(jsonPath("$.data.unread").value(1));

        mockMvc.perform(put("/api/notifications/" + id + "/read").header("Authorization", receiver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        // Another user cannot touch it.
        mockMvc.perform(delete("/api/notifications/" + id).header("Authorization", sender))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/notifications/" + id).header("Authorization", receiver))
                .andExpect(status().isOk());
    }
}
