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
    public Optional<Organization> findByCode(String code) {
        return organizationRepository.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return organizationRepository.existsByCode(code);
    }

    @Override
    public List<Organization> findByIdIn(List<Long> ids) {
        return organizationRepository.findByIdIn(ids);
    }

    @Override
    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }
}
