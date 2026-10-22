package ru.itmo.wmp.mail.infrastructure.imap;

import java.util.List;
import java.util.Map;

public record RawMailContent(
    Map<String, List<String>> headers,
    String textBody,
    String htmlBody
) {
    public static RawMailContent empty() {
        return new RawMailContent(Map.of(), "", "");
    }
}
