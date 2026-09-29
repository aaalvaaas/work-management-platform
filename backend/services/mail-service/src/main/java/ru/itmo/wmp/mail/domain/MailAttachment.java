package ru.itmo.wmp.mail.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.C;

@Entity
@Table(name = "mail_attachments")
@Getter
@Setter
public class MailAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mail_message_id", nullable = false)
    private MailMessage mailMessage;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String storageKey;
}uj
