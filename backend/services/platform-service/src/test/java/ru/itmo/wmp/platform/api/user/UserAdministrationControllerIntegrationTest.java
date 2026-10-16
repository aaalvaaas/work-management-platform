package ru.itmo.wmp.platform.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.platform.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.auth.RefreshTokenRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class UserAdministrationControllerIntegrationTest {
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
    void shouldForbidBlockingForRegularUser() throws Exception {
        User actor = userRepository.save(account("465824"));
        User target = userRepository.save(account("465825"));

        mockMvc.perform(
            post("/api/v1/users/{id}/block", target.getId())
                .with(jwt().jwt(token -> token.subject(String.valueOf(actor.getId()))))
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockAndUnblockAccount() throws Exception {
        User admin = userRepository.save(account("465900"));
        User target = userRepository.save(account("465824"));

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", target.getId()), admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/unblock", target.getId()), admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectBlockingOwnAccount() throws Exception {
        User admin = userRepository.save(account("465900"));

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", admin.getId()), admin))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("SELF_MANAGEMENT_NOT_ALLOWED"));
    }

    @Test
    void shouldRevokeRefreshTokensWhenAccountIsBlocked() throws Exception {
        User admin = userRepository.save(account("465900"));
        User target = userRepository.save(account("465824"));

        String response = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest(target.getIsuNumber(), PASSWORD)
                ))
        )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String refreshToken = objectMapper.readTree(response)
            .get("refreshToken")
            .asText();

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", target.getId()), admin))
            .andExpect(status().isOk());

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken)))
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectIssuedAccessTokenWhenAccountIsBlocked() throws Exception {
        User admin = userRepository.save(account("465900"));
        User target = userRepository.save(account("465824"));

        String response = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest(target.getIsuNumber(), PASSWORD)
                ))
        )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(response)
            .get("accessToken")
            .asText();

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isOk());

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", target.getId()), admin))
            .andExpect(status().isOk());

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepOrganizationMembershipWhenAccountIsBlocked() throws Exception {
        User admin = userRepository.save(account("465900"));
        User target = userRepository.save(account("465824"));

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", target.getId()), admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value("465824"))
            .andExpect(jsonPath("$.email").value("465824@niuitmo.ru"));
    }

    @Test
    void shouldGrantAndRevokeGlobalRole() throws Exception {
        User admin = userRepository.save(account("465900"));
        User target = userRepository.save(account("465824"));

        mockMvc.perform(
            asAdmin(put("/api/v1/users/{id}/global-roles/{role}", target.getId(), "PLATFORM_ADMIN"), admin)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.globalRoles[0]").value("PLATFORM_ADMIN"));

        mockMvc.perform(
            asAdmin(put("/api/v1/users/{id}/global-roles/{role}", target.getId(), "PLATFORM_ADMIN"), admin)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.globalRoles.length()").value(1));

        mockMvc.perform(
            asAdmin(delete("/api/v1/users/{id}/global-roles/{role}", target.getId(), "PLATFORM_ADMIN"), admin)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.globalRoles").isEmpty());
    }

    @Test
    void shouldRejectRevokingOwnAdminRole() throws Exception {
        User admin = userRepository.save(account("465900"));

        mockMvc.perform(
            asAdmin(delete("/api/v1/users/{id}/global-roles/{role}", admin.getId(), "PLATFORM_ADMIN"), admin)
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("SELF_MANAGEMENT_NOT_ALLOWED"));
    }

    @Test
    void shouldReturn404ForUnknownUser() throws Exception {
        User admin = userRepository.save(account("465900"));

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", 999999), admin))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder asAdmin(
        MockHttpServletRequestBuilder builder,
        User admin
    ) {
        return builder.with(
            jwt()
                .jwt(token -> token.subject(String.valueOf(admin.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );
    }

    private User account(String isuNumber) {
        User user = new User();

        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setLastName("Иванов");
        user.setFirstName("Иван");

        return user;
    }
}
