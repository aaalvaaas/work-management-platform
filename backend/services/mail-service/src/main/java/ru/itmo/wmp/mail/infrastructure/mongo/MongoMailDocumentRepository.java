package ru.itmo.wmp.mail.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoMailDocumentRepository extends MongoRepository<MailDocument, String> {
}
