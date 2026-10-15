package ru.itmo.wmp.platform.exception.registration;

public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException(String isuNumber) {
        super("Account for ISU number " + isuNumber + " already exists");
    }
}
