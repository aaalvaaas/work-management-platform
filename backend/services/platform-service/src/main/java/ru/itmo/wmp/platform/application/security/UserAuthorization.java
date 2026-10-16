package ru.itmo.wmp.platform.application.security;

import ru.itmo.wmp.platform.domain.organization.BlockRoleType;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public record UserAuthorization(
    Map<Long, Set<OrganizationRoleType>> organizationRoles,
    Set<Long> activeOrganizations,
    Map<Long, Set<BlockRoleType>> blockRoles
) {
    public UserAuthorization(
        Map<Long, Set<OrganizationRoleType>> organizationRoles,
        Set<Long> activeOrganizations,
        Map<Long, Set<BlockRoleType>> blockRoles
    ) {
        this.organizationRoles = organizationRoles == null ? Map.of() : Map.copyOf(organizationRoles);
        this.activeOrganizations = activeOrganizations == null ? Set.of() : Set.copyOf(activeOrganizations);
        this.blockRoles = blockRoles == null ? Map.of() : Map.copyOf(blockRoles);
    }

    public boolean hasOrganizationRole(
        Long organizationId,
        Collection<OrganizationRoleType> roles
    ) {
        return organizationRoles
            .getOrDefault(organizationId, Set.of())
            .stream()
            .anyMatch(roles::contains);
    }

    public boolean isActiveMemberOf(Long organizationId) {
        return activeOrganizations.contains(organizationId);
    }

    public boolean hasBlockRole(Long blockId, BlockRoleType role) {
        return blockRoles
            .getOrDefault(blockId, Set.of())
            .contains(role);
    }
}
