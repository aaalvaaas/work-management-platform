package ru.itmo.wmp.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_global_roles",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_user_global_roles_user_role",
            columnNames = {"user_id", "role"}
        )
    }
)
@Getter
@Setter
public class UserGlobalRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GlobalRole role;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedBy;

    @PrePersist
    void onCreate() {
        grantedAt = LocalDateTime.now();
    }
}
