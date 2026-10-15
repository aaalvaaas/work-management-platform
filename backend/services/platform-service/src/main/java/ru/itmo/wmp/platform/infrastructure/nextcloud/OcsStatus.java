package ru.itmo.wmp.platform.infrastructure.nextcloud;

final class OcsStatus {
    static final int OK = 100;
    static final int OK_V2 = 200;
    static final int USER_ALREADY_EXISTS = 102;
    static final int NOT_FOUND = 404;
    static final int UNKNOWN_USER = 998;

    private OcsStatus() {
    }

    static boolean isSuccess(int statuscode) {
        return statuscode == OK || statuscode == OK_V2;
    }

    static boolean isMissingUser(int statuscode) {
        return statuscode == NOT_FOUND || statuscode == UNKNOWN_USER;
    }
}
