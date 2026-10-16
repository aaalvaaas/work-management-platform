package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;

import java.util.List;
import java.util.Optional;

public interface JpaOrganizationParticipantRepository extends JpaRepository<OrganizationParticipant, Long> {
    List<OrganizationParticipant> findByOrganizationId(Long organizationId);

    List<OrganizationParticipant> findByUserIdAndStatus(
        Long userId,
        ParticipantStatus status
    );

    List<OrganizationParticipant> findByOrganizationIdAndStatus(
        Long organizationId,
        ParticipantStatus status
    );

    boolean existsByOrganizationIdAndUserIdAndStatus(
        Long organizationId,
        Long userId,
        ParticipantStatus status
    );

    List<OrganizationParticipant> findByIsuNumberAndStatus(
        String isuNumber,
        ParticipantStatus status
    );

    Optional<OrganizationParticipant> findByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );

    boolean existsByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );
}
