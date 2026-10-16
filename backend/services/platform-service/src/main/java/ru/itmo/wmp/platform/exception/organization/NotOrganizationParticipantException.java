package ru.itmo.wmp.platform.exception.organization;

public class NotOrganizationParticipantException extends RuntimeException {
    public NotOrganizationParticipantException(Long organizationId, String isuNumber) {
        super(
            "User with isuNumber=" + isuNumber
                + " is not an active participant of organization id=" + organizationId
        );
    }
}
