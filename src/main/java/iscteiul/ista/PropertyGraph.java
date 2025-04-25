package iscteiul.ista;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code PropertyGraph} class constructs and manages a graph of {@link PropertyRecord}
 * objects, where edges represent adjacency (shared boundaries) between properties.
 *
 * <p>It uses:
 * <ul>
 *   <li>A JGraphT {@link Graph} to store vertices (properties) and edges (adjacency)</li>
 *   <li>An R-tree ({@link STRtree}) for efficient spatial queries on property geometry</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // 1. Create a PropertyGraph instance
 * PropertyGraph propertyGraph = new PropertyGraph();
 *
 * // 2. Build the graph from a list of PropertyRecords
 * propertyGraph.buildGraph(propertyList);
 *
 * // 3. Get the resulting JGraphT Graph for further processing
 * Graph<PropertyRecord, DefaultEdge> jGraphTGraph = propertyGraph.getGraph();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class uses synchronized blocks and a
 * {@link ConcurrentHashMap} to prevent concurrency issues during graph construction.
 * If you do not need concurrency, consider simplifying the implementation by removing
 * these synchronized structures.</p>
 */
public class PropertyGraph {

    /**
     * The JGraphT graph where each vertex is a {@link PropertyRecord}
     * and each edge is a {@link DefaultEdge} representing adjacency.
     */
    private final Graph<PropertyRecord, DefaultEdge> graph;

    /**
     * Spatial index (R-tree) for quick lookups of nearby properties.
     * Used to reduce the computational cost of finding candidate neighbors.
     */
    private final STRtree spatialIndex;

    /**
     * Constructs an empty {@code PropertyGraph} with a {@link SimpleGraph}
     * and an {@link STRtree} for spatial indexing.
     */
    public PropertyGraph() {
        this.graph = new SimpleGraph<>(DefaultEdge.class);
        this.spatialIndex = new STRtree(); // R-tree for spatial indexing
    }

    /**
     * Builds the adjacency graph from a list of {@link PropertyRecord} objects.
     * <ul>
     *   <li><strong>Step 1:</strong> Each property is added to the graph as a vertex,
     *       and also inserted into the {@link STRtree} for spatial queries.</li>
     *   <li><strong>Step 2:</strong> For each property, neighbors are retrieved from
     *       the spatial index, and {@code PropertyUtils.arePropertiesAdjacent(...)}
     *       is used to verify adjacency before creating edges.</li>
     * </ul>
     *
     * <p>This method is synchronized, ensuring thread safety if multiple threads attempt
     * to build the graph simultaneously. It also employs a {@link ConcurrentHashMap}
     * for tracking properties by their {@code objectID}, though the map is not further
     * used within this method.</p>
     *
     * @param properties the list of {@link PropertyRecord} instances to incorporate
     *                   into the graph.
     */
    public void buildGraph(List<PropertyRecord> properties) {
        // A concurrent map to store properties by their ID, if needed.
        ConcurrentHashMap<Integer, PropertyRecord> propertyMap = new ConcurrentHashMap<>();

        // Step 1: Add all vertices to the graph and spatial index.
        properties.forEach(property -> {
            synchronized (this) {
                graph.addVertex(property);
                spatialIndex.insert(GeometryUtils.getEnvelope(property.getGeometry()), property);
                propertyMap.put(property.getObjectID(), property);
            }
        });

        // Step 2: Identify and add edges for adjacent properties.
        properties.forEach(property -> {
            // Query potential neighbors from the spatial index using property envelope
            List<?> neighbors = spatialIndex.query(GeometryUtils.getEnvelope(property.getGeometry()));

            // Check adjacency and add edges.
            neighbors.forEach(obj -> {
                PropertyRecord neighbor = (PropertyRecord) obj;
                if (!property.equals(neighbor)
                        && PropertyUtils.arePropertiesAdjacent(property, neighbor)) {
                    synchronized (this) {
                        // Ensure vertices still exist in the graph
                        if (graph.containsVertex(property) && graph.containsVertex(neighbor)) {
                            graph.addEdge(property, neighbor);
                        } else {
                            System.err.println(
                                    "Error: Attempting to add edge with non-existent vertex -> "
                                            + property + " <-> " + neighbor
                            );
                        }
                    }
                }
            });
        });

        System.out.println("Graph built with " + graph.vertexSet().size() + " vertices and "
                + graph.edgeSet().size() + " edges.");
    }

    /**
     * Returns the underlying JGraphT graph containing property vertices and adjacency edges.
     *
     * @return the {@link Graph} of {@link PropertyRecord} vertices and {@link DefaultEdge} edges.
     */
    public Graph<PropertyRecord, DefaultEdge> getGraph() {
        return graph;
    }
}
