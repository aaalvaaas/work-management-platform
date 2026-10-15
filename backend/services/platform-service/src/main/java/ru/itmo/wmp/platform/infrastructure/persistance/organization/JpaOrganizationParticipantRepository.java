package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;

import java.util.List;
import java.util.Optional;

public interface JpaOrganizationParticipantRepository extends JpaRepository<OrganizationParticipant, Long> {
    List<OrganizationParticipant> findByOrganizationId(Long organizationId);

    Optional<OrganizationParticipant> findByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );

    boolean existsByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );
}
