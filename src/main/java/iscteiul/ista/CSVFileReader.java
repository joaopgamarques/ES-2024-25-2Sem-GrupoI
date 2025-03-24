package iscteiul.ista;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class responsible for reading property data from a CSV file (assumed to be in the
 * project's <em>resources</em> folder) and converting each row into a {@link PropertyRecord}.
 * <p>
 * <strong>Usage:</strong> call {@link #importData(String)} with the path to the CSV resource
 * (e.g. <code>"/Madeira-Moodle-1.1.csv"</code>). If the file exists, the class will parse each
 * row, converting numeric columns appropriately. Rows that fail parsing (invalid numeric data, etc.)
 * are skipped with a warning log message. The final list of {@link PropertyRecord}
 * objects is returned for further processing.
 * <p>
 * This class uses:
 * <ul>
 *   <li><strong>SLF4J</strong> for logging at different levels (INFO, WARN, DEBUG, ERROR);</li>
 *   <li><strong>OpenCSV</strong> for CSV parsing with a semicolon delimiter;</li>
 *   <li>UTF-8 encoding to read the input stream.</li>
 * </ul>
 */
public class CSVFileReader {

    /**
     * SLF4J logger for this class, used to log events, warnings, and errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(CSVFileReader.class);

    /**
     * Reads a CSV file from the given resource path and returns a list of {@link PropertyRecord} objects.
     * <p>
     * The method attempts to open the file as a resource in the same package or classpath. If the file
     * cannot be found, it logs a warning and returns an <em>empty</em> list. When reading the file:
     * <ul>
     *   <li>Row <code>0</code> is assumed to be the header row, but it is not logged or processed.</li>
     *   <li>Subsequent rows are parsed into property records. If any parse error occurs (e.g.,
     *   invalid integer/double), that row is skipped with a WARN log.</li>
     *   <li>The numeric columns are interpreted by converting commas to periods,
     *   then parsed as <code>double</code> or <code>long</code>.</li>
     *   <li>Columns containing <code>"NA"</code> are replaced with <code>null</code> for the
     *   corresponding string fields (parish, municipality, island).</li>
     * </ul>
     *
     * @param csvResourcePath the path to the CSV file inside resources
     *                        (e.g. <code>"/Madeira-Moodle-1.1.csv"</code>)
     * @return a list of {@link PropertyRecord} objects; empty if file not found or if no rows parsed
     */
    public List<PropertyRecord> importData(String csvResourcePath) {
        // This will accumulate the final list of parsed records.
        List<PropertyRecord> propertyRecords = new ArrayList<>();

        // Attempt to open the CSV as a resource (from src/main/resources, etc.).
        try (InputStream csvStream = getClass().getResourceAsStream(csvResourcePath)) {
            if (csvStream == null) {
                // Resource not found: log a warning and return empty list.
                logger.warn("Could not find CSV resource: {}", csvResourcePath);
                return propertyRecords;
            }

            // Log the start of reading at INFO level.
            logger.info("Reading CSV from resource path: {}", csvResourcePath);

            // Configure a CSVParser to use semicolon as delimiter.
            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();

            // Wrap the InputStream in an InputStreamReader with UTF-8,
            // then build a CSVReader that uses the parser.
            try (Reader fileReader = new InputStreamReader(csvStream, StandardCharsets.UTF_8);
                 CSVReader csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build()) {

                String[] row;  // Will hold each parsed row.
                int rowIndex = 0; // Keep track of which row (0 = header).

                // Read until no more rows are found.
                while ((row = csvReader.readNext()) != null) {
                    // If it's the first row, assume it's a header row and skip it.
                    if (rowIndex == 0) {
                        // No logging or processing for the header row.
                        rowIndex++;
                        continue;
                    }

                    // Try parsing the columns as numeric or string as needed.
                    try {
                        int objectID = Integer.parseInt(row[0]);

                        // Replace commas with periods for scientific notation or decimal points.
                        String parcelIDString = row[1].replace(',', '.');
                        long parcelID = (long) Double.parseDouble(parcelIDString);

                        String parcelNumberString = row[2].replace(',', '.');
                        long parcelNumber = (long) Double.parseDouble(parcelNumberString);

                        double shapeLength = Double.parseDouble(row[3]);
                        double shapeArea   = Double.parseDouble(row[4]);
                        String geometry    = row[5];
                        int owner          = Integer.parseInt(row[6]);

                        // Convert "NA" to null.
                        String parish       = "NA".equals(row[7]) ? null : row[7];
                        String municipality = "NA".equals(row[8]) ? null : row[8];
                        String island       = "NA".equals(row[9]) ? null : row[9];

                        // Create the immutable PropertyRecord.
                        PropertyRecord propertyRecord = new PropertyRecord(objectID, parcelID, parcelNumber, shapeLength,
                                shapeArea, geometry, owner, parish, municipality, island);
                        propertyRecords.add(propertyRecord);

                        // Log at debug level for row-level detail, if desired:
                        // logger.debug("Parsed record #{}: {}", rowIndex, propertyRecord);

                    } catch (NumberFormatException e) {
                        // Log a warning if a row fails numeric parsing.
                        logger.warn("Skipping row {} due to parse error: {}", rowIndex, e.getMessage());
                    }
                    rowIndex++;
                }
            }
        } catch (IOException | CsvValidationException e) {
            // If there's an I/O or CSV-specific error, log it at ERROR level.
            logger.error("Error reading/parsing CSV: {}", e.getMessage(), e);
        }

        // Summarize final count of records loaded.
        logger.info("Finished reading CSV. Total records loaded: {}", propertyRecords.size());
        return propertyRecords;
    }
}
