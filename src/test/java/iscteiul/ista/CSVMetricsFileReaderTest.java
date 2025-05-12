package iscteiul.ista;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-test that verifies {@link CSVMetricsFileReader#importData()} correctly
 * parses {@code parish-metrics.csv} and populates expected values.
 */
class CSVMetricsFileReaderTest {

    @Test
    void importData_parsesCsvCorrectly() {
        List<ParishMetrics> metrics = CSVMetricsFileReader.importData();
        assertFalse(metrics.isEmpty(), "CSV should yield at least one row");

        // Example: Checking if "Caniço" row is present
        ParishMetrics canico = metrics.stream()
                .filter(m -> m.parishName().equals("Caniço"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Row for Caniço missing"));

        // Validate some known fields from the CSV snippet
        assertEquals(12.0, canico.distanceAirportKm(), 0.001);
        assertEquals(2200.0, canico.averagePriceEuroM2(), 0.1);
        assertEquals(5, canico.infrastructureQualityIdx());
    }
}
