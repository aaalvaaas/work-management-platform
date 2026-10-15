package ru.itmo.wmp.platform.domain.organization;

import jakarta.persistence.*;
import lombok.Getter;
import ru.itmo.wmp.platform.domain.user.User;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "organization_participants",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_participant_org_isu",
            columnNames = {"organization_id", "isu_number"}
        )
    }
)
@Getter
@Setter
public class OrganizationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 6)
    private String isuNumber;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 100)
    private String patronymic;

    @Column(nullable = false)
    private String email;

    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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
