package iscteiul.ista;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link CSVFileWriter}, ensuring it correctly writes a list of
 * {@link PropertyRecord} objects to a CSV file and that the data can be accurately
 * read back using {@link CSVFileReader}.
 *
 * <p>This class covers both writing and re-reading records, including complex geometry
 * like {@code MULTIPOLYGON}. It validates field integrity and confirms that semicolon
 * delimiters are consistently interpreted during export (in {@code CSVFileWriter}) and
 * import (in {@code CSVFileReader}).
 *
 * <p>These tests rely on consistent CSV formatting (semicolon delimiter) in both classes.
 * Failing to match delimiters typically leads to parse errors or skipped rows.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CSVFileWriterTest {

    /**
     * The file path to which sample data will be exported and later reimported.
     * Typically placed in {@code target} to keep generated files out of source directories.
     */
    private static final String TEST_OUTPUT_PATH = "target/exported-test-data.csv";

    /**
     * A list of {@link PropertyRecord} objects used across all test methods
     * to verify correctness in writing and re-reading operations.
     */
    private static List<PropertyRecord> sampleRecords;

    /**
     * Initializes sample data before all tests. At least one {@code MULTIPOLYGON} record
     * and one simpler {@code POLYGON} record are included to ensure complex geometry can be handled.
     */
    @BeforeAll
    static void setUpBeforeAll() {
        sampleRecords = new ArrayList<>();

        // Record #1: Complex geometry (MULTIPOLYGON)
        sampleRecords.add(new PropertyRecord(
                1,
                7343148L,
                2996240000000L,
                57.2469341921808,
                202.05981432070362,
                "MULTIPOLYGON (((299218.5203999998 3623637.4791, 299218.5033999998 3623637.4715, 299218.04000000004 3623638.4800000004, 299232.7400000002 3623644.6799999997, 299236.6233999999 3623637.1974, 299236.93709999975 3623636.7885999996, 299238.04000000004 3623633.4800000004, 299222.63999999966 3623627.1799999997, 299218.5203999998 3623637.4791)))",
                93,
                "Arco da Calheta",
                "Calheta",
                "Ilha da Madeira (Madeira)"
        ));

        // Record #2: Simpler POLYGON geometry
        sampleRecords.add(new PropertyRecord(
                2,
                1234567L,
                111222333444L,
                75.0,
                300.0,
                "POLYGON((1 0, 2 0, 2 1, 1 1, 1 0))",
                111,
                "SomeParish",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        ));
    }

    /**
     * Tests whether {@link CSVFileWriter} can export the sample data to a file
     * without errors, and verifies the file is non-empty.
     */
    @Test
    @Order(1)
    void testExportData() {
        try {
            // Attempt to export the sample records
            CSVFileWriter.exportData(TEST_OUTPUT_PATH, sampleRecords);
        } catch (IOException e) {
            fail("IOException while exporting data: " + e.getMessage());
        }

        File outFile = new File(TEST_OUTPUT_PATH);
        assertTrue(outFile.exists(), "The exported CSV file should exist.");
        assertTrue(outFile.length() > 0, "The exported CSV file should not be empty.");
    }

    /**
     * Tests reimporting the CSV written by {@code CSVFileWriter} using
     * {@link CSVFileReader#importDataFromFile(String)}. Compares each field
     * in the reimported data to the original {@link PropertyRecord} objects.
     */
    @Test
    @Order(2)
    void testReimportExportedData() {
        CSVFileReader reader = new CSVFileReader();
        List<PropertyRecord> reimportedRecords = reader.importDataFromFile(TEST_OUTPUT_PATH);

        // Ensure same number of records
        assertEquals(sampleRecords.size(), reimportedRecords.size(),
                "Number of records read from the exported CSV should match the original.");

        // Compare fields in each record
        for (int i = 0; i < sampleRecords.size(); i++) {
            PropertyRecord original = sampleRecords.get(i);
            PropertyRecord reimported = reimportedRecords.get(i);

            assertEquals(original.getObjectID(),       reimported.getObjectID());
            assertEquals(original.getParcelID(),       reimported.getParcelID());
            assertEquals(original.getParcelNumber(),   reimported.getParcelNumber());
            assertEquals(original.getShapeLength(),    reimported.getShapeLength(), 1e-9);
            assertEquals(original.getShapeArea(),      reimported.getShapeArea(),   1e-9);
            assertEquals(original.getGeometry(),       reimported.getGeometry());
            assertEquals(original.getOwner(),          reimported.getOwner());
            assertEquals(original.getParish(),         reimported.getParish());
            assertEquals(original.getMunicipality(),   reimported.getMunicipality());
            assertEquals(original.getIsland(),         reimported.getIsland());
        }
    }

    /**
     * Cleans up the exported CSV file after all tests have finished.
     * Helps keep the {@code target} directory tidy by removing test artifacts.
     */
    @AfterAll
    static void tearDownAfterAll() {
        File outFile = new File(TEST_OUTPUT_PATH);
        if (outFile.exists()) {
            outFile.delete();
        }
    }
}
