package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRepository;
import ru.itmo.wmp.platform.domain.organization.Organization;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationRepositoryAdapter implements OrganizationRepository {
    private final JpaOrganizationRepository organizationRepository;

    @Override
    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    @Override
    public Optional<Organization> findById(Long id) {
        return organizationRepository.findById(id);
    }

    @Override
    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }

    @Override
    public void delete(Organization organization) {
        organizationRepository.delete(organization);
    }
}
