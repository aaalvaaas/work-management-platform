package ru.itmo.wmp.platform.domain.imports;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;

@Entity
@Table(name = "import_changes")
@Getter
@Setter
public class ImportChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportChangeType type;

    @Column(length = 6)
    private String isuNumber;

    @Column(length = 100)
    private String lastName;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String patronymic;

    @Column
    private String email;

    @Column
    private String position;

    @Column(length = 1000)
    private String blockNames;

    @Column(length = 1000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private OrganizationParticipant participant;
}
