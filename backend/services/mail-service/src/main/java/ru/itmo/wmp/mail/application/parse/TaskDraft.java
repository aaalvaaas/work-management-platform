package ru.itmo.wmp.mail.application.parse;

public record TaskDraft(
    String title,
    String description,
    boolean requiresApproval,
    String requesterEmail
) {
}
