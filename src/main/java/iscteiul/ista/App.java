package iscteiul.ista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * The main entry point of the ES-Project-TerritoryManagement application.
 * <p>
 * This class demonstrates how to load a CSV file using {@link CSVFileReader},
 * then logs the number of records and an example of geometry checks using
 * {@link GeometryUtils}.
 */
public class App {

    /**
     * SLF4J logger for the App class.
     */
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Main method: loads the CSV, prints the number of records.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        // Create the CSV reader.
        CSVFileReader csvFileReader = new CSVFileReader();

        // Provide the path to your CSV file in resources (e.g. "/Madeira-Moodle-1.1.csv").
        List<PropertyRecord> propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.1.csv");

        // Log how many records we loaded.
        logger.info("Total records loaded: {}", propertyRecords.size());

        // If we have at least one record, log the first one.
        if (!propertyRecords.isEmpty()) {
            logger.info("First record: {}", propertyRecords.get(0));
        }

        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp2 = "MULTIPOLYGON(((4 4,9 4,9 9,4 9,4 4)))";

        boolean adjacent  = GeometryUtils.areAdjacent(mp1, mp2);
        boolean intersect = GeometryUtils.doIntersect(mp1, mp2);
        boolean disjoint  = GeometryUtils.areDisjoint(mp1, mp2);

        logger.info("mp1 & mp2 -> adjacent? {}, intersect? {}, disjoint? {}", adjacent, intersect, disjoint);
    }
}
