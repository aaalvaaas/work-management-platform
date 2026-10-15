package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationParticipantRepositoryAdapter implements OrganizationParticipantRepository {
    private final JpaOrganizationParticipantRepository organizationParticipantRepository;

    @Override
    public List<OrganizationParticipant> findAll() {
        return organizationParticipantRepository.findAll();
    }

    @Override
    public List<OrganizationParticipant> findByOrganizationId(Long organizationId) {
        return organizationParticipantRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Optional<OrganizationParticipant> findById(Long id) {
        return organizationParticipantRepository.findById(id);
    }

    @Override
    public Optional<OrganizationParticipant> findByOrganizationIdAndIsuNumber(Long organizationId, String isuNumber) {
        return organizationParticipantRepository.findByOrganizationIdAndIsuNumber(organizationId, isuNumber);
    }

    @Override
    public boolean existsByOrganizationIdAndIsuNumber(Long organizationId, String isuNumber) {
        return organizationParticipantRepository.existsByOrganizationIdAndIsuNumber(organizationId, isuNumber);
    }

    @Override
    public OrganizationParticipant save(OrganizationParticipant participant) {
        return organizationParticipantRepository.save(participant);
    }

    @Override
    public void delete(OrganizationParticipant participant) {
        organizationParticipantRepository.delete(participant);
    }
}
