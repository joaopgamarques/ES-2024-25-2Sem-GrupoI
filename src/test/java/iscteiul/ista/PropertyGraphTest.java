package iscteiul.ista;

import org.junit.jupiter.api.Test;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link PropertyGraph}.
 * <p>
 * This class contains unit tests to verify the functionality of the {@code PropertyGraph} class,
 * ensuring that property vertices are added and adjacency edges are correctly formed.
 * By examining the resulting graph, we confirm that each property is present as a vertex
 * and that properties sharing a boundary have an edge between them.
 *
 * <p><strong>Date:</strong> 2024-03-05</p>
 * <p><strong>Author:</strong> bfaae</p>
 */
class PropertyGraphTest {

    /**
     * Tests the {@link PropertyGraph#buildGraph(List)} method to ensure
     * that vertices (properties) are correctly added to the underlying graph structure.
     *
     * <p><strong>Scenario:</strong> Two {@code PropertyRecord} objects are passed in.
     * <ul>
     *   <li>Expected Outcome: Both records should exist as vertices in the graph.</li>
     *   <li>Complexity: Cyclomatic complexity 2</li>
     * </ul>
     */
    @Test
    void buildGraph1() {
        // Arrange: Create a new PropertyGraph instance and two PropertyRecords.
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
        List<PropertyRecord> records = Arrays.asList(record1, record2);

        // Act: Build the graph with these records.
        propertyGraph.buildGraph(records);
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Assert: Check that both vertices are in the graph.
        assertTrue(
                graph.containsVertex(record1),
                "record1 should be present in the graph as a vertex."
        );
        assertTrue(
                graph.containsVertex(record2),
                "record2 should be present in the graph as a vertex."
        );
    }

    /**
     * Tests the {@link PropertyGraph#buildGraph(List)} method to verify that
     * edges are added between adjacent properties.
     *
     * <p><strong>Scenario:</strong> Three {@code PropertyRecord} objects are passed in,
     * arranged such that each property is adjacent to at least one other.
     * <ul>
     *   <li>Expected Outcome: Edges (in either direction) exist between the appropriate pairs.</li>
     *   <li>No edge is expected if properties are not adjacent.</li>
     * </ul>
     */
    @Test
    void buildGraph2() {
        // Arrange: Create three PropertyRecords that share boundaries in sequence
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
        PropertyRecord record3 = new PropertyRecord(
                3, 3L, 3L, 200.0, 300.0,
                "MULTIPOLYGON(((2 2, 3 2, 3 3, 2 3, 2 2)))",
                3, "Parish3", "Municipality3", "Island3"
        );
        List<PropertyRecord> records = Arrays.asList(record1, record2, record3);

        // Act: Build the graph with these records.
        propertyGraph.buildGraph(records);
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Assert: Verify edges exist between adjacent records.
        assertTrue(
                graph.containsEdge(record1, record2) || graph.containsEdge(record2, record1),
                "An edge between record1 and record2 should exist in the graph."
        );
        assertTrue(
                graph.containsEdge(record2, record3) || graph.containsEdge(record3, record2),
                "An edge between record2 and record3 should exist in the graph."
        );
    }

    /**
     * Tests the {@link PropertyGraph#getGraph()} method to ensure
     * that the returned {@link Graph} is not null.
     *
     * <p><strong>Scenario:</strong> After creating a new {@code PropertyGraph},
     * the user calls {@code getGraph()}.
     * <ul>
     *   <li>Expected Outcome: The graph reference should never be null.</li>
     *   <li>Complexity: Cyclomatic complexity 1</li>
     * </ul>
     */
    @Test
    void getGraph() {
        // Arrange: Create a new PropertyGraph instance.
        PropertyGraph propertyGraph = new PropertyGraph();

        // Act: Retrieve the graph.
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Assert: Confirm it is not null.
        assertNotNull(graph, "The returned graph should not be null.");
    }
}
