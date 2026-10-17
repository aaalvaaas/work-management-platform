package ru.itmo.wmp.platform.domain.mailbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.organization.Organization;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_mailboxes")
@Getter
@Setter
public class OrganizationMailbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_cipher", nullable = false, columnDefinition = "text")
    private String passwordCipher;

    @Column(nullable = false, length = 100)
    private String folder;

    @Column(nullable = false)
    private boolean useSsl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MailboxStatus status;

    @Column(length = 500)
    private String lastError;

    @Column
    private LocalDateTime lastCheckedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
