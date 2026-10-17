package ru.itmo.wmp.platform.exception.registration;

public class RegistrationRequestNotPendingException extends RuntimeException {
    public RegistrationRequestNotPendingException(Long requestId, String status) {
        super("Registration request id=" + requestId + " is already in status " + status);
    }
}
