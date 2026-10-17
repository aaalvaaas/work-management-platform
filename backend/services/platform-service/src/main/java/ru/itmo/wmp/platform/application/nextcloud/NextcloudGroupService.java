package ru.itmo.wmp.platform.application.nextcloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.domain.organization.BlockMember;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextcloudGroupService {
    private final NextcloudGroupGateway groupGateway;
    private final BlockMemberRepository blockMemberRepository;

    @Transactional(readOnly = true)
    public void sync(OrganizationParticipant participant) {
        if (participant.getUser() == null) {
            return;
        }

        String uid = participant.getUser().getIsuNumber();

        try {
            apply(uid, desiredGroups(participant));
        } catch (RuntimeException exception) {
            log.warn(
                "Nextcloud groups of {} were not synchronized: {}",
                uid,
                exception.getMessage()
            );
        }
    }

    private Set<String> desiredGroups(OrganizationParticipant participant) {
        Set<String> desired = new HashSet<>();

        if (participant.getStatus() != ParticipantStatus.ACTIVE) {
            return desired;
        }

        String organizationCode = participant.getOrganization().getCode();

        desired.add(NextcloudGroups.organization(organizationCode));

        List<BlockMember> memberships = blockMemberRepository
            .findByParticipantId(participant.getId());

        memberships.forEach(member -> desired.add(
            NextcloudGroups.block(organizationCode, member.getBlock().getId())
        ));

        return desired;
    }

    private void apply(String uid, Set<String> desired) {
        Set<String> current = new HashSet<>(groupGateway.groupsOf(uid));

        desired.stream()
            .filter(group -> !current.contains(group))
            .forEach(group -> {
                groupGateway.ensureGroup(group);
                groupGateway.addUserToGroup(uid, group);
            });

        current.stream()
            .filter(NextcloudGroups::isManaged)
            .filter(group -> !desired.contains(group))
            .forEach(group -> groupGateway.removeUserFromGroup(uid, group));
    }
}
