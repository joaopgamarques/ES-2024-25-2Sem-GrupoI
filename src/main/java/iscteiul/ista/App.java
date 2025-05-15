package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main entry point of the ES-Project-TerritoryManagement application.
 */
public final class App {

    /**
     * SLF4J logger for the App class.
     */
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * A private static list storing all {@link PropertyRecord} objects loaded
     * from the CSV (e.g. "/Madeira-Moodle-1.2.csv"). This allows other methods
     * within {@code App} to access the full dataset, while preventing external
     * classes from directly modifying it.
     *
     * <p>We assign this list once in {@link #main(String[])} after reading the CSV,
     * and it remains available for the lifetime of the application.</p>
     */
    private static List<PropertyRecord> propertyRecords = null;

    /**
     * Private static reference to the #11074 property, once loaded.
     * We'll assign it in main() after reading from CSV. Other classes
     * can read it via {@link #getFunchalPropertyRecord()} but cannot overwrite it.
     */
    private static PropertyRecord funchalPropertyRecord = null;

    /**
     * Private static reference to the #11517 property, once loaded.
     * We'll assign it in main() after reading from CSV. Other classes
     * can read it via {@link #getMachicoPropertyRecord()} but cannot overwrite it.
     */
    private static PropertyRecord machicoPropertyRecord = null;

    /**
     * Provides read-only access to the entire list of {@link PropertyRecord} objects
     * loaded from the CSV. If the CSV was never read or an error occurred, this might
     * be {@code null} or an empty list. Callers must not modify this list in place.
     *
     * @return the loaded list of PropertyRecord objects, or {@code null} if not yet assigned
     */
    public static List<PropertyRecord> getPropertyRecords() {
        return propertyRecords;
    }

    /**
     * Sets the property records list,
     * so unit tests can simulate different scenarios.
     */
    public static void setPropertyRecords(List<PropertyRecord> records) {
        propertyRecords = records;
    }

    /**
     * Provides read-only access to our Funchal Sé reference property.
     * If #11074 wasn't found in the CSV, this returns null.
     */
    public static PropertyRecord getFunchalPropertyRecord() {
        return funchalPropertyRecord;
    }

    /**
     * Sets the Funchal property record (objectID=11074),
     * so unit tests can simulate different scenarios.
     */
    public static void setFunchalPropertyRecord(PropertyRecord record) {
        funchalPropertyRecord = record;
    }

    /**
     * Provides read-only access to our Machico reference property.
     * If #11517 wasn't found in the CSV, this returns null.
     */
    public static PropertyRecord getMachicoPropertyRecord() {
        return machicoPropertyRecord;
    }

    /**
     * Sets the Machico property record (objectID=11517),
     * so unit tests can simulate different scenarios.
     */
    public static void setMachicoPropertyRecord(PropertyRecord record) {
        machicoPropertyRecord = record;
    }

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * Since this class is designed to be run from its static {@code main} method
     * (and contains no instance fields), no objects should ever be created.
     *
     * @throws AssertionError always, since instantiation is not allowed
     */
    private App() {
        throw new AssertionError("Utility class - do not instantiate.");
    }

    /**
     * Main method: loads the CSV, filters by parish, builds a Graph,
     * picks a random property from the subset, and compares adjacency using two methods.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        // 1. Read proprieties CSV
        CSVFileReader csvFileReader = new CSVFileReader();
        propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.2.csv");
        logger.info("Total records loaded: {}", propertyRecords.size());
        propertyRecords.removeIf(pr -> pr.getParish() == null || pr.getParish().isBlank());

        // 1a. Print distinct parishes and municipalities.
        Set<String> distinctParishes = PropertyUtils.getDistinctParishes(propertyRecords);
        Set<String> distinctMunicipalities = PropertyUtils.getDistinctMunicipalities(propertyRecords);
        logger.info("Distinct Parishes: {}", distinctParishes);
        logger.info("Distinct Municipalities: {}", distinctMunicipalities);

        // 1b) Find objectID=11074 in the data, copy it into our private static field
        funchalPropertyRecord = propertyRecords.stream()
                .filter(pr -> pr.getObjectID() == 11074)
                .findFirst()
                .map(original -> new PropertyRecord(original.getObjectID(), original.getParcelID(),
                        original.getParcelNumber(), original.getShapeLength(), original.getShapeArea(), original.getGeometry(),
                        original.getOwner(), original.getParish(), original.getMunicipality(), original.getIsland()))
                .orElse(null);

        // 1c) Find objectID=11517 in the data, copy it into our private static field
        machicoPropertyRecord = propertyRecords.stream()
                .filter(pr -> pr.getObjectID() == 11517)
                .findFirst()
                .map(original -> new PropertyRecord(original.getObjectID(), original.getParcelID(),
                        original.getParcelNumber(), original.getShapeLength(), original.getShapeArea(), original.getGeometry(),
                        original.getOwner(), original.getParish(), original.getMunicipality(), original.getIsland()))
                .orElse(null);

        // 1d. Read metrics CSV
        List<ParishMetrics> metrics = CSVMetricsFileReader.importData();
        logger.info("Total metrics loaded: {}", metrics.size());

        // 1c. Print each parish metric (distance, price, etc.)
        for (ParishMetrics pm : metrics) {
            System.out.printf("%-30s Distance Airport [km]=%.1f | Distance Funchal Sé [km]=%.1f | Price [€/m²]=%.0f | " +
                            "Population Density [Hab./km²]=%d | Infrastructure Quality Index=%d%n",
                    pm.parishName(),
                    pm.distanceAirportKm(),
                    pm.distanceFunchalSeKm(),
                    pm.averagePriceEuroM2(),
                    pm.populationDensityHabKm2(),
                    pm.infrastructureQualityIdx());
        }

        // 2. Filter to a chosen parish.
        String chosenParish = "Machico";
        List<PropertyRecord> parishSubset = propertyRecords.stream()
                .filter(pr -> chosenParish.equals(pr.getParish()))
                .collect(Collectors.toList());
        logger.info("Records in parish '{}': {}", chosenParish, parishSubset.size());

        // 2a. Calculate the average area of properties in the chosen parish (no adjacency grouping).
        List<PropertyRecord> parishProperties = PropertyUtils.findByParish(propertyRecords, chosenParish);
        double averageArea = PropertyUtils.calculateAverageArea(parishProperties);
        logger.info("Records in parish '{}': {}", chosenParish, parishProperties.size());
        logger.info("Average area (no adjacency grouping) in '{}' [ha]: {}", chosenParish, String.format("%.2f", averageArea));

        // 3. Build the custom (O(N²)) Graph from the parish subset.
        Graph propertyGraph = new Graph(parishSubset);

        // 4. Pick a random PropertyRecord from the subset.
        if (parishSubset.isEmpty()) {
            logger.warn("No records found in parish '{}'. Exiting.", chosenParish);
            return;
        }

        // 4a. For each property in parishSubset, print its neighbors
        for (PropertyRecord record : parishSubset) {
            int objectID = record.getObjectID();
            List<Graph.GraphNode> neighbors = propertyGraph.getNeighbors(objectID);
            if (neighbors.isEmpty()) {
                System.out.println("Property ID=" + objectID
                                + ", Area [ha]=" + String.format("%.2f", record.getShapeArea())
                                + ", Owner=" + record.getOwner() + " has no neighbors.");
            } else {
                System.out.println("Neighbors of Property ID=" + objectID
                        + ", Area [ha]=" + String.format("%.2f", record.getShapeArea()) + ", Owner=" + record.getOwner() + ":");
                for (Graph.GraphNode neighbor : neighbors) {
                    System.out.println("  -> objectID=" + neighbor.getObjectID()
                            + ", Area [ha]=" + String.format("%.2f", neighbor.getShapeArea())
                            + ", Owner=" + neighbor.getOwner());
                }
            }
        }

        Random random = new Random();
        int randomIndex = random.nextInt(parishSubset.size());
        PropertyRecord randomProperty = parishSubset.get(randomIndex);
        int testObjectID = randomProperty.getObjectID();

        logger.info("Randomly chosen objectID {} from parish '{}'", testObjectID, chosenParish);

        // 4a. Parse geometry and print centroid + other attributes for just this node.
        WKTReader wktReader = new WKTReader();
        String wkt = randomProperty.getGeometry();
        if (wkt != null) {
            try {
                Geometry geom = wktReader.read(wkt);
                Point centroid = geom.getCentroid();
                double cx = centroid.getX();
                double cy = centroid.getY();

                logger.info("Selected node attributes:");
                logger.info(" -> Object ID      = {}", randomProperty.getObjectID());
                logger.info(" -> Parcel ID      = {}", randomProperty.getParcelID());
                logger.info(" -> Perimeter [km] = {}", randomProperty.getShapeLength());
                logger.info(" -> Area [ha]      = {}", randomProperty.getShapeArea());
                logger.info(" -> Owner          = {}", randomProperty.getOwner());
                logger.info(" -> Parish         = {}", randomProperty.getParish());
                logger.info(" -> Municipality   = {}", randomProperty.getMunicipality());
                logger.info(" -> Island         = {}", randomProperty.getIsland());
                logger.info(" -> Centroid       = ({}, {})", cx, cy);

            } catch (ParseException e) {
                logger.warn("Failed to parse WKT for objectID={}: {}",
                        randomProperty.getObjectID(), e.getMessage());
            }
        }

        // 5. Get adjacency from the Graph.
        List<Graph.GraphNode> graphNeighbors = propertyGraph.getNeighbors(testObjectID);
        Set<Integer> graphNeighborIDs = graphNeighbors.stream()
                .map(Graph.GraphNode::getObjectID)
                .collect(Collectors.toSet());

        // 6. List-based adjacency check for the same record (using parishSubset).
        List<PropertyRecord> listNeighbors = PropertyUtils.findAdjacentProperties(randomProperty, parishSubset);
        Set<Integer> listNeighborIDs = listNeighbors.stream()
                .map(PropertyRecord::getObjectID)
                .collect(Collectors.toSet());

        // 7. Compare the sets.
        if (graphNeighborIDs.equals(listNeighborIDs)) {
            logger.info("Both methods agree on adjacency for objectID = {}", testObjectID);
        } else {
            logger.warn("Mismatch in adjacency sets for objectID {}:\nGraph-based: {}\nList-based: {}",
                    testObjectID, graphNeighborIDs, listNeighborIDs);
        }

        // 8. Build the JGraphT-based PropertyGraph (with STRtree-based adjacency).
        PropertyGraph propertyGraphJgt = new PropertyGraph();
        propertyGraphJgt.buildGraph(parishSubset);

        // 8a. Calculate the average area of properties grouped by owner.
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> jgtGraph = propertyGraphJgt.getGraph();
        double averageGroupedArea = PropertyUtils.calculateAverageGroupedArea(parishSubset, jgtGraph);
        System.out.println("Average area of properties in parish (grouped by owner) [ha]: " + String.format("%.2f",averageGroupedArea));
        if (averageArea != averageGroupedArea) {
            System.out.println("These averages differ: " + String.format("%.2f",averageArea) + " ha" + " vs "
                    + String.format("%.2f",averageGroupedArea) + " ha");
        }

        // 8b. Demonstrate merging adjacent properties that belong to the same owner, within this parish subset.
        demonstrateMergingProperties(parishSubset);

        // 8c) Merge same-owner adjacency
        List<PropertyRecord> merged = PropertyMerger.mergeSameOwner(parishSubset);

        // 8d) Build adjacency among these merged props
        SimpleGraph<PropertyRecord, DefaultEdge> mergedGraph = MergedPropertyGraph.buildGraph(merged);

        // 8e) Suggest swaps with areaThreshold=0.1 => up to 10 suggestions
        List<SwapSuggestion> suggestions = PropertySwapAdvisor.suggestSwaps(mergedGraph, 0.1, 10);

        // 8f) Print the suggestions
        for (SwapSuggestion s : suggestions) {
            System.out.println(s);
        }

        // 9. Build the owner graph using the same parish subset.
        OwnerGraph ownerGraph = new OwnerGraph();
        ownerGraph.buildGraph(parishSubset);

        // 10. Check how many owners are in the graph.
        System.out.println("Number of owners in the OwnerGraph: " + ownerGraph.getOwners().size());

        // 11. Pick one owner's ID (from the first property) and see who they're adjacent to.
        if (!parishSubset.isEmpty()) {
            int someOwner = parishSubset.get(0).getOwner();
            Set<Integer> ownerNeighbors = ownerGraph.getNeighbors(someOwner);
            System.out.println("Owner " + someOwner + " is adjacent to owners: " + ownerNeighbors);
        }

        // 12. Calculate the distance to Funchal Sé.
        double distanceToFunchal = PropertyUtils.distanceToFunchal(1234);
        System.out.println("Distance to Funchal Sé in kilometers: " + String.format("%.1f", distanceToFunchal/1000));

        // 12a. Calculate the distance to Machico.
        double distanceMachico = PropertyUtils.distanceToMachico(1234);
        System.out.println("Distance to Machico in kilometers: " + String.format("%.1f", distanceMachico/1000));

        // 13. Visualize the STRtree-based property graph in GraphStream.
        GraphVisualization.visualizeGraph(propertyGraphJgt);
    }

    /**
     * Demonstrates merging adjacent properties belonging to the same owner.
     * <p>
     * This example filters properties by a chosen parish, then merges them
     * to produce a new list where each connected component is collapsed
     * into a single {@link PropertyRecord}.
     */
    private static void demonstrateMergingProperties(List<PropertyRecord> parishSubset) {
        System.out.println("\n--- Demonstrate Merging of Adjacent Properties ---");
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(parishSubset);

        System.out.println("Original subset size: " + parishSubset.size());
        System.out.println("Merged subset size: " + merged.size());
        for (PropertyRecord mp : merged) {
            System.out.println("Merged property => Object ID=" + mp.getObjectID()
                    + ", Area [ha]=" + mp.getShapeArea()
                    + ", Geometry=" + mp.getGeometry().substring(0, Math.min(60, mp.getGeometry().length())) + "...");
        }
    }
}
