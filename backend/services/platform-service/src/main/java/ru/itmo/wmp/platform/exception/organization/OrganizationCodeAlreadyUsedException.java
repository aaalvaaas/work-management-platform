package ru.itmo.wmp.platform.exception.organization;

public class OrganizationCodeAlreadyUsedException extends RuntimeException {
    public OrganizationCodeAlreadyUsedException(String code) {
        super("Organization code " + code + " is already used");
    }
}
