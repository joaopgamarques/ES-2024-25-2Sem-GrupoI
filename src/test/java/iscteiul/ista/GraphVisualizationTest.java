package iscteiul.ista;

import org.junit.jupiter.api.Test;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link GraphVisualization}.
 * <p>
 * This class contains methods that validate the visualization
 * functionality provided by {@code GraphVisualization}.
 * Since the visualization is rendered in a live GraphStream window,
 * these tests primarily check for runtime errors and basic graph integrity.
 *
 * <p><strong>Date:</strong> 2024-03-05</p>
 * <p><strong>Author:</strong> bfaae</p>
 */
class GraphVisualizationTest {

    /**
     * Tests the {@link GraphVisualization#visualizeGraph(PropertyGraph)} method.
     * <p>
     * Complexity: Cyclomatic complexity 1
     * </p>
     *
     * <p><strong>Notes:</strong> This test verifies that the graph is not null,
     * and that key vertices are present after building and visualizing the graph.
     * It does not automate verification of the live UI display, which requires
     * user inspection in an interactive window.</p>
     */
    @Test
    void visualizeGraph() {
        // Arrange: Create a sample PropertyGraph.
        PropertyGraph propertyGraph = new PropertyGraph();
        PropertyRecord record1 = new PropertyRecord(
                1, 1L, 1L, 100.0, 200.0,
                "MULTIPOLYGON(((0 0, 1 0, 1 1, 0 1, 0 0)))",
                1, "Parish1", "Municipality1", "Island1"
        );
        PropertyRecord record2 = new PropertyRecord(
                2, 2L, 2L, 150.0, 250.0,
                "MULTIPOLYGON(((1 1, 2 1, 2 2, 1 2, 1 1)))",
                2, "Parish2", "Municipality2", "Island2"
        );
        propertyGraph.buildGraph(Arrays.asList(record1, record2));

        // Act: Invoke the visualization method.
        GraphVisualization.visualizeGraph(propertyGraph);

        // Assert: Verify that the graph is not null and contains our records.
        assertNotNull(propertyGraph.getGraph(),
                "The underlying graph should not be null.");
        assertTrue(propertyGraph.getGraph().containsVertex(record1),
                "record1 should be present in the graph as a vertex.");
        assertTrue(propertyGraph.getGraph().containsVertex(record2),
                "record2 should be present in the graph as a vertex.");
    }
}
