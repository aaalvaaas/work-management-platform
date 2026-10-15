package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;

import java.util.List;
import java.util.Optional;

public interface OrganizationParticipantRepository {
    List<OrganizationParticipant> findAll();

    List<OrganizationParticipant> findByOrganizationId(Long organizationId);

    List<OrganizationParticipant> findByIsuNumberAndStatus(
        String isuNumber,
        ParticipantStatus status
    );

    Optional<OrganizationParticipant> findById(Long id);

    Optional<OrganizationParticipant> findByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );

    boolean existsByOrganizationIdAndIsuNumber(
        Long organizationId,
        String isuNumber
    );

    OrganizationParticipant save(OrganizationParticipant participant);

    List<OrganizationParticipant> saveAll(List<OrganizationParticipant> participants);

    void delete(OrganizationParticipant participant);
}
