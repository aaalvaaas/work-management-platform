package ru.itmo.wmp.platform.application.repository;

import ru.itmo.wmp.platform.domain.OrganizationParticipant;

import java.util.List;
import java.util.Optional;

public interface OrganizationParticipantRepository {
    List<OrganizationParticipant> findAll();

    List<OrganizationParticipant> findByOrganizationId(Long organizationId);

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

    void delete(OrganizationParticipant participant);
}
