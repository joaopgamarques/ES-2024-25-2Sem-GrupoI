package iscteiul.ista;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkGEXF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demonstrates exporting a subset of {@link PropertyRecord} data (filtered
 * by parish) to a GEXF file for use in Gephi, using the GraphStream library.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Read CSV data via {@link CSVFileReader} (e.g. "/Madeira-Moodle-1.1.csv").</li>
 *   <li>Filter records by a chosen parish (e.g. "Arco da Calheta").</li>
 *   <li>Create a {@link SingleGraph} in GraphStream, adding each property as a node
 *       and adjacency edges determined by
 *       {@link PropertyUtils#arePropertiesAdjacent(PropertyRecord, PropertyRecord)}.</li>
 *   <li>Attach additional attributes (e.g. owner, shapeArea) to each node,
 *       so they appear in Gephi's Data Laboratory.</li>
 *   <li>Use {@link FileSinkGEXF} to write "output.gexf".</li>
 *   <li>Open "output.gexf" in Gephi for advanced layout &amp; analysis.</li>
 * </ol>
 *
 * <p><strong>Dependencies:</strong>
 * <ul>
 *   <li>{@code gs-core} (GraphStream) in your Maven {@code pom.xml} to build and export the graph.</li>
 *   <li>{@code CSVFileReader} and {@code PropertyUtils} from your existing code.</li>
 *   <li>Any adjacency logic in {@code PropertyUtils} (like geometry-based adjacency checks).</li>
 * </ul>
 */
public final class ExportToGephiDemo {

    private static final Logger logger = LoggerFactory.getLogger(ExportToGephiDemo.class);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * Since this is a utility class providing only static methods,
     * no instances should be created. The constructor immediately
     * throws an {@link AssertionError}.
     *
     * @throws AssertionError always, since this constructor should never be called
     */
    private ExportToGephiDemo() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Builds a GraphStream {@link Graph} from a list of {@link PropertyRecord} objects.
     * Each record becomes a node, labeled with its object ID, and edges are added
     * wherever {@code arePropertiesAdjacent(...) == true}.
     *
     * <p>Additionally, this method attaches extra attributes (e.g. "owner", "shapeArea")
     * to each node, so these fields appear in the exported GEXF and can be used
     * in Gephi for partitioning or styling.
     *
     * @param properties The subset of records (already filtered if needed).
     * @return A {@link SingleGraph} representing adjacency among these properties.
     */
    public static Graph buildGraph(List<PropertyRecord> properties) {
        // Create a new graph with the ID "MyGephiExport"
        Graph graph = new SingleGraph("MyGephiExport");

        // Add a node for each property
        for (PropertyRecord pr : properties) {
            // Node ID is the object's unique ID
            String nodeId = String.valueOf(pr.getObjectID());
            Node node = graph.addNode(nodeId);

            // Basic label (so it can appear in Gephi's UI if needed)
            node.setAttribute("ui.label", "ID: " + pr.getObjectID());

            // Attach additional attributes so they show up in Gephi's Data Laboratory
            node.setAttribute("owner", pr.getOwner());
            node.setAttribute("shapeArea", pr.getShapeArea());
            // You can add more if you like, e.g. geometry, municipality, island, etc.
            node.setAttribute("municipality", pr.getMunicipality());
            node.setAttribute("parish", pr.getParish());
        }

        // Add edges for adjacency
        for (int i = 0; i < properties.size(); i++) {
            for (int j = i + 1; j < properties.size(); j++) {
                PropertyRecord a = properties.get(i);
                PropertyRecord b = properties.get(j);

                // If adjacency logic says they're connected
                if (PropertyUtils.arePropertiesAdjacent(a, b)) {
                    String edgeId = a.getObjectID() + "_" + b.getObjectID();
                    graph.addEdge(edgeId,
                            String.valueOf(a.getObjectID()),
                            String.valueOf(b.getObjectID()),
                            false);
                    // 'false' -> undirected edge
                }
            }
        }

        logger.info("Graph built with {} nodes and {} edges.",
                graph.getNodeCount(), graph.getEdgeCount());
        return graph;
    }

    /**
     * Main method:
     * <ol>
     *   <li>Reads property records from CSV via {@link CSVFileReader}.</li>
     *   <li>Filters them by parish ("Arco da Calheta").</li>
     *   <li>Builds a GraphStream graph (with attributes for "owner", "shapeArea").</li>
     *   <li>Exports to "output.gexf" using {@link FileSinkGEXF}.</li>
     * </ol>
     *
     * <p>Open "output.gexf" in Gephi for analysis and layout.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // 1) Load data
        CSVFileReader csvFileReader = new CSVFileReader();
        List<PropertyRecord> propertyRecords = csvFileReader.importData("/Madeira-Moodle-1.1.csv");
        logger.info("Total records loaded: {}", propertyRecords.size());

        // 2) Filter records by parish
        String chosenParish = "Arco da Calheta";
        List<PropertyRecord> parishSubset = propertyRecords.stream()
                .filter(pr -> chosenParish.equals(pr.getParish()))
                .collect(Collectors.toList());
        logger.info("Records in parish '{}': {}", chosenParish, parishSubset.size());

        // 3) Build the adjacency graph from that subset
        Graph graph = buildGraph(parishSubset);

        // 4) Export to GEXF
        FileSinkGEXF fileSink = new FileSinkGEXF();
        // fileSink.setPrettyPrint(true); // optional for more readable GEXF

        try {
            fileSink.writeAll(graph, "output.gexf");
            logger.info("Exported graph to output.gexf! Open this file in Gephi.");
        } catch (IOException e) {
            logger.error("Error writing GEXF file", e);
        }
    }
}
