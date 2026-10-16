package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.util.List;
import java.util.Optional;

public interface OrganizationRoleRepository {
    List<OrganizationRole> findByOrganizationId(Long organizationId);

    List<OrganizationRole> findByUserId(Long userId);

    Optional<OrganizationRole> findByOrganizationIdAndRole(
        Long organizationId,
        OrganizationRoleType role
    );

    Optional<OrganizationRole> findByOrganizationIdAndUserIdAndRole(
        Long organizationId,
        Long userId,
        OrganizationRoleType role
    );

    boolean existsByOrganizationIdAndUserIdAndRoleIn(
        Long organizationId,
        Long userId,
        List<OrganizationRoleType> roles
    );

    OrganizationRole save(OrganizationRole organizationRole);

    void delete(OrganizationRole organizationRole);
}
