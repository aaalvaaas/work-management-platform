package ru.itmo.wmp.platform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.dto.request.UserRequest;

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

    @Test
    void shouldCreateUser() throws Exception {
        UserRequest request = new UserRequest(
            "test",
            "test@test.ru"
        );

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("test"))
            .andExpect(jsonPath("$.email").value("test@test.ru"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        UserRequest request = new UserRequest(
            "test",
            "test@test.ru"
        );

        String response = mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long id = objectMapper.readTree(response)
            .get("id")
            .asLong();

        mockMvc.perform(get("/api/v1/users/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.username").value("test"))
            .andExpect(jsonPath("$.email").value("test@test.ru"));
    }

    @Test
    void shouldReturnUsers() throws Exception {
        UserRequest request = new UserRequest(
            "test",
            "test@test.ru"
        );

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("test"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id=999999 not found"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserRequest createRequest = new UserRequest(
            "old",
            "old@test.ru"
        );

        String response = mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long id = objectMapper.readTree(response)
            .get("id")
            .asLong();

        UserRequest updateRequest = new UserRequest(
            "new",
            "new@test.ru"
        );

        mockMvc.perform(
            put("/api/v1/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.username").value("new"))
            .andExpect(jsonPath("$.email").value("new@test.ru"));

        mockMvc.perform(get("/api/v1/users/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("new"))
            .andExpect(jsonPath("$.email").value("new@test.ru"));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        UserRequest request = new UserRequest(
            "test",
            "test@test.ru"
        );

        String response = mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long id = objectMapper.readTree(response)
            .get("id")
            .asLong();

        mockMvc.perform(delete("/api/v1/users/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id=999999 not found"));
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        UserRequest request = new UserRequest(
            "",
            "wrong-email"
        );

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.username").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
    }
}
