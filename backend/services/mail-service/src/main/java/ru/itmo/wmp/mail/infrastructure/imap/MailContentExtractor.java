package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MailContentExtractor {
    public String extractText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) return part.getContent().toString();

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();

            String htmlFallback = "";

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String result = extractText(bodyPart);

                if (bodyPart.isMimeType("text/plain") && !result.isBlank()) return result;

                if (bodyPart.isMimeType("text/html")) htmlFallback = result;

                if (!result.isBlank() && htmlFallback.isBlank()) htmlFallback = result;
            }

            return htmlFallback;
        }

        return "";
    }
}
