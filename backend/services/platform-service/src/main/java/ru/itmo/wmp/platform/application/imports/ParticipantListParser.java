package ru.itmo.wmp.platform.application.imports;

import java.util.List;

public interface ParticipantListParser {
    List<ParsedParticipantRow> parse(String filename, byte[] content);
}
