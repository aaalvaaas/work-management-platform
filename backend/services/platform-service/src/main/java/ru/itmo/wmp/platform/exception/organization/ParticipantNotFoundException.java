package ru.itmo.wmp.platform.exception.organization;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(Long organizationId, String isuNumber) {
        super(
            "Participant with isuNumber=" + isuNumber
                + " is not listed in organization id=" + organizationId
        );
    }
}
