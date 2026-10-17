package ru.itmo.wmp.platform.application.nextcloud;

public final class NextcloudGroups {
    public static final String PREFIX = "wmp-";

    private NextcloudGroups() {
    }

    public static String organization(String organizationCode) {
        return PREFIX + "org-" + organizationCode;
    }

    public static String block(String organizationCode, Long blockId) {
        return PREFIX + "block-" + organizationCode + "-" + blockId;
    }

    public static boolean isManaged(String groupId) {
        return groupId != null && groupId.startsWith(PREFIX);
    }
}
