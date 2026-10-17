package ru.itmo.wmp.platform.application.nextcloud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.domain.organization.Block;
import ru.itmo.wmp.platform.domain.organization.BlockMember;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextcloudGroupServiceTest {
    private static final String UID = "465824";

    @Mock
    private NextcloudGroupGateway groupGateway;

    @Mock
    private BlockMemberRepository blockMemberRepository;

    @InjectMocks
    private NextcloudGroupService groupService;

    @Test
    void shouldAddActiveParticipantToOrganizationAndBlockGroups() {
        OrganizationParticipant participant = participant(ParticipantStatus.ACTIVE);

        when(blockMemberRepository.findByParticipantId(participant.getId()))
            .thenReturn(List.of(blockMember(participant, 3L)));
        when(groupGateway.groupsOf(UID)).thenReturn(List.of());

        groupService.sync(participant);

        verify(groupGateway).addUserToGroup(UID, "wmp-org-megafaculty");
        verify(groupGateway).addUserToGroup(UID, "wmp-block-megafaculty-3");
    }

    @Test
    void shouldRemoveExcludedParticipantFromManagedGroups() {
        OrganizationParticipant participant = participant(ParticipantStatus.INACTIVE);

        when(groupGateway.groupsOf(UID))
            .thenReturn(List.of("wmp-org-megafaculty", "wmp-block-megafaculty-3", "admin"));

        groupService.sync(participant);

        verify(groupGateway).removeUserFromGroup(UID, "wmp-org-megafaculty");
        verify(groupGateway).removeUserFromGroup(UID, "wmp-block-megafaculty-3");
        verify(groupGateway, never()).removeUserFromGroup(UID, "admin");
    }

    @Test
    void shouldLeaveGroupsThatAreAlreadyCorrect() {
        OrganizationParticipant participant = participant(ParticipantStatus.ACTIVE);

        when(blockMemberRepository.findByParticipantId(participant.getId()))
            .thenReturn(List.of());
        when(groupGateway.groupsOf(UID)).thenReturn(List.of("wmp-org-megafaculty"));

        groupService.sync(participant);

        verify(groupGateway, never()).addUserToGroup(anyString(), anyString());
        verify(groupGateway, never()).removeUserFromGroup(anyString(), anyString());
    }

    @Test
    void shouldSkipParticipantWithoutAccount() {
        OrganizationParticipant participant = participant(ParticipantStatus.ACTIVE);

        participant.setUser(null);

        groupService.sync(participant);

        verify(groupGateway, never()).groupsOf(anyString());
    }

    private OrganizationParticipant participant(ParticipantStatus status) {
        Organization organization = new Organization();

        organization.setId(10L);
        organization.setCode("megafaculty");

        User user = new User();

        user.setId(100L);
        user.setIsuNumber(UID);

        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setId(1L);
        participant.setOrganization(organization);
        participant.setUser(user);
        participant.setStatus(status);

        return participant;
    }

    private BlockMember blockMember(OrganizationParticipant participant, Long blockId) {
        Block block = new Block();

        block.setId(blockId);

        BlockMember member = new BlockMember();

        member.setBlock(block);
        member.setParticipant(participant);

        return member;
    }
}
