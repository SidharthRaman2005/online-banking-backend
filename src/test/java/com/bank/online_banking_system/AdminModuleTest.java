package com.bank.online_banking_system;

import com.bank.online_banking_system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Module 7 — admin dashboard, user management, monitoring, and role enforcement. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiClient.class)
class AdminModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestApiClient api;

    @Autowired
    private UserRepository userRepository;

    private String adminToken() throws Exception {
        return api.login("admin@obs.com", "Admin@123");
    }

    @Test
    void theSeededAdminCanLogInAndReadStats() throws Exception {
        String admin = adminToken();

        mockMvc.perform(get("/api/admin/stats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalAccounts").isNumber())
                .andExpect(jsonPath("$.data.totalTransactions").isNumber())
                .andExpect(jsonPath("$.data.totalMoneyInSystem").isNumber());
    }

    @Test
    void everyAdminRouteIsForbiddenForANormalUser() throws Exception {
        String user = api.register("peasant", "peasant@mail.com");

        for (String path : new String[]{"/api/admin/stats", "/api/admin/users",
                "/api/admin/accounts", "/api/admin/transactions"}) {
            mockMvc.perform(get(path).header("Authorization", user))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }

        mockMvc.perform(put("/api/admin/users/1/status").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DEACTIVATED"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoutesRejectAnonymousCallersWith401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void usersAndAccountsAreSearchableAndNeverExposePasswords() throws Exception {
        api.register("searchme", "searchme@mail.com");
        String admin = adminToken();

        String body = mockMvc.perform(get("/api/admin/users?search=searchme").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("searchme"))
                .andExpect(jsonPath("$.data.content[0].accountNumber").isString())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("$2a$");

        mockMvc.perform(get("/api/admin/accounts?search=searchme@obs").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].holderName").value("searchme"));
    }

    @Test
    void deactivatingAUserBlocksLoginAndFreezesTheirAccount() throws Exception {
        api.register("condemned", "condemned@mail.com");
        String admin = adminToken();
        Long id = userRepository.findByEmail("condemned@mail.com").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DEACTIVATED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"condemned@mail.com","password":"Secret123"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DEACTIVATED"));

        // Reversible from the same screen.
        mockMvc.perform(put("/api/admin/users/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isOk());
        api.login("condemned@mail.com", "Secret123");
    }

    @Test
    void anAdminCannotDeactivateThemselves() throws Exception {
        String admin = adminToken();
        Long adminId = userRepository.findByEmail("admin@obs.com").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + adminId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DEACTIVATED"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));
    }

    @Test
    void transactionMonitoringFiltersByTypeAndStatus() throws Exception {
        String user = api.register("monitored", "monitored@mail.com");
        api.deposit(user, "4200");
        String admin = adminToken();

        mockMvc.perform(get("/api/admin/transactions?type=DEPOSIT&status=SUCCESS")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.content[0].ledgerAccountHolder").isString());

        mockMvc.perform(get("/api/admin/transactions?status=BOGUS").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }
}
