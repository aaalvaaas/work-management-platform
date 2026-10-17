package ru.itmo.wmp.platform.exception.registration;

public class RegistrationRequestAlreadySubmittedException extends RuntimeException {
    public RegistrationRequestAlreadySubmittedException(String isuNumber) {
        super("Registration request for ISU number " + isuNumber + " is already waiting for a decision");
    }
}
