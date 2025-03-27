package iscteiul.ista;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * A utility class responsible for exporting a list of {@link PropertyRecord}
 * objects into a CSV file using semicolons as the delimiter.
 *
 * <p>Typical usage:
 * <pre>{@code
 * List<PropertyRecord> records = ...;
 * try {
 *     CSVFileWriter.exportData("target/exported-data.csv", records);
 * } catch (IOException e) {
 *     // Handle error
 * }
 * }</pre>
 * <p>The resulting CSV file will include a header row and one row per
 * {@code PropertyRecord}, ensuring {@code null} string fields are written
 * as empty strings (i.e., "").
 */
public final class CSVFileWriter {

    /**
     * Private constructor to prevent instantiation,
     * as this is intended to be a static utility class.
     */
    private CSVFileWriter() {
        // no-op
    }

    /**
     * Writes the given list of {@link PropertyRecord} objects into a CSV file
     * at the specified path, including a header row. Uses semicolons as the
     * delimiter to align with a semicolon-based {@code CSVFileReader}.
     *
     * @param outputCsvPath the file path where the CSV should be saved (e.g. "target/exported-data.csv")
     * @param records the list of {@link PropertyRecord} objects to write
     * @throws IOException if there's an error writing to or creating the file
     */
    public static void exportData(String outputCsvPath, List<PropertyRecord> records) throws IOException {
        // Use the legacy CSVWriter constructor to specify a semicolon delimiter
        try (FileWriter fileWriter = new FileWriter(outputCsvPath);
             CSVWriter writer = new CSVWriter(
                     fileWriter,
                     ';',                          // delimiter
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END
             )) {

            // Write header row
            writer.writeNext(new String[] {
                    "objectID", "parcelID", "parcelNumber", "shapeLength",
                    "shapeArea", "geometry", "owner", "parish",
                    "municipality", "island"
            });

            // Write each PropertyRecord as a row, replacing null fields with ""
            for (PropertyRecord pr : records) {
                writer.writeNext(new String[] {
                        String.valueOf(pr.getObjectID()),
                        String.valueOf(pr.getParcelID()),
                        String.valueOf(pr.getParcelNumber()),
                        String.valueOf(pr.getShapeLength()),
                        String.valueOf(pr.getShapeArea()),
                        pr.getGeometry() == null ? "" : pr.getGeometry(),
                        String.valueOf(pr.getOwner()),
                        pr.getParish() == null ? "" : pr.getParish(),
                        pr.getMunicipality() == null ? "" : pr.getMunicipality(),
                        pr.getIsland() == null ? "" : pr.getIsland()
                });
            }
        }
    }
}
