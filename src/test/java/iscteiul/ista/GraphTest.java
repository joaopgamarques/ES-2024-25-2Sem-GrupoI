package iscteiul.ista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphTest {

    @Test
    void testGraphCreationAndAdjacency() {
        // Create a few PropertyRecord objects with WKT polygons.
        // Polygons #1 and #2 share a boundary and should be adjacent.
        // Polygon #3 does not touch the others and should not be adjacent to them.
        List<PropertyRecord> propertyRecords = new ArrayList<>();

        // (1) Square from (0,0) to (1,1)
        propertyRecords.add(
                new PropertyRecord(
                        1,            // objectID
                        100L,         // parcelID
                        999L,         // parcelNumber
                        100.0,        // shapeLength
                        50.0,         // shapeArea
                        "POLYGON((0 0,0 1,1 1,1 0,0 0))", // geometry
                        101,          // owner
                        "ParishA",    // parish
                        "MunicipA",   // municipality
                        "IslandX"     // island
                )
        );

        // (2) Square from (1,0) to (2,1), sharing edge with #1
        propertyRecords.add(
                new PropertyRecord(
                        2,
                        200L,
                        888L,
                        120.0,
                        60.0,
                        "POLYGON((1 0,1 1,2 1,2 0,1 0))",
                        102,
                        "ParishA",
                        "MunicipA",
                        "IslandX"
                )
        );

        // (3) Square from (2.1,0) to (3.1,1) — no shared edge with #1 or #2
        propertyRecords.add(
                new PropertyRecord(
                        3,
                        300L,
                        777L,
                        200.0,
                        80.0,
                        "POLYGON((2.1 0,2.1 1,3.1 1,3.1 0,2.1 0))",
                        103,
                        "ParishB",
                        "MunicipB",
                        "IslandY"
                )
        );

        // Build the graph
        Graph graph = new Graph(propertyRecords);

        // Verify the number of nodes
        assertEquals(3, graph.getAllNodes().size(),
                "Graph should contain 3 GraphNodes corresponding to 3 PropertyRecords.");

        // Check adjacency for objectID = 1
        List<Graph.GraphNode> neighbors1 = graph.getNeighbors(1);
        assertEquals(1, neighbors1.size(),
                "ObjectID=1 should have exactly 1 neighbor (objectID=2).");
        assertEquals(2, neighbors1.get(0).getObjectID(),
                "ObjectID=1's neighbor should have objectID=2.");

        // Check adjacency for objectID = 2
        List<Graph.GraphNode> neighbors2 = graph.getNeighbors(2);
        assertEquals(1, neighbors2.size(),
                "ObjectID=2 should have exactly 1 neighbor (objectID=1).");
        assertEquals(1, neighbors2.get(0).getObjectID(),
                "ObjectID=2's neighbor should have objectID=1.");

        // Check adjacency for objectID = 3
        List<Graph.GraphNode> neighbors3 = graph.getNeighbors(3);
        assertTrue(neighbors3.isEmpty(),
                "ObjectID=3 should not be adjacent to #1 or #2.");

        // Validate that we can retrieve nodes by objectID
        Graph.GraphNode node1 = graph.getNodeByObjectID(1);
        assertNotNull(node1, "Should retrieve a node for objectID=1.");
        assertEquals(1, node1.getObjectID());

        Graph.GraphNode nodeNonExistent = graph.getNodeByObjectID(999);
        assertNull(nodeNonExistent, "Should return null for an objectID that does not exist in the graph.");
    }

    @Test
    void testEmptyGraph() {
        // Construct a graph with an empty list
        Graph graph = new Graph(Collections.emptyList());

        // Should have no nodes
        assertTrue(graph.getAllNodes().isEmpty(),
                "Graph built from an empty list should have no nodes.");
    }

    @Test
    void testSinglePropertyRecord() {
        // If there's only one property, it can't have any neighbors
        PropertyRecord singleRecord = new PropertyRecord(
                10,         // objectID
                500L,       // parcelID
                400L,       // parcelNumber
                50.0,       // shapeLength
                25.0,       // shapeArea
                "POLYGON((0 0,0 1,1 1,1 0,0 0))",
                200,        // owner
                "ParishSingle",
                "MunicipSingle",
                "IslandSingle"
        );
        Graph graph = new Graph(Collections.singletonList(singleRecord));

        assertEquals(1, graph.getAllNodes().size(),
                "Graph should contain exactly 1 node.");

        // No neighbors expected
        assertTrue(graph.getNeighbors(10).isEmpty(),
                "The single property node should not have neighbors.");
    }
}
