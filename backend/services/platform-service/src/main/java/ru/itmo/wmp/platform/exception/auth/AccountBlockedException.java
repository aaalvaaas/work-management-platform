package ru.itmo.wmp.platform.exception.auth;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(String isuNumber) {
        super("Account with isuNumber=" + isuNumber + " is blocked");
    }
}
