package iscteiul.ista;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OwnerGraphTest {

    private List<PropertyRecord> sampleRecords;
    private OwnerGraph ownerGraph;

    @BeforeEach
    void setUp() {
        // 1) Create a small set of PropertyRecords with multiple owners.

        // We'll use simple squares as WKT for adjacency checks:
        // Polygon A: (0,0) to (1,1)
        String polyA = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))";

        // Polygon B: (1,0) to (2,1) - touches A along x=1
        String polyB = "POLYGON((1 0, 1 1, 2 1, 2 0, 1 0))";

        // Polygon C: far away at (10,10) to (11,11)
        String polyC = "POLYGON((10 10, 10 11, 11 11, 11 10, 10 10))";

        // Let's assign owners:
        // Owner 1 => recordA
        // Owner 2 => recordB
        // Owner 3 => recordC
        // A & B are adjacent; B & C are not, A & C are not => so owners 1 & 2 should be adjacent, 2 & 3 not, 1 & 3 not.

        PropertyRecord recordA = new PropertyRecord(
                100,       // objectID
                111L,      // parcelID
                0L,        // parcelNumber
                0.0,       // shapeLength
                0.0,       // shapeArea
                polyA,     // geometry
                1,         // owner
                "TestParish",
                "TestMunicipality",
                "TestIsland"
        );

        PropertyRecord recordB = new PropertyRecord(
                200,
                222L,
                0L,
                0.0,
                0.0,
                polyB,
                2,         // owner
                "TestParish",
                "TestMunicipality",
                "TestIsland"
        );

        PropertyRecord recordC = new PropertyRecord(
                300,
                333L,
                0L,
                0.0,
                0.0,
                polyC,
                3,         // owner
                "AnotherParish",
                "AnotherMunicipality",
                "AnotherIsland"
        );

        // Also let's add a 4th record with the same geometry as B, but same owner as A:
        // so A & D share owner => that doesn't create adjacency between owners,
        // but ensures multiple properties for one owner.
        PropertyRecord recordD = new PropertyRecord(
                400,
                444L,
                0L,
                0.0,
                0.0,
                polyB,
                1,         // same owner as recordA
                "TestParish",
                "TestMunicipality",
                "TestIsland"
        );

        sampleRecords = new ArrayList<>();
        sampleRecords.add(recordA);
        sampleRecords.add(recordB);
        sampleRecords.add(recordC);
        sampleRecords.add(recordD);

        // 2) Instantiate the OwnerGraph and build it.
        ownerGraph = new OwnerGraph();
        ownerGraph.buildGraph(sampleRecords);
    }

    @Test
    void testOwnerVertices() {
        // We expect owners = {1,2,3} from our dataset
        Set<Integer> owners = ownerGraph.getOwners();
        assertEquals(3, owners.size(), "Should have 3 distinct owners in the graph");
        assertTrue(owners.contains(1));
        assertTrue(owners.contains(2));
        assertTrue(owners.contains(3));
    }

    @Test
    void testOwnerAdjacency() {
        // Our adjacency logic:
        // Owner1 -> properties: (recordA=polyA), (recordD=polyB)
        // Owner2 -> properties: (recordB=polyB)
        // Owner3 -> properties: (recordC=polyC)

        // polyA touches polyB => owners(1,2) should be adjacent
        // polyB & polyB do not create adjacency across different owners
        // polyB & polyC do not touch => owners(2,3) not adjacent
        // polyA & polyC do not touch => owners(1,3) not adjacent

        // Check neighbors using getNeighbors(ownerId)
        Set<Integer> neighborsOf1 = ownerGraph.getNeighbors(1);
        Set<Integer> neighborsOf2 = ownerGraph.getNeighbors(2);
        Set<Integer> neighborsOf3 = ownerGraph.getNeighbors(3);

        // Owner1 is adjacent to Owner2
        assertTrue(neighborsOf1.contains(2), "Owner1 should be adjacent to Owner2");
        assertFalse(neighborsOf1.contains(3), "Owner1 should not be adjacent to Owner3");

        // Owner2 is adjacent to Owner1
        assertTrue(neighborsOf2.contains(1), "Owner2 should be adjacent to Owner1");
        assertFalse(neighborsOf2.contains(3), "Owner2 should not be adjacent to Owner3");

        // Owner3 is not adjacent to 1 or 2
        assertFalse(neighborsOf3.contains(1), "Owner3 should not be adjacent to Owner1");
        assertFalse(neighborsOf3.contains(2), "Owner3 should not be adjacent to Owner2");
    }

    @Test
    void testUnderlyingGraphStructure() {
        Graph<Integer, DefaultEdge> graph = ownerGraph.getGraph();

        // Confirm we have 3 vertices (owners) in the underlying graph
        assertEquals(3, graph.vertexSet().size());

        // Check edges. We expect 1 edge: (1--2)
        assertEquals(1, graph.edgeSet().size(), "Should have exactly one edge");
        Integer[] owners = graph.vertexSet().toArray(new Integer[0]);
        // Since we only expect an edge between owners 1 and 2
        assertTrue(graph.containsEdge(1, 2));
        assertFalse(graph.containsEdge(1, 3));
        assertFalse(graph.containsEdge(2, 3));
    }
}
