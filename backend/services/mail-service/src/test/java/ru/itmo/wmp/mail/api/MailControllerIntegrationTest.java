package ru.itmo.wmp.mail.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.application.repository.MailRepository;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.dto.request.MailRequest;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MailControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MailAttachmentRepository mailAttachmentRepository;

    @BeforeEach
    void cleanDatabase() {
        mailRepository.deleteAll();
    }

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
    void shouldGetMailAttachments() throws Exception {
        MailMessage mail = new MailMessage();

        mail.setMessageId("mail-with-attachments");
        mail.setSenderEmail("sender@test.ru");
        mail.setRecipientEmail("recipient@test.ru");
        mail.setSubject("Mail with attachments");
        mail.setPlainTextBody("Test body");
        mail.setReceivedAt(LocalDateTime.now());
        mail.setProcessingStatus(MailProcessingStatus.RECEIVED);

        MailMessage savedMail = mailRepository.save(mail);

        MailAttachment attachment1 = new MailAttachment();

        attachment1.setMailMessage(savedMail);
        attachment1.setFilename("document.pdf");
        attachment1.setContentType("application/pdf");
        attachment1.setSize(1024L);
        attachment1.setStorageKey("mail/1/document.pdf");

        MailAttachment attachment2 = new MailAttachment();

        attachment2.setMailMessage(savedMail);
        attachment2.setFilename("image.png");
        attachment2.setContentType("image/png");
        attachment2.setSize(2048L);
        attachment2.setStorageKey("mail/1/image.png");

        mailAttachmentRepository.save(attachment1);
        mailAttachmentRepository.save(attachment2);

        mockMvc.perform(
            get(
                "/api/v1/mails/{messageId}/attachments",
                savedMail.getMessageId()
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].filename").value("document.pdf"))
            .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
            .andExpect(jsonPath("$[0].size").value(1024L))
            .andExpect(jsonPath("$[1].filename").value("image.png"))
            .andExpect(jsonPath("$[1].contentType").value("image/png"))
            .andExpect(jsonPath("$[1].size").value(2048L));
    }

    @Test
    void shouldReturnEmptyListWhenMailHasNoAttachments() throws Exception {
        MailMessage mail = new MailMessage();

        mail.setMessageId("mail-without-attachments");
        mail.setSenderEmail("sender@test.ru");
        mail.setRecipientEmail("recipient@test.ru");
        mail.setSubject("Mail without attachments");
        mail.setPlainTextBody("Test body");
        mail.setReceivedAt(LocalDateTime.now());
        mail.setProcessingStatus(MailProcessingStatus.RECEIVED);

        mailRepository.save(mail);

        mockMvc.perform(
            get(
                "/api/v1/mails/{messageId}/attachments",
                mail.getMessageId()
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenMailDoesNotExit() throws Exception {
        mockMvc.perform(
            get(
                "/api/v1/mails/{messageId}/attachments",
                "unknown-message-id"
            )
        )
            .andExpect(status().isNotFound());
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
