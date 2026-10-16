package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.util.List;
import java.util.Optional;

public interface JpaOrganizationRoleRepository extends JpaRepository<OrganizationRole, Long> {
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
}
