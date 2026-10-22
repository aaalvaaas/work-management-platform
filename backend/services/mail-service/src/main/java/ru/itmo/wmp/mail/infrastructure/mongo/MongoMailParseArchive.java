package ru.itmo.wmp.mail.infrastructure.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.archive.MailParseArchive;
import ru.itmo.wmp.mail.application.archive.ParseRecord;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MongoMailParseArchive implements MailParseArchive {
    private final MongoMailParseRepository repository;

    @Override
    public void record(ParseRecord parseRecord) {
        MailParseDocument document = new MailParseDocument();

        document.setMessageId(parseRecord.messageId());
        document.setOrganizationId(parseRecord.organizationId());
        document.setParser(parseRecord.parser());
        document.setParserVersion(parseRecord.parserVersion());
        document.setParsedAt(parseRecord.parsedAt());
        document.setTitle(parseRecord.title());
        document.setDescription(parseRecord.description());
        document.setRequiresApproval(parseRecord.requiresApproval());
        document.setRequesterEmail(parseRecord.requesterEmail());
        document.setConfidence(parseRecord.confidence());
        document.setDetails(parseRecord.details());

        repository.save(document);
    }

    @Override
    public List<ParseRecord> parsesOf(String messageId) {
        return repository.findByMessageIdOrderByParsedAtDesc(messageId)
            .stream()
            .map(this::toParseRecord)
            .toList();
    }

    private ParseRecord toParseRecord(MailParseDocument document) {
        return new ParseRecord(
            document.getMessageId(),
            document.getOrganizationId(),
            document.getParser(),
            document.getParserVersion(),
            document.getParsedAt(),
            document.getTitle(),
            document.getDescription(),
            document.isRequiresApproval(),
            document.getRequesterEmail(),
            document.getConfidence(),
            document.getDetails() == null ? Map.of() : document.getDetails()
        );
    }
}
