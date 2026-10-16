package ru.itmo.wmp.platform.domain.organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "block_roles",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_block_roles_scope_user_role",
            columnNames = {"block_id", "user_id", "role"}
        )
    }
)
@Getter
@Setter
public class BlockRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockRoleType role;

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
