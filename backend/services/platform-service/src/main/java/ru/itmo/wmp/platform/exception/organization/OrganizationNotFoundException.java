package ru.itmo.wmp.platform.exception.organization;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(Long id) {
        super("Organization with id=" + id + " not found");
    }

    public OrganizationNotFoundException(String code) {
        super("Organization with code=" + code + " not found");
    }
}
