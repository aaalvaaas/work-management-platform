package ru.itmo.wmp.platform.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.platform.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class AuthControllerIntegrationTest {
    private static final String PASSWORD = "Str0ngPassword!";
    private static final String REFRESH_COOKIE = "wmp_refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldIssueAccessTokenAndRefreshCookieForValidCredentials() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        MvcResult result = mockMvc.perform(loginRequest("465824", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn();

        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);

        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
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

        String accessToken = accessToken("465824");

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value("465824"));
    }

    @Test
    void shouldRotateRefreshCookieAndRejectTheOldOne() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        Cookie issued = refreshCookie("465824");

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(issued))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn();

        Cookie rotated = refreshed.getResponse().getCookie(REFRESH_COOKIE);

        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(issued.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(issued))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldRejectRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldClearCookieAndRejectBothTokensAfterLogout() throws Exception {
        userRepository.save(user("465824", UserStatus.ACTIVE));

        MvcResult login = mockMvc.perform(loginRequest("465824", PASSWORD))
            .andExpect(status().isOk())
            .andReturn();

        Cookie issued = login.getResponse().getCookie(REFRESH_COOKIE);
        String accessToken = objectMapper
            .readTree(login.getResponse().getContentAsString())
            .get("accessToken")
            .asText();

        MvcResult logout = mockMvc.perform(
            post("/api/v1/auth/logout")
                .cookie(issued)
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(logout.getResponse().getCookie(REFRESH_COOKIE).getMaxAge()).isZero();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(issued))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldValidateLoginRequest() throws Exception {
        mockMvc.perform(loginRequest("abc", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private Cookie refreshCookie(String isuNumber) throws Exception {
        return mockMvc.perform(loginRequest(isuNumber, PASSWORD))
            .andReturn()
            .getResponse()
            .getCookie(REFRESH_COOKIE);
    }

    private String accessToken(String isuNumber) throws Exception {
        String response = mockMvc.perform(loginRequest(isuNumber, PASSWORD))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response)
            .get("accessToken")
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
