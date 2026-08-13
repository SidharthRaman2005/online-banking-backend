package com.bank.online_banking_system;

import com.bank.online_banking_system.repository.BankAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module 2 acceptance criteria: dashboard, profile read/update, change password.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    /** Registers a user and returns their bearer token. */
    private String tokenFor(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"%s"}
                                """.formatted(username, email, password)))
                .andExpect(status().isCreated());
        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).at("/data/token").asText();
    }

    @Test
    void dashboardReturnsTheTokenHoldersOwnAccount() throws Exception {
        String token = tokenFor("dashuser", "dash@mail.com", "Secret123");
        var account = bankAccountRepository.findByUpiId("dashuser@obs").orElseThrow();

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("dashuser"))
                .andExpect(jsonPath("$.data.accountNumber").value(account.getAccountNumber()))
                .andExpect(jsonPath("$.data.upiId").value("dashuser@obs"))
                .andExpect(jsonPath("$.data.balance").exists())
                .andExpect(jsonPath("$.data.totalIncome").value(0))
                .andExpect(jsonPath("$.data.totalExpense").value(0))
                .andExpect(jsonPath("$.data.recentTransactions").isEmpty());
    }

    @Test
    void profileNeverExposesThePasswordHash() throws Exception {
        String token = tokenFor("profuser", "prof@mail.com", "Secret123");

        String body = mockMvc.perform(get("/api/user/profile").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    void updatingTheUsernameLeavesTheUpiIdUnchanged() throws Exception {
        String token = tokenFor("oldname", "rename@mail.com", "Secret123");

        mockMvc.perform(put("/api/user/profile").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newname"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newname"))
                .andExpect(jsonPath("$.data.upiId").value("oldname@obs"));

        assertThat(bankAccountRepository.findByUpiId("newname@obs")).isEmpty();
    }

    @Test
    void changingTheEmailKeepsTheExistingTokenValid() throws Exception {
        String token = tokenFor("emailuser", "before@mail.com", "Secret123");

        mockMvc.perform(put("/api/user/profile").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"AFTER@Mail.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("after@mail.com"));

        // The JWT subject is the user id, so the token issued before the change still resolves.
        mockMvc.perform(get("/api/user/dashboard").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("after@mail.com"));
    }

    @Test
    void profileUpdateRejectsDuplicatesAndEmptyPayloads() throws Exception {
        tokenFor("takenuser", "taken@mail.com", "Secret123");
        String token = tokenFor("updater", "updater@mail.com", "Secret123");

        mockMvc.perform(put("/api/user/profile").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"taken@mail.com"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

        mockMvc.perform(put("/api/user/profile").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void changePasswordRequiresTheCorrectCurrentPassword() throws Exception {
        String token = tokenFor("pwuser", "pw@mail.com", "Secret123");

        mockMvc.perform(put("/api/user/change-password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongOne123","newPassword":"Brand2New"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        mockMvc.perform(put("/api/user/change-password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Secret123","newPassword":"Secret123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));

        mockMvc.perform(put("/api/user/change-password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Secret123","newPassword":"Brand2New"}"""))
                .andExpect(status().isOk());

        // Old password no longer works, new one does.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pw@mail.com","password":"Secret123"}"""))
                .andExpect(status().isUnauthorized());
        login("pw@mail.com", "Brand2New");
    }

    @Test
    void oneUserCannotSeeAnotherUsersDashboard() throws Exception {
        String tokenA = tokenFor("alpha", "alpha@mail.com", "Secret123");
        tokenFor("beta", "beta@mail.com", "Secret123");

        var betaAccount = bankAccountRepository.findByUpiId("beta@obs").orElseThrow();

        String body = mockMvc.perform(get("/api/user/dashboard").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).get("data");
        assertThat(data.get("accountNumber").asText()).isNotEqualTo(betaAccount.getAccountNumber());
        assertThat(data.get("username").asText()).isEqualTo("alpha");
    }
}
