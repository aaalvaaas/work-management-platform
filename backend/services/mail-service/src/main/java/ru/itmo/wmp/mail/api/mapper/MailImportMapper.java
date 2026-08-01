package ru.itmo.wmp.mail.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.MailImportResult;
import ru.itmo.wmp.mail.dto.response.MailImportResponse;

@Component
public class MailImportMapper {
    public MailImportResponse toResponse(MailImportResult result) {
        return new MailImportResponse(
            result.imported(),
            result.skipped()
        );
    }
}
