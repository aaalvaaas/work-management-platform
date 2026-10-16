package ru.itmo.wmp.platform.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class JwkSetControllerIntegrationTest {
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
    void shouldPublishPublicSigningKeyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].kid").exists())
            .andExpect(jsonPath("$.keys[0].n").exists())
            .andExpect(jsonPath("$.keys[0].d").doesNotExist())
            .andExpect(jsonPath("$.keys[0].p").doesNotExist());
    }

    @Test
    void shouldSignAccessTokenWithThePublishedKey() throws Exception {
        userRepository.save(user("465824"));

        JsonNode header = accessTokenHeader();

        assertThat(header.get("alg").asText()).isEqualTo("RS256");
        assertThat(header.get("kid").asText()).isEqualTo(publishedKeyId());
    }

    private JsonNode accessTokenHeader() throws Exception {
        String response = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("465824", PASSWORD)))
        )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = objectMapper.readTree(response)
            .get("accessToken")
            .asText();

        byte[] decoded = Base64.getUrlDecoder()
            .decode(accessToken.substring(0, accessToken.indexOf('.')));

        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }

    private String publishedKeyId() throws Exception {
        String response = mockMvc.perform(get("/.well-known/jwks.json"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response)
            .get("keys")
            .get(0)
            .get("kid")
            .asText();
    }

    private User user(String isuNumber) {
        User user = new User();

        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setLastName("Иванов");
        user.setFirstName("Иван");
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }
}
