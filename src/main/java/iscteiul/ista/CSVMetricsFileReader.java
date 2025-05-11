package iscteiul.ista;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2>CSVMetricsFileReader</h2>
 *
 * <p>Utility class that loads {@code /parish-metrics.csv} (in {@code src/main/resources})
 * and converts each row into a {@link ParishMetrics} instance.</p>
 *
 * <p>Rows with parsing errors are skipped, logging a warning via SLF4J.</p>
 */
public final class CSVMetricsFileReader {

    private static final Logger log = LoggerFactory.getLogger(CSVMetricsFileReader.class);
    private static final String RESOURCE = "/parish-metrics.csv";

    /** Static-only utility class – no instances allowed. */
    private CSVMetricsFileReader() {
        // no-op
    }

    /**
     * Reads the CSV file from the classpath and returns a list of metrics.
     *
     * @return list of {@link ParishMetrics} (possibly empty if file missing/empty)
     */
    public static List<ParishMetrics> importData() {
        List<ParishMetrics> list = new ArrayList<>();

        // 1) Load the resource as stream
        try (InputStream is = CSVMetricsFileReader.class.getResourceAsStream(RESOURCE)) {
            if (is == null) {
                log.error("CSV resource not found: {}", RESOURCE);
                return list;
            }

            // 2) Use OpenCSV to parse lines
            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .withSkipLines(1) // skip CSV header row
                    .build()) {

                String[] row;
                int line = 2; // first data row
                while ((row = reader.readNext()) != null) {
                    try {
                        ParishMetrics pm = new ParishMetrics(
                                row[0].trim(),
                                Double.parseDouble(row[1]),
                                Double.parseDouble(row[2]),
                                Double.parseDouble(row[3]),
                                Double.parseDouble(row[4]),
                                Integer.parseInt(row[5]),
                                Integer.parseInt(row[6])
                        );
                        list.add(pm);
                    } catch (RuntimeException ex) {
                        log.warn("Skipping malformed line {} → {}", line, ex.getMessage());
                    }
                    line++;
                }
            }
        } catch (Exception e) {
            log.error("Error reading {}", RESOURCE, e);
        }

        log.info("Loaded {} parish-metric rows", list.size());
        return list;
    }
}
