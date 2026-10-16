package ru.itmo.wmp.platform.infrastructure.imports;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.imports.ParsedParticipantRow;
import ru.itmo.wmp.platform.application.imports.ParticipantListParser;
import ru.itmo.wmp.platform.exception.imports.ImportFileException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SpreadsheetParticipantListParser implements ParticipantListParser {
    private static final String BOM = "﻿";
    private static final int HEADER_ROW = 0;

    @Override
    public List<ParsedParticipantRow> parse(String filename, byte[] content) {
        if (filename == null) {
            throw new ImportFileException("File name is missing");
        }

        String name = filename.toLowerCase(Locale.ROOT);

        if (name.endsWith(".csv")) {
            return parseCsv(content);
        }

        if (name.endsWith(".xlsx")) {
            return parseExcel(content);
        }

        throw new ImportFileException("Only .csv and .xlsx files are supported");
    }

    private List<ParsedParticipantRow> parseCsv(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);

        if (text.startsWith(BOM)) {
            text = text.substring(1);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(detectDelimiter(text))
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .get();

        try (Reader reader = new InputStreamReader(
            new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
        ); CSVParser parser = CSVParser.parse(reader, format)) {
            List<CSVRecord> records = parser.getRecords();

            if (records.isEmpty()) {
                throw new ImportFileException("File is empty");
            }

            Map<ParticipantColumn, Integer> columns = mapColumns(toList(records.getFirst()));

            List<ParsedParticipantRow> rows = new ArrayList<>();

            for (int i = 1; i < records.size(); i++) {
                List<String> values = toList(records.get(i));

                if (isBlank(values)) {
                    continue;
                }

                rows.add(row(i + 1, columns, values));
            }

            return rows;
        } catch (IOException exception) {
            throw new ImportFileException("Cannot read csv file", exception);
        }
    }

    private List<ParsedParticipantRow> parseExcel(byte[] content) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(HEADER_ROW);

            if (header == null) {
                throw new ImportFileException("File is empty");
            }

            DataFormatter formatter = new DataFormatter();

            Map<ParticipantColumn, Integer> columns = mapColumns(cells(header, formatter));

            List<ParsedParticipantRow> rows = new ArrayList<>();

            for (int i = HEADER_ROW + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                List<String> values = cells(row, formatter);

                if (isBlank(values)) {
                    continue;
                }

                rows.add(row(i + 1, columns, values));
            }

            return rows;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ImportFileException importFileException) {
                throw importFileException;
            }

            throw new ImportFileException("Cannot read excel file", exception);
        }
    }

    private char detectDelimiter(String text) {
        String firstLine = text.lines().findFirst().orElse("");

        long semicolons = firstLine.chars().filter(symbol -> symbol == ';').count();
        long commas = firstLine.chars().filter(symbol -> symbol == ',').count();

        return semicolons > commas ? ';' : ',';
    }

    private Map<ParticipantColumn, Integer> mapColumns(List<String> headers) {
        Map<ParticipantColumn, Integer> columns = new EnumMap<>(ParticipantColumn.class);

        for (int i = 0; i < headers.size(); i++) {
            ParticipantColumn column = ParticipantColumn.of(headers.get(i));

            if (column != null) {
                columns.putIfAbsent(column, i);
            }
        }

        List<String> missing = Arrays.stream(ParticipantColumn.values())
            .filter(ParticipantColumn::isRequired)
            .filter(column -> !columns.containsKey(column))
            .map(ParticipantColumn::title)
            .toList();

        if (!missing.isEmpty()) {
            throw new ImportFileException("Missing required columns: " + String.join(", ", missing));
        }

        return columns;
    }

    private ParsedParticipantRow row(
        int rowNumber,
        Map<ParticipantColumn, Integer> columns,
        List<String> values
    ) {
        return new ParsedParticipantRow(
            rowNumber,
            value(columns, values, ParticipantColumn.ISU_NUMBER),
            value(columns, values, ParticipantColumn.LAST_NAME),
            value(columns, values, ParticipantColumn.FIRST_NAME),
            value(columns, values, ParticipantColumn.PATRONYMIC),
            value(columns, values, ParticipantColumn.EMAIL),
            value(columns, values, ParticipantColumn.POSITION),
            blocks(value(columns, values, ParticipantColumn.BLOCKS))
        );
    }

    private String value(
        Map<ParticipantColumn, Integer> columns,
        List<String> values,
        ParticipantColumn column
    ) {
        Integer index = columns.get(column);

        if (index == null || index >= values.size()) {
            return null;
        }

        String value = values.get(index).trim();

        return value.isEmpty() ? null : value;
    }

    private List<String> blocks(String value) {
        if (value == null) {
            return List.of();
        }

        return Arrays.stream(value.split("[;,]"))
            .map(String::trim)
            .filter(block -> !block.isEmpty())
            .toList();
    }

    private List<String> cells(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();

        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }

        return values;
    }

    private List<String> toList(CSVRecord record) {
        List<String> values = new ArrayList<>();

        record.forEach(values::add);

        return values;
    }

    private boolean isBlank(List<String> values) {
        return values.stream().allMatch(value -> value == null || value.isBlank());
    }
}
