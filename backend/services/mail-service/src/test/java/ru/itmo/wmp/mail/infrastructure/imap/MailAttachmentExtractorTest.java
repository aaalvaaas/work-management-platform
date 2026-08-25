package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class MailAttachmentExtractorTest {
    private final MailAttachmentExtractor extractor = new MailAttachmentExtractor();

    @Test
    void shouldExtractAttachment() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart body = new MimeBodyPart();
        body.setText("Test Body");

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setFileName("test.txt");
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment.setText("Attachment Content");

        multipart.addBodyPart(body);
        multipart.addBodyPart(attachment);

        message.setContent(multipart);
        message.saveChanges();

        List<ParsedAttachment> result = extractor.extract(message);

        assertEquals(1, result.size());

        ParsedAttachment parsed = result.get(0);

        assertEquals("test.txt", parsed.filename());
        assertTrue(parsed.contentType().startsWith("text/plain"));
        assertNotNull(parsed.inputStream());
    }

    @Test
    void shouldExtractMultipleAttachments() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart attachment1 = new MimeBodyPart();
        attachment1.setFileName("document.pdf");
        attachment1.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment1.setText("PDF content");
        attachment1.setHeader(
            "Content-Type",
            "application/pdf"
        );

        MimeBodyPart attachment2 = new MimeBodyPart();
        attachment2.setFileName("image.png");
        attachment2.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment2.setText("PNG content");
        attachment2.setHeader(
            "Content-Type",
            "image/png"
        );

        multipart.addBodyPart(attachment1);
        multipart.addBodyPart(attachment2);

        message.setContent(multipart);
        message.saveChanges();

        List<ParsedAttachment> result = extractor.extract(message);

        assertEquals(2, result.size());
        assertEquals("document.pdf", result.get(0).filename());
        assertEquals("image.png", result.get(1).filename());
    }

    @Test
    void shouldReturnEmptyListWhenNoAttachments() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart body = new MimeBodyPart();
        body.setText("Test Body");

        multipart.addBodyPart(body);

        message.setContent(multipart);
        message.saveChanges();

        List<ParsedAttachment> result = extractor.extract(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreInlineParts() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart body = new MimeBodyPart();
        body.setText("Test Body");

        MimeBodyPart inline = new MimeBodyPart();
        inline.setFileName("image.png");
        inline.setDisposition(MimeBodyPart.INLINE);
        inline.setText("Image Content");
        inline.setHeader(
            "Content-Type",
            "image/png"
        );

        multipart.addBodyPart(body);
        multipart.addBodyPart(inline);

        message.setContent(multipart);
        message.saveChanges();

        List<ParsedAttachment> result = extractor.extract(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExtractNestedAttachment() throws MessagingException, IOException {
        MimeMessage message = createMessage();

        MimeMultipart outerMultipart = new MimeMultipart("mixed");
        MimeMultipart nestedMultipart = new MimeMultipart("mixed");

        MimeBodyPart body = new MimeBodyPart();
        body.setText("Test Body");

        MimeBodyPart nested = new MimeBodyPart();

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setFileName("nested.txt");
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment.setText("Nested Attachment");
        attachment.setHeader(
            "Content-Type",
            "text/plain"
        );

        nestedMultipart.addBodyPart(attachment);
        nested.setContent(nestedMultipart);

        outerMultipart.addBodyPart(body);
        outerMultipart.addBodyPart(nested);

        message.setContent(outerMultipart);
        message.saveChanges();

        List<ParsedAttachment> result = extractor.extract(message);

        assertEquals(1, result.size());
        assertEquals("nested.txt", result.get(0).filename());
    }

    private MimeMessage createMessage() throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);

        message.setFrom("sender@test.ru");
        message.setRecipients(
            Message.RecipientType.TO,
            "recipient@test.ru"
        );
        message.setSubject("Test Subject");

        return message;
    }
}
