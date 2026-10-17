package ru.itmo.wmp.platform.dto.organization;

import ru.itmo.wmp.platform.domain.organization.BlockRoleType;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;

import java.util.List;
import java.util.Set;

public record OrganizationMembershipResponse(
    Long organizationId,
    Long userId,
    boolean platformAdmin,
    boolean member,
    Set<OrganizationRoleType> roles,
    List<BlockMembershipResponse> blocks
) {
    public record BlockMembershipResponse(
        Long blockId,
        Set<BlockRoleType> roles
    ) {
    }
}
