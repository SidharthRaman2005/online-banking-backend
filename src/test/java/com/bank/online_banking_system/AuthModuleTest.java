package com.bank.online_banking_system;

import com.bank.online_banking_system.entity.BankAccount;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the Module 1 acceptance criteria and the failure cases from the test matrix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String registerBody(String username, String email, String password) {
        return """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(username, email, password);
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    void registerCreatesUserWithAccountNumberUpiIdAndBalance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("sidharth", "sid@mail.com", "Secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").isString())
                .andExpect(jsonPath("$.data.upiId").value("sidharth@obs"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertThat(data.get("accountNumber").asText()).matches("[1-9]\\d{9}");
        assertThat(new BigDecimal(data.get("balance").asText()))
                .isBetween(new BigDecimal("100000"), new BigDecimal("9900000"));
        assertThat(data.has("password")).isFalse();
    }

    @Test
    void duplicateEmailAndUsernameAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("dupuser", "dup@mail.com", "Secret123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("dupuser2", "dup@mail.com", "Secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("dupuser", "other@mail.com", "Secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void weakPasswordReturnsFieldLevelValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("weakuser", "weak@mail.com", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsTokenAndDoesNotMutateTheAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("rahul", "rahul@mail.com", "Secret123")))
                .andExpect(status().isCreated());

        BankAccount before = bankAccountRepository.findByUpiId("rahul@obs").orElseThrow();
        String accountNumber = before.getAccountNumber();
        BigDecimal balance = before.getBalance();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("rahul@mail.com", "Secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));

        BankAccount after = bankAccountRepository.findByUpiId("rahul@obs").orElseThrow();
        assertThat(after.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(after.getBalance()).isEqualByComparingTo(balance);
    }

    @Test
    void unknownEmailAndWrongPasswordReturnTheSameMessage() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("probe", "probe@mail.com", "Secret123")))
                .andExpect(status().isCreated());

        String unknown = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@mail.com", "Secret123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("probe@mail.com", "WrongPass123")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(unknown).get("message").asText())
                .isEqualTo(objectMapper.readTree(wrongPassword).get("message").asText());
    }

    @Test
    void protectedEndpointRejectsMissingAndTamperedTokens() throws Exception {
        mockMvc.perform(get("/api/user/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upiIdCollisionGetsANumericSuffix() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("clash", "clash1@mail.com", "Secret123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Clash", "clash2@mail.com", "Secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.upiId").value(org.hamcrest.Matchers.matchesPattern("clash\\d{3}@obs")));
    }
}
