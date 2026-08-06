package ru.itmo.wmp.mail.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.mail.dto.request.MailRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MailControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReceiveMail() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value("test-001"))
            .andExpect(jsonPath("$.senderEmail").value("sender@test.ru"))
            .andExpect(jsonPath("$.recipientEmail").value("recipient@test.ru"))
            .andExpect(jsonPath("$.processingStatus").value("RECEIVED"));
    }

    @Test
    void shouldGetMailByMessageId() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        String response = mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        String messageId = objectMapper.readTree(response)
            .get("messageId")
            .asText();

        mockMvc.perform(get("/api/v1/mails/{messageId}", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value(messageId));
    }

    @Test
    void shouldReturnAllMails() throws Exception {
        MailRequest request1 = new MailRequest(
            "test-001",
            "sender1@test.ru",
            "recipient1@test.ru",
            "subject1",
            "body1"
        );

        MailRequest request2 = new MailRequest(
            "test-002",
            "sender2@test.ru",
            "recipient2@test.ru",
            "subject2",
            "body2"
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        );

        mockMvc.perform(get("/api/v1/mails"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].messageId").value("test-001"));
    }

    @Test
    void shouldMarkMailAsProcessing() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        String response = mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        String messageId = objectMapper.readTree(response)
            .get("messageId")
            .asText();

        mockMvc.perform(patch("/api/v1/mails/{messageId}/processing", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("PROCESSING"));
    }

    @Test
    void shouldMarkMailAsProcessed() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        String response = mockMvc.perform(
                post("/api/v1/mails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        String messageId = objectMapper.readTree(response)
            .get("messageId")
            .asText();

        mockMvc.perform(patch("/api/v1/mails/{messageId}/processing", messageId));
        mockMvc.perform(patch("/api/v1/mails/{messageId}/processed", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));
    }

    @Test
    void shouldMarkMailAsFailed() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        String response = mockMvc.perform(
                post("/api/v1/mails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        String messageId = objectMapper.readTree(response)
            .get("messageId")
            .asText();

        mockMvc.perform(patch("/api/v1/mails/{messageId}/processing", messageId));
        mockMvc.perform(patch("/api/v1/mails/{messageId}/failed", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingStatus").value("FAILED"));
    }

    @Test
    void shouldRejectInvalidStatusTransition() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        String response = mockMvc.perform(
                post("/api/v1/mails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        String messageId = objectMapper.readTree(response)
            .get("messageId")
            .asText();

        mockMvc.perform(patch("/api/v1/mails/{messageId}/processed", messageId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenMailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/mails/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicateMail() throws Exception {
        MailRequest request = new MailRequest(
            "test-001",
            "sender@test.ru",
            "recipient@test.ru",
            "subject",
            "body"
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        MailRequest request = new MailRequest(
            "",
            "",
            "",
            "",
            ""
        );

        mockMvc.perform(
            post("/api/v1/mails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
