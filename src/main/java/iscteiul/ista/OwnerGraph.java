package iscteiul.ista;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a graph of owners, where each node is an owner (identified by owner ID),
 * and edges indicate that these owners have at least one pair of adjacent properties.
 *
 * <p>Internally uses a JGraphT {@link SimpleGraph} of (Integer, DefaultEdge).
 */
public class OwnerGraph {

    /**
     * The underlying JGraphT graph with:
     * - Vertex = Integer (the owner's ID)
     * - Edge = DefaultEdge (undirected, representing adjacency between owners)
     */
    private final Graph<Integer, DefaultEdge> graph;

    /**
     * Constructs an empty OwnerGraph using a SimpleGraph.
     */
    public OwnerGraph() {
        this.graph = new SimpleGraph<>(DefaultEdge.class);
    }

    /**
     * Builds the Owner graph from the list of PropertyRecord objects.
     * <ol>
     *   <li>Collect all unique owner IDs.</li>
     *   <li>Add each owner ID as a vertex in the graph.</li>
     *   <li>For each pair of owners, check if they have at least one pair of adjacent properties.
     *       If so, add an edge between those owners.</li>
     * </ol>
     *
     * <p><strong>Note:</strong> This is an O(N^2 * M^2) approach in the worst case,
     * if you have N owners each with up to M properties, because for each pair of owners,
     * you compare all properties. For smaller datasets, it's fine. For large data, consider
     * more efficient adjacency checks or spatial indexing.
     *
     * @param properties A list of all {@link PropertyRecord} objects (possibly from the entire dataset).
     */
    public void buildGraph(List<PropertyRecord> properties) {
        // 1) Group properties by their owner
        Map<Integer, List<PropertyRecord>> propsByOwner = properties.stream()
                .collect(Collectors.groupingBy(PropertyRecord::getOwner));

        // 2) Add each unique owner as a vertex in the graph
        for (Integer ownerId : propsByOwner.keySet()) {
            graph.addVertex(ownerId);
        }

        // 3) For each pair of owners, check adjacency among their properties
        List<Integer> ownerList = new ArrayList<>(propsByOwner.keySet());
        for (int i = 0; i < ownerList.size(); i++) {
            for (int j = i + 1; j < ownerList.size(); j++) {
                Integer ownerA = ownerList.get(i);
                Integer ownerB = ownerList.get(j);

                // Retrieve each owner's list of properties
                List<PropertyRecord> propsA = propsByOwner.get(ownerA);
                List<PropertyRecord> propsB = propsByOwner.get(ownerB);

                // Check if any property in A's list is adjacent to any in B's list
                if (ownersHaveAdjacentProperties(propsA, propsB)) {
                    // Add an edge in the graph (undirected)
                    graph.addEdge(ownerA, ownerB);
                }
            }
        }
    }

    /**
     * Simple helper method to check if any property in listA is adjacent to any property in listB.
     */
    private boolean ownersHaveAdjacentProperties(List<PropertyRecord> listA, List<PropertyRecord> listB) {
        for (PropertyRecord a : listA) {
            for (PropertyRecord b : listB) {
                if (PropertyUtils.arePropertiesAdjacent(a, b)) {
                    return true; // Found at least one adjacent pair
                }
            }
        }
        return false;
    }

    /**
     * @return the underlying JGraphT graph of (ownerID, edges).
     */
    public Graph<Integer, DefaultEdge> getGraph() {
        return graph;
    }

    /**
     * Returns a set of all owner IDs currently in this graph.
     *
     * @return a {@code Set} containing each owner's unique integer ID
     */
    public Set<Integer> getOwners() {
        return graph.vertexSet();
    }

    /**
     * Returns the set of adjacent owners (neighbors) for a given owner ID.
     * <p>
     * An adjacent owner is one who shares at least one pair of adjacent
     * properties with the specified owner.
     *
     * @param ownerId the integer ID of the owner whose neighbors are sought
     * @return a {@code Set} of owner IDs adjacent to the given {@code ownerId},
     *         or an empty set if the owner does not exist or has no neighbors
     */
    public Set<Integer> getNeighbors(int ownerId) {
        if (!graph.containsVertex(ownerId)) {
            return Collections.emptySet();
        }
        // In JGraphT, you can find neighbors by checking edges from the given vertex.
        Set<DefaultEdge> edges = graph.edgesOf(ownerId);
        Set<Integer> neighbors = new HashSet<>();
        for (DefaultEdge e : edges) {
            Integer source = graph.getEdgeSource(e);
            Integer target = graph.getEdgeTarget(e);
            // The "other" node is whichever is not the ownerId
            if (!source.equals(ownerId)) {
                neighbors.add(source);
            }
            if (!target.equals(ownerId)) {
                neighbors.add(target);
            }
        }
        return neighbors;
    }
}
