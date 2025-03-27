package iscteiul.ista;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading property data from CSV files. It includes:
 * <ul>
 *   <li>{@link #importData(String)} to read from a resource on the classpath (e.g. /Madeira-Moodle-1.1.csv)</li>
 *   <li>{@link #importDataFromFile(String)} to read from a normal file path on disk</li>
 * </ul>
 */
public class CSVFileReader {

    private static final Logger logger = LoggerFactory.getLogger(CSVFileReader.class);

    /**
     * Reads a CSV file from the classpath (e.g., /Madeira-Moodle-1.1.csv).
     * Uses getResourceAsStream, so the CSV must be in src/main/resources or
     * otherwise on the classpath.
     *
     * @param csvResourcePath e.g. "/Madeira-Moodle-1.1.csv"
     * @return a List of PropertyRecord, or empty if not found or parse error
     */
    public List<PropertyRecord> importData(String csvResourcePath) {
        InputStream csvStream = getClass().getResourceAsStream(csvResourcePath);
        if (csvStream == null) {
            logger.warn("Could not find CSV resource: {}", csvResourcePath);
            return new ArrayList<>();
        }
        logger.info("Reading CSV from resource path: {}", csvResourcePath);
        return parseCsvInputStream(csvStream, csvResourcePath);
    }

    /**
     * Reads a CSV file from a regular file path on the disk (not the classpath).
     *
     * @param filePath the path to the CSV file, e.g. "C:/data/properties.csv" or "target/exported-test-data.csv"
     * @return a List of PropertyRecord, or empty if file not found or parse errors
     */
    public List<PropertyRecord> importDataFromFile(String filePath) {
        File csvFile = new File(filePath);
        if (!csvFile.exists()) {
            logger.warn("CSV file not found at: {}", filePath);
            return new ArrayList<>();
        }
        logger.info("Reading CSV from filesystem path: {}", filePath);

        try (FileInputStream fis = new FileInputStream(csvFile)) {
            return parseCsvInputStream(fis, filePath);
        } catch (IOException e) {
            logger.error("Error opening CSV file: {}", filePath, e);
            return new ArrayList<>();
        }
    }

    /**
     * Shared logic to parse an InputStream using OpenCSV, converting each row into a PropertyRecord.
     */
    private List<PropertyRecord> parseCsvInputStream(InputStream inputStream, String sourceDesc) {
        List<PropertyRecord> propertyRecords = new ArrayList<>();

        try (Reader fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            try (CSVReader csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build()) {

                String[] row;
                int rowIndex = 0;

                while ((row = csvReader.readNext()) != null) {
                    // Skip header
                    if (rowIndex == 0) {
                        rowIndex++;
                        continue;
                    }
                    try {
                        int objectID = Integer.parseInt(row[0]);

                        String parcelIDString = row[1].replace(',', '.');
                        long parcelID = (long) Double.parseDouble(parcelIDString);

                        String parcelNumberString = row[2].replace(',', '.');
                        long parcelNumber = (long) Double.parseDouble(parcelNumberString);

                        double shapeLength = Double.parseDouble(row[3]);
                        double shapeArea = Double.parseDouble(row[4]);
                        String geometry = row[5];
                        int owner = Integer.parseInt(row[6]);

                        String parish = "NA".equals(row[7]) ? null : row[7];
                        String municipality = "NA".equals(row[8]) ? null : row[8];
                        String island = "NA".equals(row[9]) ? null : row[9];

                        PropertyRecord record = new PropertyRecord(objectID, parcelID, parcelNumber,
                                shapeLength, shapeArea, geometry, owner, parish, municipality, island);

                        propertyRecords.add(record);
                    } catch (NumberFormatException e) {
                        logger.warn("Skipping row {} in {} due to parse error: {}", rowIndex, sourceDesc, e.getMessage());
                    }
                    rowIndex++;
                }
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("Error reading/parsing CSV from {}: {}", sourceDesc, e.getMessage(), e);
        }

        logger.info("Finished reading CSV from {}. Total records loaded: {}", sourceDesc, propertyRecords.size());
        return propertyRecords;
    }
}
