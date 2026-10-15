package ru.itmo.wmp.platform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.request.UserProfileRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.JpaUserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {
    private static final String ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    void shouldRejectAnonymousRequests() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnOwnProfile() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(authenticatedAs(get("/api/v1/users/me"), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.isuNumber").value("465824"))
            .andExpect(jsonPath("$.globalRoles").isArray())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldAllowUserToReadOwnProfileById() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(authenticatedAs(get("/api/v1/users/{id}", user.getId()), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value("465824"));
    }

    @Test
    void shouldForbidReadingAnotherUserProfile() throws Exception {
        User user = userRepository.save(user("465824"));
        User other = userRepository.save(user("465825"));

        mockMvc.perform(authenticatedAs(get("/api/v1/users/{id}", other.getId()), user))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowPlatformAdminToReadAnyProfile() throws Exception {
        User admin = userRepository.save(user("465824"));
        User other = userRepository.save(user("465825"));

        mockMvc.perform(authenticatedAsAdmin(get("/api/v1/users/{id}", other.getId()), admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value("465825"));
    }

    @Test
    void shouldForbidUserListForRegularUser() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(authenticatedAs(get("/api/v1/users"), user))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUserListForPlatformAdmin() throws Exception {
        User admin = userRepository.save(user("465824"));

        mockMvc.perform(authenticatedAsAdmin(get("/api/v1/users"), admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.isuNumber == '465824')]").exists());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        User admin = userRepository.save(user("465824"));

        mockMvc.perform(authenticatedAsAdmin(get("/api/v1/users/{id}", 999999), admin))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldUpdateOwnAllowedProfileFields() throws Exception {
        User user = userRepository.save(user("465824"));

        UserProfileRequest request = new UserProfileRequest(
            "Сергеевич",
            "Аналитик"
        );

        mockMvc.perform(
            authenticatedAs(patch("/api/v1/users/me"), user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patronymic").value("Сергеевич"))
            .andExpect(jsonPath("$.position").value("Аналитик"))
            .andExpect(jsonPath("$.isuNumber").value("465824"))
            .andExpect(jsonPath("$.lastName").value("Иванов"));
    }

    private MockHttpServletRequestBuilder authenticatedAs(
        MockHttpServletRequestBuilder builder,
        User user
    ) {
        return builder.with(
            jwt().jwt(token -> token.subject(String.valueOf(user.getId())))
        );
    }

    private MockHttpServletRequestBuilder authenticatedAsAdmin(
        MockHttpServletRequestBuilder builder,
        User user
    ) {
        return builder.with(
            jwt()
                .jwt(token -> token.subject(String.valueOf(user.getId())))
                .authorities(new SimpleGrantedAuthority(ADMIN_AUTHORITY))
        );
    }

    private User user(String isuNumber) {
        User user = new User();

        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash("hash");
        user.setLastName("Иванов");
        user.setFirstName("Иван");

        return user;
    }
}
