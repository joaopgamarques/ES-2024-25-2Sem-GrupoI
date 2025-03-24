package iscteiul.ista;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CSVFileReader} using the sample test-data.csv file.
 */
class CSVFileReaderTest {

    /**
     * Tests that the CSVFileReader correctly parses two rows from test-data.csv,
     * creating two PropertyRecord objects with the expected field values.
     */
    @Test
    void testImportDataWithValidFile() {
        CSVFileReader reader = new CSVFileReader();
        // This assumes you have test-data.csv in src/test/resources/
        List<PropertyRecord> records = reader.importData("/test-data.csv");

        // We expect exactly 2 records in our sample file
        assertEquals(2, records.size(), "Should parse exactly 2 records from test-data.csv");

        // Check first record
        PropertyRecord first = records.get(0);
        assertEquals(1, first.getObjectID(),       "objectID for first row");
        assertEquals(7343148L, first.getParcelID(), "parcelID for first row");
        assertEquals(2996240000000L, first.getParcelNumber(), "parcelNumber for first row");
        assertEquals(57.2469341921808, first.getShapeLength(), 1e-9, "shapeLength for first row");
        assertEquals(202.05981432070362, first.getShapeArea(), 1e-9, "shapeArea for first row");
        assertEquals(93, first.getOwner(), "owner for first row");
        assertEquals("Arco da Calheta", first.getParish(), "parish for first row");
        assertEquals("Calheta", first.getMunicipality(), "municipality for first row");
        assertEquals("Ilha da Madeira (Madeira)", first.getIsland(), "island for first row");

        // Check second record
        PropertyRecord second = records.get(1);
        assertEquals(2, second.getObjectID(),       "objectID for second row");
        assertEquals(7344660L, second.getParcelID(), "parcelID for second row");
        assertEquals(2996220000000L, second.getParcelNumber(), "parcelNumber for second row");
        assertEquals(55.6380071234, second.getShapeLength(), 1e-9, "shapeLength for second row");
        assertEquals(151.7638755432, second.getShapeArea(), 1e-9, "shapeArea for second row");
        assertEquals(68, second.getOwner(), "owner for second row");
        assertEquals("Arco da Calheta", second.getParish(), "parish for second row");
        assertEquals("Calheta", second.getMunicipality(), "municipality for second row");
        assertEquals("Ilha da Madeira (Madeira)", second.getIsland(), "island for second row");
    }

    /**
     * Tests that the CSVFileReader returns an empty list (and prints a message) if the file does not exist.
     */
    @Test
    void testImportDataWithMissingFile() {
        CSVFileReader reader = new CSVFileReader();
        // Provide a path that doesn't exist
        List<PropertyRecord> records = reader.importData("/nonexistent.csv");

        // Expect an empty list since the file doesn't exist
        assertTrue(records.isEmpty(), "Records should be empty if the CSV file doesn't exist.");
    }
}
