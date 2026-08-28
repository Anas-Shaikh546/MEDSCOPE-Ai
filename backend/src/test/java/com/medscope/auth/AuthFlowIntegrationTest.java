package com.medscope.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the Step 2.15 security testing checklist end to end against an
 * in-memory H2 database, exercising the real Spring Security filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "firstName", "Anas",
                "lastName", "Shaikh",
                "email", email,
                "password", password
        ));
    }

    @Test
    void test1_registerValidUser_returns201() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(registerBody("user1@example.com", "password123")))
                .andExpect(status().isCreated());
    }

    @Test
    void test2_registerSameEmail_returns409() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("dupe@example.com", "password123")));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(registerBody("dupe@example.com", "password123")))
                .andExpect(status().isConflict());
    }

    @Test
    void test3_registerInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(registerBody("not-an-email", "password123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test4_registerWeakPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(registerBody("weakpw@example.com", "123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test5_correctLogin_returnsJwt() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("login-ok@example.com", "password123")));

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "login-ok@example.com", "password", "password123"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void test6_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("wrongpw@example.com", "password123")));

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "wrongpw@example.com", "password", "totallyWrong"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test7_protectedEndpointWithoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test8_protectedEndpointWithInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test9_meWithValidJwt_returnsCurrentUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("me@example.com", "password123")));

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "me@example.com", "password", "password123"));

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    void test10_twoUsersCannotAccessEachOthersData() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("usera@example.com", "password123")));
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(registerBody("userb@example.com", "password123")));

        String tokenA = objectMapper.readTree(
                mockMvc.perform(post("/auth/login").contentType("application/json")
                                .content(objectMapper.writeValueAsString(
                                        Map.of("email", "usera@example.com", "password", "password123"))))
                        .andReturn().getResponse().getContentAsString()
        ).get("accessToken").asText();

        String tokenB = objectMapper.readTree(
                mockMvc.perform(post("/auth/login").contentType("application/json")
                                .content(objectMapper.writeValueAsString(
                                        Map.of("email", "userb@example.com", "password", "password123"))))
                        .andReturn().getResponse().getContentAsString()
        ).get("accessToken").asText();

        // /users/me is derived purely from each caller's own JWT via
        // @CurrentUser - there is no id parameter to tamper with, so each
        // token can only ever resolve to its own owner's profile.
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usera@example.com"));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("userb@example.com"));
    }
}
