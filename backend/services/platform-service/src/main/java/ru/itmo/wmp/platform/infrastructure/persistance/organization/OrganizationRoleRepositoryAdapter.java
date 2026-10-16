package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRoleRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationRoleRepositoryAdapter implements OrganizationRoleRepository {
    private final JpaOrganizationRoleRepository organizationRoleRepository;

    @Override
    public List<OrganizationRole> findByOrganizationId(Long organizationId) {
        return organizationRoleRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<OrganizationRole> findByUserId(Long userId) {
        return organizationRoleRepository.findByUserId(userId);
    }

    @Override
    public Optional<OrganizationRole> findByOrganizationIdAndRole(
        Long organizationId,
        OrganizationRoleType role
    ) {
        return organizationRoleRepository.findByOrganizationIdAndRole(organizationId, role);
    }

    @Override
    public Optional<OrganizationRole> findByOrganizationIdAndUserIdAndRole(
        Long organizationId,
        Long userId,
        OrganizationRoleType role
    ) {
        return organizationRoleRepository
            .findByOrganizationIdAndUserIdAndRole(organizationId, userId, role);
    }

    @Override
    public boolean existsByOrganizationIdAndUserIdAndRoleIn(
        Long organizationId,
        Long userId,
        List<OrganizationRoleType> roles
    ) {
        return organizationRoleRepository
            .existsByOrganizationIdAndUserIdAndRoleIn(organizationId, userId, roles);
    }

    @Override
    public OrganizationRole save(OrganizationRole organizationRole) {
        return organizationRoleRepository.save(organizationRole);
    }

    @Override
    public void delete(OrganizationRole organizationRole) {
        organizationRoleRepository.delete(organizationRole);
    }
}
