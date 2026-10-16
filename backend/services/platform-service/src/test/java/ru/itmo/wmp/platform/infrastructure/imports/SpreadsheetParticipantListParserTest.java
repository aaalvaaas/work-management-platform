package ru.itmo.wmp.platform.infrastructure.imports;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import ru.itmo.wmp.platform.application.imports.ParsedParticipantRow;
import ru.itmo.wmp.platform.exception.imports.ImportFileException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpreadsheetParticipantListParserTest {
    private final SpreadsheetParticipantListParser parser = new SpreadsheetParticipantListParser();

    @Test
    void shouldParseCommaSeparatedCsv() {
        String csv = """
            Номер ИСУ,Фамилия,Имя,Отчество,Email,Должность,Блоки
            465824,Иванов,Иван,Иванович,ivanov@niuitmo.ru,Аналитик,Разработка
            """;

        List<ParsedParticipantRow> rows = parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, rows.size());

        ParsedParticipantRow row = rows.getFirst();

        assertEquals(2, row.rowNumber());
        assertEquals("465824", row.isuNumber());
        assertEquals("Иванов", row.lastName());
        assertEquals("Иван", row.firstName());
        assertEquals("Иванович", row.patronymic());
        assertEquals("ivanov@niuitmo.ru", row.email());
        assertEquals("Аналитик", row.position());
        assertEquals(List.of("Разработка"), row.blockNames());
    }

    @Test
    void shouldParseSemicolonSeparatedCsvWithBom() {
        String csv = "﻿Номер ИСУ;Фамилия;Имя;Email\n465824;Иванов;Иван;ivanov@niuitmo.ru\n";

        List<ParsedParticipantRow> rows = parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, rows.size());
        assertEquals("465824", rows.getFirst().isuNumber());
        assertNull(rows.getFirst().patronymic());
        assertTrue(rows.getFirst().blockNames().isEmpty());
    }

    @Test
    void shouldSplitSeveralBlocks() {
        String csv = """
            Номер ИСУ,Фамилия,Имя,Email,Блоки
            465824,Иванов,Иван,ivanov@niuitmo.ru,Разработка; Дизайн
            """;

        List<ParsedParticipantRow> rows = parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("Разработка", "Дизайн"), rows.getFirst().blockNames());
    }

    @Test
    void shouldSkipEmptyLines() {
        String csv = """
            Номер ИСУ,Фамилия,Имя,Email
            465824,Иванов,Иван,ivanov@niuitmo.ru
            ,,,
            465825,Петров,Пётр,petrov@niuitmo.ru
            """;

        List<ParsedParticipantRow> rows = parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, rows.size());
    }

    @Test
    void shouldAcceptEnglishHeaders() {
        String csv = """
            isu,last_name,first_name,email
            465824,Ivanov,Ivan,ivanov@niuitmo.ru
            """;

        List<ParsedParticipantRow> rows = parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals("465824", rows.getFirst().isuNumber());
    }

    @Test
    void shouldRejectFileWithoutRequiredColumns() {
        String csv = """
            Фамилия,Имя
            Иванов,Иван
            """;

        ImportFileException exception = assertThrows(
            ImportFileException.class,
            () -> parser.parse("list.csv", csv.getBytes(StandardCharsets.UTF_8))
        );

        assertTrue(exception.getMessage().contains("номер ису"));
        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    void shouldRejectUnsupportedFormat() {
        assertThrows(
            ImportFileException.class,
            () -> parser.parse("list.pdf", new byte[]{1, 2, 3})
        );
    }

    @Test
    void shouldParseExcelKeepingIsuNumberAsText() throws Exception {
        byte[] content = excel();

        List<ParsedParticipantRow> rows = parser.parse("list.xlsx", content);

        assertEquals(1, rows.size());
        assertEquals("465824", rows.getFirst().isuNumber());
        assertEquals("Иванов", rows.getFirst().lastName());
        assertEquals(List.of("Разработка"), rows.getFirst().blockNames());
    }

    private byte[] excel() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Участники");

            Row header = sheet.createRow(0);

            header.createCell(0).setCellValue("Номер ИСУ");
            header.createCell(1).setCellValue("Фамилия");
            header.createCell(2).setCellValue("Имя");
            header.createCell(3).setCellValue("Email");
            header.createCell(4).setCellValue("Блоки");

            Row row = sheet.createRow(1);

            row.createCell(0).setCellValue(465824);
            row.createCell(1).setCellValue("Иванов");
            row.createCell(2).setCellValue("Иван");
            row.createCell(3).setCellValue("ivanov@niuitmo.ru");
            row.createCell(4).setCellValue("Разработка");

            workbook.write(output);

            return output.toByteArray();
        }
    }
}
