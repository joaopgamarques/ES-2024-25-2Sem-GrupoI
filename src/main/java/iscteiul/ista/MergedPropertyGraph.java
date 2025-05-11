package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;

/**
 * Builds a graph of "merged properties" where each vertex is a property
 * (resulting from merging same-owner polygons), and an edge indicates
 * these two merged properties are neighbors (their polygons touch).
 */
public final class MergedPropertyGraph {

    private MergedPropertyGraph() {
        // no instantiation
    }

    /**
     * Constructs a graph of merged properties, where edges link
     * two merged properties if they are adjacent (touch).
     *
     * @param mergedProps the list of properties after merging
     * @return a {@link SimpleGraph} with each property as a vertex, edges for adjacency
     */
    public static SimpleGraph<PropertyRecord, DefaultEdge> buildGraph(List<PropertyRecord> mergedProps) {
        SimpleGraph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        // Add vertices
        for (PropertyRecord p : mergedProps) {
            graph.addVertex(p);
        }

        // Check adjacency among these "big" merged properties
        for (int i = 0; i < mergedProps.size(); i++) {
            for (int j = i + 1; j < mergedProps.size(); j++) {
                PropertyRecord a = mergedProps.get(i);
                PropertyRecord b = mergedProps.get(j);
                // skip if same owner => we only want adjacency among different owners?
                // or do we keep adjacency anyway? depends on your definition
                // We'll build adjacency for all, so you can choose later
                if (GeometryUtils.areAdjacent(a.getGeometry(), b.getGeometry())) {
                    graph.addEdge(a, b);
                }
            }
        }

        return graph;
    }
}
