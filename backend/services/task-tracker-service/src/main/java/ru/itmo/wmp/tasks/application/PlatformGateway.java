package ru.itmo.wmp.tasks.application;

public interface PlatformGateway {
    OrganizationMembership membershipOf(Long organizationId);

    void requireBlockInOrganization(Long organizationId, Long blockId);

    ParticipantSummary requireActiveParticipant(Long organizationId, String isuNumber);
}
