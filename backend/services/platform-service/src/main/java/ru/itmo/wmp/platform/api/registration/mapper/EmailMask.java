package ru.itmo.wmp.platform.api.registration.mapper;

final class EmailMask {
    private static final String HIDDEN = "***";

    private EmailMask() {
    }

    static String apply(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int separator = email.indexOf('@');

        if (separator <= 1) {
            return HIDDEN + email.substring(Math.max(separator, 0));
        }

        return email.charAt(0) + HIDDEN + email.substring(separator);
    }
}
