package ru.itmo.wmp.platform.domain.imports;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
@Getter
@Setter
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportBatchStatus status;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int errorCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by_user_id")
    private User appliedBy;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isApplicable() {
        return status == ImportBatchStatus.VALIDATED;
    }
}
