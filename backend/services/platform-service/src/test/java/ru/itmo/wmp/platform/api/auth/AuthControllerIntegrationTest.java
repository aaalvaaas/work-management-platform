package ru.itmo.wmp.platform.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.platform.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.auth.RefreshTokenRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class AuthControllerIntegrationTest {
    private static final String PASSWORD = "Str0ngPassword!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldIssueTokensForValidCredentials() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        mockMvc.perform(loginRequest("465824", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        mockMvc.perform(loginRequest("465824", "wrong-password"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRejectUnknownIsuNumber() throws Exception {
        mockMvc.perform(loginRequest("465999", PASSWORD))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRejectBlockedAccount() throws Exception {
        userRepository.save(user("465824", UserStatus.BLOCKED));

        mockMvc.perform(loginRequest("465824", PASSWORD))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void shouldAcceptIssuedAccessTokenOnProtectedEndpoint() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        String accessToken = tokenField("465824", "accessToken");

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value("465824"));
    }

    @Test
    void shouldRotateRefreshTokenAndRejectTheOldOne() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        String refreshToken = tokenField("465824", "refreshToken");

        mockMvc.perform(refreshRequest(refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").exists());

        mockMvc.perform(refreshRequest(refreshToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldRejectRefreshTokenAfterLogout() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        String refreshToken = tokenField("465824", "refreshToken");

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken)))
        )
            .andExpect(status().isNoContent());

        mockMvc.perform(refreshRequest(refreshToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldValidateLoginRequest() throws Exception {
        mockMvc.perform(loginRequest("abc", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private String tokenField(String isuNumber, String field) throws Exception {
        String response = mockMvc.perform(loginRequest(isuNumber, PASSWORD))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response)
            .get(field)
            .asText();
    }

    private MockHttpServletRequestBuilder loginRequest(
        String isuNumber,
        String password
    ) throws Exception {
        return post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(isuNumber, password)));
    }

    private MockHttpServletRequestBuilder refreshRequest(String refreshToken) throws Exception {
        return post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken)));
    }

    private User user(String isuNumber, UserStatus status) {
        User user = new User();

        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setLastName("Иванов");
        user.setFirstName("Иван");
        user.setStatus(status);

        return user;
    }
}
