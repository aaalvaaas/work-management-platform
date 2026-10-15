package ru.itmo.wmp.platform.exception.registration;

public class RegistrationNotAllowedException extends RuntimeException {
    public RegistrationNotAllowedException(String isuNumber) {
        super("ISU number " + isuNumber + " is not present in any organization participant list");
    }
}
