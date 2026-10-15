package ru.itmo.wmp.platform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.dto.request.UserProfileRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.JpaUserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    void shouldGetUserById() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.isuNumber").value("465824"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.globalRoles").isArray());
    }

    @Test
    void shouldNotExposePasswordHash() throws Exception {
        User user = userRepository.save(user("465824"));

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnUsers() throws Exception {
        userRepository.save(user("465824"));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.isuNumber == '465824')]").exists());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id=999999 not found"));
    }

    @Test
    void shouldUpdateAllowedProfileFields() throws Exception {
        User user = userRepository.save(user("465824"));

        UserProfileRequest request = new UserProfileRequest(
            "Сергеевич",
            "Аналитик"
        );

        mockMvc.perform(
            patch("/api/v1/users/{id}", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patronymic").value("Сергеевич"))
            .andExpect(jsonPath("$.position").value("Аналитик"))
            .andExpect(jsonPath("$.isuNumber").value("465824"))
            .andExpect(jsonPath("$.lastName").value("Иванов"));
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
