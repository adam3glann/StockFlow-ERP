package com.adam.warehouse.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal RFC-4180 compatible CSV writer for exportable reports. */
public final class CsvExportUtil {
    private CsvExportUtil() { }
    public static void write(Path path, List<List<String>> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            for (List<String> row : rows) {
                for (int column = 0; column < row.size(); column++) {
                    if (column > 0) writer.write(',');
                    writer.write(escape(row.get(column)));
                }
                writer.newLine();
            }
        }
    }
    private static String escape(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
