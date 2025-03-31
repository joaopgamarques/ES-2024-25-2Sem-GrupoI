package iscteiul.ista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
//        CSVFileReader csvFileReader = new CSVFileReader();
//
//        // Provide the path to your CSV file in resources (e.g. "/Madeira-Moodle-1.1.csv").
//        List<PropertyRecord> propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.1.csv");
//
//        // Log how many records we loaded.
//        logger.info("Total records loaded: {}", propertyRecords.size());
//
//        // If we have at least one record, log the first one.
//        if (!propertyRecords.isEmpty()) {
//            logger.info("First record: {}", propertyRecords.get(0));
//        }
//
//        // Example usage of a method that filters properties by a given owner.
//        List<PropertyRecord> ownedProperties = PropertyUtils.findByOwner(propertyRecords, propertyRecords.get(0).getOwner());
//        ownedProperties.forEach(propertyRecord -> logger.info("Owned property: {}", propertyRecord));
//        logger.info("Total owned properties: {}", ownedProperties.size());
//
//        try {
//            CSVFileWriter.exportData("exported-data.csv", ownedProperties);
//            logger.info("Successfully exported {} records to 'exported-data.csv'", ownedProperties.size());
//        } catch (IOException e) {
//            logger.error("Error while exporting data to CSV.", e);
//        }

        CSVFileReader csvFileReader = new CSVFileReader();
        String chosenParish = "Arco da Calheta";
        List<PropertyRecord> propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.1.csv");
        List<PropertyRecord> parishSubset = propertyRecords.stream()
                .filter(pr -> chosenParish.equals(pr.getParish()))
                .collect(Collectors.toList());
        PropertyGraph propertyGraph = new PropertyGraph();
        propertyGraph.buildGraph(parishSubset);
        GraphVisualization.visualizeGraph(propertyGraph);
    }
}
