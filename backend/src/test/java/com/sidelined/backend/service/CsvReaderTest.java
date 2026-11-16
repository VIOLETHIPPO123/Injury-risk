package com.sidelined.backend.service;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {

    private static CsvReader read(String csv) throws IOException {
        return new CsvReader(new StringReader(csv));
    }

    @Test
    void rows_areReadByHeaderName_untilTheEnd() throws IOException {
        CsvReader csv = read("id,name\n1,Ann\n2,Bob\n");

        CsvReader.Row first = csv.next();
        CsvReader.Row second = csv.next();

        assertEquals("1", first.get("id"));
        assertEquals("Ann", first.get("name"));
        assertEquals("Bob", second.get("name"));
        assertNull(csv.next());
    }

    @Test
    void quotedField_keepsItsCommasLineBreaksAndQuotes() throws IOException {
        CsvReader csv = read("name,url,note\n"
            + "Ann,\"https://x/f_auto,q_auto/a\",\"said \"\"hi\"\"\nand left\"\n");

        CsvReader.Row row = csv.next();

        assertEquals("https://x/f_auto,q_auto/a", row.get("url"));
        assertEquals("said \"hi\"\nand left", row.get("note"));
        assertNull(csv.next());
    }

    @Test
    void windowsLineEndings_andNoFinalLineBreak_areFine() throws IOException {
        CsvReader csv = read("id,name\r\n1,Ann\r\n2,Bob");

        assertEquals("Ann", csv.next().get("name"));
        assertEquals("Bob", csv.next().get("name"));
        assertNull(csv.next());
    }

    @Test
    void emptyAndMissingValues_areEmptyStrings() throws IOException {
        CsvReader csv = read("id,name,team\n1,,ARI\n2\n");

        assertEquals("", csv.next().get("name"));
        assertEquals("", csv.next().get("team"));
    }

    @Test
    void unknownColumn_throws() throws IOException {
        CsvReader.Row row = read("id\n1\n").next();

        assertThrows(IllegalStateException.class, () -> row.get("name"));
    }

    @Test
    void getIfPresent_columnTheFileDoesntHave_isEmpty() throws IOException {
        CsvReader.Row row = read("id,name\n1,Ann\n").next();

        assertEquals("Ann", row.getIfPresent("name"));
        assertEquals("", row.getIfPresent("date_modified"));
    }

    @Test
    void emptyFile_hasNoRows() throws IOException {
        assertNull(read("").next());
    }
}
