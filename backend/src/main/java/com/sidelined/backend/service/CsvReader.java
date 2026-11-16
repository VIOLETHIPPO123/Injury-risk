package com.sidelined.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reads a CSV file one row at a time, so a big download never has to fit in memory and the
// caller can stop early. The first row is the header. Handles quoted fields: commas, line
// breaks and doubled quotes ("") inside quotes are part of the value.
final class CsvReader {

    private final BufferedReader in;
    private final Map<String, Integer> columns = new HashMap<>();

    CsvReader(Reader reader) throws IOException {
        this.in = new BufferedReader(reader);
        List<String> header = readRecord();
        if (header != null) {
            for (int i = 0; i < header.size(); i++) {
                columns.put(header.get(i), i);
            }
        }
    }

    // The next row, or null at the end of the file.
    Row next() throws IOException {
        List<String> values = readRecord();
        return values == null ? null : new Row(values);
    }

    final class Row {
        private final List<String> values;

        private Row(List<String> values) {
            this.values = values;
        }

        // The value under a header name; "" if this row is short. A header the file doesn't
        // have is an error: the file's format changed and the caller must not guess.
        String get(String column) {
            Integer index = columns.get(column);
            if (index == null) {
                throw new IllegalStateException("CSV has no column \"" + column + "\"");
            }
            return index < values.size() ? values.get(index) : "";
        }
    }

    private List<String> readRecord() throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean readAnything = false;

        int c;
        while ((c = in.read()) != -1) {
            readAnything = true;
            if (quoted) {
                if (c != '"') {
                    field.append((char) c);
                } else if (nextIsQuote()) {
                    field.append('"');
                } else {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                break;
            } else if (c != '\r') {
                field.append((char) c);
            }
        }
        if (!readAnything) {
            return null;
        }
        fields.add(field.toString());
        return fields;
    }

    // Inside quotes, "" is one literal quote. Consumes the second quote only if it is there.
    private boolean nextIsQuote() throws IOException {
        in.mark(1);
        if (in.read() == '"') {
            return true;
        }
        in.reset();
        return false;
    }
}
