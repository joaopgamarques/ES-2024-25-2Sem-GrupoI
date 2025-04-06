package iscteiul.ista;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link ExportToGephiDemo}, focusing on the buildGraph method.
 */
public class ExportToGephiDemoTest {

    /**
     * Verifies that buildGraph creates nodes and edges for the given PropertyRecord list.
     * Two records (#1 and #2) share a boundary, while #3 is isolated.
     * Ensures node attributes (owner, shapeArea, etc.) are assigned correctly.
     */
    @Test
    void testBuildGraphCreatesNodesAndEdges() {
        // Create PropertyRecord objects.
        // We'll assume #1 and #2 share a boundary, and #3 is separate.
        List<PropertyRecord> records = new ArrayList<>();
        records.add(new PropertyRecord(
                1,       // objectID
                100L,    // parcelID
                999L,    // parcelNumber
                10.0,    // shapeLength
                20.0,    // shapeArea
                "POLYGON((0 0,0 1,1 1,1 0,0 0))", // geometry
                11,      // owner
                "ParishA",
                "MunicipA",
                "IslandX"
        ));
        records.add(new PropertyRecord(
                2,
                200L,
                888L,
                10.0,
                30.0,
                "POLYGON((1 0,1 1,2 1,2 0,1 0))", // touches #1
                12,
                "ParishA",
                "MunicipA",
                "IslandX"
        ));
        records.add(new PropertyRecord(
                3,
                300L,
                777L,
                10.0,
                25.0,
                "POLYGON((3 3,3 4,4 4,4 3,3 3))", // separate from #1 and #2
                13,
                "ParishB",
                "MunicipB",
                "IslandY"
        ));

        // Build the graph via ExportToGephiDemo
        Graph graph = ExportToGephiDemo.buildGraph(records);

        // 1) Verify node count
        assertEquals(3, graph.getNodeCount(),
                "Expected 3 nodes for the 3 PropertyRecords.");

        // Retrieve the nodes by their IDs (stringified objectID)
        Node node1 = graph.getNode("1");
        Node node2 = graph.getNode("2");
        Node node3 = graph.getNode("3");

        // 2) Make sure each node is present
        assertNotNull(node1, "Node '1' should exist in the graph.");
        assertNotNull(node2, "Node '2' should exist in the graph.");
        assertNotNull(node3, "Node '3' should exist in the graph.");

        // 3) Check node attributes
        // For 'owner', we cast the stored attribute to Number and convert to int.
        int actualOwnerNode1 = ((Number) node1.getAttribute("owner")).intValue();
        assertEquals(11, actualOwnerNode1, "Node '1' should have owner=11.");

        double actualShapeAreaNode1 = ((Number) node1.getAttribute("shapeArea")).doubleValue();
        assertEquals(20.0, actualShapeAreaNode1, 0.0001,
                "Node '1' shapeArea should be 20.0.");

        assertEquals("ParishA", node1.getAttribute("parish"));
        assertEquals("MunicipA", node1.getAttribute("municipality"));

        // 4) Check adjacency (edges)
        // We expect node1 and node2 to be adjacent => 1 edge
        // Node3 is isolated => no edges
        assertEquals(1, graph.getEdgeCount(),
                "Expected exactly 1 edge (between nodes #1 and #2).");

        // Check node1's degree (should be 1, connecting to node2)
        assertEquals(1, node1.getDegree(),
                "Node1 should have exactly 1 edge (with node2).");

        // Check node2's degree (should be 1, connecting to node1)
        assertEquals(1, node2.getDegree(),
                "Node2 should have exactly 1 edge (with node1).");

        // Check node3's degree (should be 0, isolated)
        assertEquals(0, node3.getDegree(),
                "Node3 should not have any edges (isolated).");
    }

    /**
     * Verifies that passing an empty list of PropertyRecord results in a graph
     * with zero nodes and zero edges.
     */
    @Test
    void testBuildGraphWithEmptyList() {
        // If no records are provided, the graph should have 0 nodes, 0 edges
        List<PropertyRecord> empty = new ArrayList<>();
        Graph graph = ExportToGephiDemo.buildGraph(empty);

        assertEquals(0, graph.getNodeCount(),
                "No nodes should be in the graph for an empty list.");
        assertEquals(0, graph.getEdgeCount(),
                "No edges should be in the graph for an empty list.");
    }
}
