package ru.itmo.wmp.platform.infrastructure.imports;

import java.util.List;
import java.util.Locale;

enum ParticipantColumn {
    ISU_NUMBER(true, "номер ису", "ису", "isu", "isu_number", "isunumber"),
    LAST_NAME(true, "фамилия", "last_name", "lastname", "surname"),
    FIRST_NAME(true, "имя", "first_name", "firstname"),
    PATRONYMIC(false, "отчество", "patronymic", "middle_name", "middlename"),
    EMAIL(true, "email", "e-mail", "почта", "электронная почта"),
    POSITION(false, "должность", "position"),
    BLOCKS(false, "блок", "блоки", "block", "blocks");

    private final boolean required;
    private final List<String> titles;

    ParticipantColumn(boolean required, String... titles) {
        this.required = required;
        this.titles = List.of(titles);
    }

    boolean isRequired() {
        return required;
    }

    String title() {
        return titles.getFirst();
    }

    static ParticipantColumn of(String header) {
        if (header == null) {
            return null;
        }

        String normalized = header.trim().toLowerCase(Locale.ROOT).replace(' ', ' ');

        for (ParticipantColumn column : values()) {
            if (column.titles.contains(normalized)) {
                return column;
            }
        }

        return null;
    }
}
