package ru.itmo.wmp.platform.api.organization.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.dto.organization.OrganizationResponse;
import ru.itmo.wmp.platform.dto.organization.OrganizationRoleResponse;
import ru.itmo.wmp.platform.dto.organization.ParticipantResponse;
import ru.itmo.wmp.platform.application.security.UserAuthorization;
import ru.itmo.wmp.platform.dto.organization.OrganizationMembershipResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class OrganizationMapper {
    public List<OrganizationMembershipResponse.BlockMembershipResponse> toBlockMemberships(
        Collection<Long> organizationBlockIds,
        UserAuthorization authorization
    ) {
        return organizationBlockIds.stream()
            .filter(blockId -> !authorization.blockRoles()
                .getOrDefault(blockId, Set.of())
                .isEmpty())
            .map(blockId -> new OrganizationMembershipResponse.BlockMembershipResponse(
                blockId,
                authorization.blockRoles().get(blockId)
            ))
            .toList();
    }

    public OrganizationResponse toResponse(Organization organization, String managerIsuNumber) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getCode(),
            organization.getName(),
            organization.getStatus(),
            managerIsuNumber,
            organization.getCreatedAt()
        );
    }

    public OrganizationRoleResponse toResponse(OrganizationRole role) {
        return new OrganizationRoleResponse(
            role.getUser().getId(),
            role.getUser().getIsuNumber(),
            role.getUser().fullName(),
            role.getRole(),
            role.getGrantedAt()
        );
    }

    public ParticipantResponse toResponse(OrganizationParticipant participant) {
        return new ParticipantResponse(
            participant.getId(),
            participant.getIsuNumber(),
            participant.getLastName(),
            participant.getFirstName(),
            participant.getPatronymic(),
            participant.getEmail(),
            participant.getPosition(),
            participant.getStatus(),
            participant.getUser() != null
        );
    }
}
