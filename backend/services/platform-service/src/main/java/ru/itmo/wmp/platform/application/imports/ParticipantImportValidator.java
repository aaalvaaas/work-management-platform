package ru.itmo.wmp.platform.application.imports;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ParticipantImportValidator {
    private static final Pattern ISU_NUMBER = Pattern.compile("[1-9][0-9]{5}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}");

    public List<String> validate(
        ParsedParticipantRow row,
        Map<String, Long> blocksByName,
        List<String> seenIsuNumbers
    ) {
        List<String> errors = new java.util.ArrayList<>();

        if (isBlank(row.isuNumber())) {
            errors.add("не заполнен номер ИСУ");
        } else if (!ISU_NUMBER.matcher(row.isuNumber()).matches()) {
            errors.add("номер ИСУ должен состоять из шести цифр");
        } else if (seenIsuNumbers.contains(row.isuNumber())) {
            errors.add("номер ИСУ встречается в файле несколько раз");
        }

        if (isBlank(row.lastName())) {
            errors.add("не заполнена фамилия");
        }

        if (isBlank(row.firstName())) {
            errors.add("не заполнено имя");
        }

        if (isBlank(row.email())) {
            errors.add("не заполнен email");
        } else if (!EMAIL.matcher(row.email()).matches()) {
            errors.add("некорректный email");
        }

        row.blockNames().stream()
            .filter(block -> !blocksByName.containsKey(block.toLowerCase(Locale.ROOT)))
            .forEach(block -> errors.add("в организации нет блока " + block));

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
