package com.restaurant.saas.identity;

import com.restaurant.saas.identity.dto.AuthResponse;
import com.restaurant.saas.identity.dto.AuthUserResponse;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void createUser() {
        if (userRepository.existsByEmail("auth-test@example.com")) {
            return;
        }
        Role role = roleRepository.findSystemRoleWithPermissions("SUPER_ADMIN").orElseThrow();
        User user = new User();
        user.setEmail("auth-test@example.com");
        user.setFirstName("Auth");
        user.setLastName("Test");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("ACTIVE");
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Test
    void loginReturnsAccessTokenAndUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"auth-test@example.com","password":"Password123!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("auth-test@example.com"))
            .andReturn();

        AuthResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(response.user().roles()).contains("SUPER_ADMIN");
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"auth-test@example.com","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/superadmin/tenants"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSuperAdminCanListTenants() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/v1/superadmin/tenants")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        String token = loginToken();
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        AuthUserResponse user = objectMapper.readValue(
            result.getResponse().getContentAsString(), AuthUserResponse.class);
        assertThat(user.email()).isEqualTo("auth-test@example.com");
    }

    private String loginToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"auth-test@example.com","password":"Password123!"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readValue(
            result.getResponse().getContentAsString(), AuthResponse.class).accessToken();
    }
}
