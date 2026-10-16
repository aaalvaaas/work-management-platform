package ru.itmo.wmp.platform.domain.organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "block_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_block_members_block_participant",
            columnNames = {"block_id", "participant_id"}
        )
    }
)
@Getter
@Setter
public class BlockMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private OrganizationParticipant participant;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    @PrePersist
    void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
