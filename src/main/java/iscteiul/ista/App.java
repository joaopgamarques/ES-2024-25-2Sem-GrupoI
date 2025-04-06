package iscteiul.ista;

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
public class App {

    /**
     * SLF4J logger for the App class.
     */
    private static final Logger logger = LoggerFactory.getLogger(App.class);

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

        // 2. Filter to a chosen parish.
        String chosenParish = "Arco da Calheta";
        List<PropertyRecord> parishSubset = propertyRecords.stream()
                .filter(pr -> chosenParish.equals(pr.getParish()))
                .collect(Collectors.toList());
        logger.info("Records in parish '{}': {}", chosenParish, parishSubset.size());

        // 3. Build the graph from the parish subset.
        Graph propertyGraph = new Graph(parishSubset);

        // 4. Pick a random PropertyRecord from the subset.
        if (parishSubset.isEmpty()) {
            logger.warn("No records found in parish '{}'. Exiting.", chosenParish);
            return;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(parishSubset.size());     // pick random index
        PropertyRecord randomProperty = parishSubset.get(randomIndex);
        int testObjectID = randomProperty.getObjectID();           // get that property’s ID

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

        // 6. List-based adjacency for the same record (using parishSubset to match the graph).
        List<PropertyRecord> listNeighbors = PropertyUtils.findAdjacentProperties(randomProperty, parishSubset);
        Set<Integer> listNeighborIDs = listNeighbors.stream()
                .map(PropertyRecord::getObjectID)
                .collect(Collectors.toSet());

        // 7. Print the results.
        logger.info("Graph-based neighbors for objectID {}: {}", testObjectID, graphNeighborIDs);
        logger.info("List-based neighbors for objectID {}: {}", testObjectID, listNeighborIDs);

        // 8. Compare the sets.
        if (graphNeighborIDs.equals(listNeighborIDs)) {
            logger.info("Both methods agree on adjacency for objectID = {}", testObjectID);
        } else {
            logger.warn("Mismatch in adjacency sets for objectID {}:\nGraph-based: {}\nList-based: {}",
                    testObjectID, graphNeighborIDs, listNeighborIDs);
        }

        // Visualize the same subset using PropertyGraph + GraphVisualization.

        // 9. Create a JGraphT-based PropertyGraph.
        PropertyGraph propertyGraphJgt = new PropertyGraph();

        // 10. Build the graph (uses STRtree + adjacency checks under the hood).
        propertyGraphJgt.buildGraph(parishSubset);

        // 11. Use GraphVisualization to display the result in a GraphStream window.
        //     (this will appear in a small pop-up window with auto-layout)
        GraphVisualization.visualizeGraph(propertyGraphJgt);
    }
}
