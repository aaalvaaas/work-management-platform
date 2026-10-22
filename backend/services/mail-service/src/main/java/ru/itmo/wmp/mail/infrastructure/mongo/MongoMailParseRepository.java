package ru.itmo.wmp.mail.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoMailParseRepository extends MongoRepository<MailParseDocument, String> {
    List<MailParseDocument> findByMessageIdOrderByParsedAtDesc(String messageId);
}
