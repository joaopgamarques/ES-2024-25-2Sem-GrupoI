package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
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
        // 1. Read CSV
        CSVFileReader csvFileReader = new CSVFileReader();
        List<PropertyRecord> propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.1.csv");
        logger.info("Total records loaded: {}", propertyRecords.size());

        // 1a. Print distinct parishes and municipalities.
        Set<String> distinctParishes = PropertyUtils.getDistinctParishes(propertyRecords);
        Set<String> distinctMunicipalities = PropertyUtils.getDistinctMunicipalities(propertyRecords);
        logger.info("Distinct Parishes: {}", distinctParishes);
        logger.info("Distinct Municipalities: {}", distinctMunicipalities);

        // 2. Filter to a chosen parish.
        String chosenParish = "Canhas";
        List<PropertyRecord> parishSubset = propertyRecords.stream()
                .filter(pr -> chosenParish.equals(pr.getParish()))
                .collect(Collectors.toList());
        logger.info("Records in parish '{}': {}", chosenParish, parishSubset.size());

        // 2a. Calculate the average area of properties in the chosen parish (no adjacency grouping).
        List<PropertyRecord> parishProperties = PropertyUtils.findByParish(propertyRecords, chosenParish);
        double averageArea = PropertyUtils.calculateAverageArea(parishProperties);
        logger.info("Records in parish '{}': {}", chosenParish, parishProperties.size());
        logger.info("Average area (no adjacency grouping) in parish '{}': {}", chosenParish, averageArea);

        // 3. Build the custom (O(N²)) Graph from the parish subset.
        Graph propertyGraph = new Graph(parishSubset);

        // 4. Pick a random PropertyRecord from the subset.
        if (parishSubset.isEmpty()) {
            logger.warn("No records found in parish '{}'. Exiting.", chosenParish);
            return;
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
                logger.info(" -> objectID      = {}", randomProperty.getObjectID());
                logger.info(" -> parcelID      = {}", randomProperty.getParcelID());
                logger.info(" -> shapeLength   = {}", randomProperty.getShapeLength());
                logger.info(" -> shapeArea     = {}", randomProperty.getShapeArea());
                logger.info(" -> owner         = {}", randomProperty.getOwner());
                logger.info(" -> parish        = {}", randomProperty.getParish());
                logger.info(" -> municipality  = {}", randomProperty.getMunicipality());
                logger.info(" -> island        = {}", randomProperty.getIsland());
                logger.info(" -> centroid      = ({}, {})", cx, cy);

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
        System.out.println("Average area of properties in parish (grouped by owner): " + averageGroupedArea);
        if (averageArea != averageGroupedArea) {
            System.out.println("These averages differ: " + averageArea + " vs " + averageGroupedArea);
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

        // 12. Visualize the STRtree-based property graph in GraphStream.
        GraphVisualization.visualizeGraph(propertyGraphJgt);
    }
}
