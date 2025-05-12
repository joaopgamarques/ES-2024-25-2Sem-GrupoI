package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link MergedPropertyGraph}.
 */
public class MergedPropertyGraphTest {

    /**
     * A small list of merged properties used in testing.
     */
    private List<PropertyRecord> testProps;

    /**
     * Sets up a list of sample {@link PropertyRecord} objects
     * with simple WKT geometries to test adjacency.
     *
     * In this setup:
     * - propA and propB should be adjacent,
     * - propC should not touch propA or propB.
     */
    @BeforeEach
    public void setUp() {
        testProps = new ArrayList<>();

        // A simple square polygon at coordinates (0,0) to (1,1)
        String squareA = "POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))";
        // Another square immediately to the right: (1,0) to (2,1), so it touches squareA at x=1
        String squareB = "POLYGON((1 0, 2 0, 2 1, 1 1, 1 0))";
        // A square that's far away, e.g. (10,10) to (11,11)
        String squareC = "POLYGON((10 10, 11 10, 11 11, 10 11, 10 10))";

        // Build 3 sample PropertyRecord objects with geometry
        PropertyRecord propA = new PropertyRecord(
                101, 10001, 20001, 4.0, 1.0, // dummy length/area
                squareA,
                777, "ParishX", "MunicipalityX", "IslandX"
        );

        PropertyRecord propB = new PropertyRecord(
                102, 10002, 20002, 4.0, 1.0,
                squareB,
                888, "ParishX", "MunicipalityX", "IslandX"
        );

        PropertyRecord propC = new PropertyRecord(
                103, 10003, 20003, 4.0, 1.0,
                squareC,
                999, "ParishY", "MunicipalityY", "IslandY"
        );

        // Add them to our test list
        testProps.add(propA);
        testProps.add(propB);
        testProps.add(propC);
    }

    /**
     * Tests that an empty list of properties returns an empty graph.
     */
    @Test
    public void testBuildGraph_EmptyList() {
        List<PropertyRecord> emptyList = new ArrayList<>();
        SimpleGraph<PropertyRecord, DefaultEdge> graph =
                MergedPropertyGraph.buildGraph(emptyList);

        assertTrue(graph.vertexSet().isEmpty(),
                "Graph should have no vertices for empty input list");
        assertTrue(graph.edgeSet().isEmpty(),
                "Graph should have no edges for empty input list");
    }

    /**
     * Tests that the graph correctly identifies adjacency among polygons
     * and non-adjacency for distant polygons.
     */
    @Test
    public void testBuildGraph_Adjacency() {
        SimpleGraph<PropertyRecord, DefaultEdge> graph =
                MergedPropertyGraph.buildGraph(testProps);

        // We expect 3 vertices total
        assertEquals(3, graph.vertexSet().size(),
                "Graph should have 3 vertices (one per property)");

        // Retrieve each property from the test list
        PropertyRecord propA = testProps.get(0);
        PropertyRecord propB = testProps.get(1);
        PropertyRecord propC = testProps.get(2);

        // propA & propB share a boundary => should be adjacent
        assertTrue(graph.containsEdge(propA, propB),
                "propA and propB must be adjacent in the graph");

        // propC is far away => should NOT be adjacent to A or B
        assertFalse(graph.containsEdge(propA, propC),
                "propA and propC must not be adjacent");
        assertFalse(graph.containsEdge(propB, propC),
                "propB and propC must not be adjacent");
    }
}
