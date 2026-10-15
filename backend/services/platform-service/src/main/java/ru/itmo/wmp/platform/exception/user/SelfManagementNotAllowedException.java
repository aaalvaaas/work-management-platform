package ru.itmo.wmp.platform.exception.user;

public class SelfManagementNotAllowedException extends RuntimeException {
    public SelfManagementNotAllowedException(String operation) {
        super("Operation " + operation + " cannot be applied to your own account");
    }
}
