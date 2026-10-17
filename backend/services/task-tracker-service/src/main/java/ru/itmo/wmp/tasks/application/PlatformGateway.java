package ru.itmo.wmp.tasks.application;

public interface PlatformGateway {
    OrganizationMembership membershipOf(Long organizationId);

    OrganizationSummary organizationOf(Long organizationId);

    BlockSummary blockOf(Long organizationId, Long blockId);

    ParticipantSummary requireActiveParticipant(Long organizationId, String isuNumber);
}
