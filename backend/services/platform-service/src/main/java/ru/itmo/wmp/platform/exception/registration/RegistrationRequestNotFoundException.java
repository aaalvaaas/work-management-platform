package ru.itmo.wmp.platform.exception.registration;

public class RegistrationRequestNotFoundException extends RuntimeException {
    public RegistrationRequestNotFoundException(Long organizationId, Long requestId) {
        super("Registration request id=" + requestId
            + " not found in organization id=" + organizationId);
    }
}
