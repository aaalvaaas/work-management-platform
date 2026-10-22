package ru.itmo.wmp.mail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "ru.itmo.wmp.mail.infrastructure.persistance")
@EnableMongoRepositories(basePackages = "ru.itmo.wmp.mail.infrastructure.mongo")
public class PersistenceConfig {
}
