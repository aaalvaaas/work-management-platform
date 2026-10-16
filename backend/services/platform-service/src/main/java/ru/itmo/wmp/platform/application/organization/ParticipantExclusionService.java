package ru.itmo.wmp.platform.application.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.application.organization.repository.BlockRoleRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRoleRepository;
import ru.itmo.wmp.platform.application.security.UserAuthorizationService;
import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.organization.ParticipantNotFoundException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantExclusionService {
    private final OrganizationParticipantRepository participantRepository;
    private final BlockMemberRepository blockMemberRepository;
    private final BlockRoleRepository blockRoleRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final UserAuthorizationService userAuthorizationService;

    public OrganizationParticipant exclude(Long organizationId, String isuNumber) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organizationId, isuNumber)
            .orElseThrow(() -> new ParticipantNotFoundException(organizationId, isuNumber));

        participant.setStatus(ParticipantStatus.INACTIVE);

        OrganizationParticipant excluded = participantRepository.save(participant);

        blockMemberRepository.deleteAll(
            blockMemberRepository.findByParticipantId(participant.getId())
        );

        User user = participant.getUser();

        if (user != null) {
            revokeOrganizationRoles(organizationId, user.getId());
            revokeBlockRoles(organizationId, user.getId());

            userAuthorizationService.evict(user.getId());
        }

        return excluded;
    }

    private void revokeOrganizationRoles(Long organizationId, Long userId) {
        organizationRoleRepository.findByUserId(userId)
            .stream()
            .filter(role -> Objects.equals(role.getOrganization().getId(), organizationId))
            .forEach(organizationRoleRepository::delete);
    }

    private void revokeBlockRoles(Long organizationId, Long userId) {
        List<BlockRole> roles = blockRoleRepository.findByUserId(userId)
            .stream()
            .filter(role -> Objects.equals(
                role.getBlock().getOrganization().getId(),
                organizationId
            ))
            .toList();

        roles.forEach(blockRoleRepository::delete);
    }
}
