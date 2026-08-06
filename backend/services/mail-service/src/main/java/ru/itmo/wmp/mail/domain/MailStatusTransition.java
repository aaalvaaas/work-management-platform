package ru.itmo.wmp.mail.domain;

public final class MailStatusTransition {
    private MailStatusTransition() {}

    public static boolean canMove(MailProcessingStatus from, MailProcessingStatus to) {
        return switch (from) {
            case RECEIVED ->
                to == MailProcessingStatus.PROCESSING;

            case PROCESSING ->
                to == MailProcessingStatus.PROCESSED || to == MailProcessingStatus.FAILED;

            case PROCESSED, FAILED ->
                false;
        };
    }
}
