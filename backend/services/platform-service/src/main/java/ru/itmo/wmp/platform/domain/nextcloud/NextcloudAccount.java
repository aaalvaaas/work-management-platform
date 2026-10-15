package ru.itmo.wmp.platform.domain.nextcloud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "nextcloud_accounts")
@Getter
@Setter
public class NextcloudAccount {
    public static final int LAST_ERROR_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String nextcloudUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NextcloudAccountStatus status = NextcloudAccountStatus.PENDING;

    @Column(length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column
    private LocalDateTime lastSyncedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
