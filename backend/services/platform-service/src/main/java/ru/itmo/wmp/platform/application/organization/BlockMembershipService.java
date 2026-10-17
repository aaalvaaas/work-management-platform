package ru.itmo.wmp.platform.application.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.security.UserAuthorizationService;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudGroupService;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.domain.organization.Block;
import ru.itmo.wmp.platform.domain.organization.BlockMember;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.exception.organization.NotOrganizationParticipantException;
import ru.itmo.wmp.platform.exception.organization.ParticipantNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockMembershipService {
    private final BlockMemberRepository blockMemberRepository;
    private final OrganizationParticipantRepository participantRepository;
    private final BlockService blockService;
    private final UserService userService;
    private final UserAuthorizationService userAuthorizationService;
    private final NextcloudGroupService nextcloudGroupService;

    public List<BlockMember> membersOf(Long organizationId, Long blockId) {
        blockService.getInOrganization(organizationId, blockId);

        return blockMemberRepository.findByBlockId(blockId);
    }

    @Transactional
    public BlockMember addMember(
        Long organizationId,
        Long blockId,
        String isuNumber,
        Long actorUserId
    ) {
        Block block = blockService.getInOrganization(organizationId, blockId);
        OrganizationParticipant participant = activeParticipant(organizationId, isuNumber);

        evictParticipant(participant);

        BlockMember member = blockMemberRepository
            .findByBlockIdAndParticipantId(blockId, participant.getId())
            .orElseGet(() -> {
                BlockMember created = new BlockMember();

                created.setBlock(block);
                created.setParticipant(participant);
                created.setAddedBy(userService.getById(actorUserId));

                return blockMemberRepository.save(created);
            });

        nextcloudGroupService.sync(participant);

        return member;
    }

    @Transactional
    public void removeMember(Long organizationId, Long blockId, String isuNumber) {
        blockService.getInOrganization(organizationId, blockId);

        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organizationId, isuNumber)
            .orElseThrow(() -> new ParticipantNotFoundException(organizationId, isuNumber));

        blockMemberRepository
            .findByBlockIdAndParticipantId(blockId, participant.getId())
            .ifPresent(blockMemberRepository::delete);

        evictParticipant(participant);

        nextcloudGroupService.sync(participant);
    }

    @Transactional
    public void removeFromAllBlocks(Long participantId) {
        blockMemberRepository.deleteAll(blockMemberRepository.findByParticipantId(participantId));
    }

    private void evictParticipant(OrganizationParticipant participant) {
        if (participant.getUser() != null) {
            userAuthorizationService.evict(participant.getUser().getId());
        }
    }

    private OrganizationParticipant activeParticipant(Long organizationId, String isuNumber) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organizationId, isuNumber)
            .orElseThrow(() -> new ParticipantNotFoundException(organizationId, isuNumber));

        if (participant.getStatus() != ParticipantStatus.ACTIVE) {
            throw new NotOrganizationParticipantException(organizationId, isuNumber);
        }

        return participant;
    }
}
