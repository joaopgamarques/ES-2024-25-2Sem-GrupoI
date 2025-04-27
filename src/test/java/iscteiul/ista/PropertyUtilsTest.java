package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link PropertyUtils}, verifying the functionality of filtering
 * properties by owner, municipality, and island, and checking adjacency among
 * polygonal geometries.
 *
 * <p>These tests rely on sample {@link PropertyRecord} objects with valid WKT
 * (Well-Known Text) polygons to confirm adjacency. Specifically:
 * <ul>
 *     <li>{@code rec1} and {@code rec2} share a boundary (adjacent)</li>
 *     <li>{@code rec3} is far away, ensuring no adjacency with {@code rec1} or {@code rec2}</li>
 * </ul>
 *
 * <p>Filtering tests validate that {@code findByOwner()}, {@code findByMunicipality()},
 * and {@code findByIsland()} return the correct subset or an empty list where appropriate.
 * Adjacency tests use {@link GeometryUtils#areAdjacent(String, String)} behind the scenes
 * via the utility methods in {@code PropertyUtils}.
 *
 * @author
 *    Your Name (or your team name)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PropertyUtilsTest {

    /** A shared list of sample {@link PropertyRecord} objects for all tests. */
    private static List<PropertyRecord> sampleRecords;

    /** Sample property with geometry adjacent to rec2. */
    private static PropertyRecord rec1;

    /** Sample property with geometry adjacent to rec1. */
    private static PropertyRecord rec2;

    /** Sample property placed far away to ensure non-adjacency. */
    private static PropertyRecord rec3;

    /**
     * Sets up the sample property records before any tests run.
     * Each record has different owners, municipalities, and polygons.
     */
    @BeforeAll
    static void setUpBeforeAll() {
        sampleRecords = new ArrayList<>();

        /*
         * rec1 and rec2 share a boundary in their WKT polygons,
         * rec3 is far away to ensure a non-adjacent scenario.
         */

        // rec1: a 1x1 square from (0,0) to (1,1).
        rec1 = new PropertyRecord(
                1,
                7343148L,
                2996240000000L,
                57.2469,
                202.0598,
                "POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))",
                93,
                "Arco da Calheta",
                "Calheta",
                "Ilha da Madeira (Madeira)"
        );

        // rec2: a 1x1 square that shares a boundary with rec1 along x=1.
        rec2 = new PropertyRecord(
                2,
                7343149L,
                2996240000001L,
                50.0,
                300.0,
                "POLYGON((1 0, 2 0, 2 1, 1 1, 1 0))",
                93, // same owner as rec1
                "Some Parish",
                "Calheta", // same municipality as rec1
                "Ilha da Madeira (Madeira)"
        );

        // rec3: placed at (10,10) to (11,11), far from rec1/rec2.
        rec3 = new PropertyRecord(
                3,
                1234567L,
                9876543210000L,
                120.5,
                600.0,
                "POLYGON((10 10, 11 10, 11 11, 10 11, 10 10))",
                999,
                "NA",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );

        sampleRecords.add(rec1);
        sampleRecords.add(rec2);
        sampleRecords.add(rec3);
    }

    /**
     * Clears the sample data after all tests finish.
     */
    @AfterAll
    static void tearDownAfterAll() {
        sampleRecords.clear();
    }

    // ------------------------------------------------------------------------
    // TESTS for findByOwner()
    // ------------------------------------------------------------------------

    /**
     * Tests finding properties by an existing owner (93),
     * expecting both rec1 and rec2 to match.
     */
    @Test
    @Order(1)
    void testFindByOwner_existingOwner() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 93);
        assertEquals(2, found.size(), "Should find 2 properties owned by 93.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list should contain rec1 and rec2.");
    }

    /**
     * Tests finding properties by a non-existing owner (555),
     * expecting an empty list.
     */
    @Test
    @Order(2)
    void testFindByOwner_nonExistingOwner() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 555);
        assertTrue(found.isEmpty(), "Should return an empty list if the owner doesn't exist.");
    }

    // ------------------------------------------------------------------------
    // TESTS for findByMunicipality()
    // ------------------------------------------------------------------------

    /**
     * Tests finding properties in municipality "Calheta",
     * expecting rec1 and rec2.
     */
    @Test
    @Order(3)
    void testFindByMunicipality_match() {
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "Calheta");
        assertEquals(2, found.size(), "Calheta should have 2 properties in the sample data.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list for Calheta should contain rec1 and rec2.");
    }

    /**
     * Tests finding properties in municipality "SomethingElse",
     * expecting no matches.
     */
    @Test
    @Order(4)
    void testFindByMunicipality_noMatch() {
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "SomethingElse");
        assertTrue(found.isEmpty(), "No property should match municipality 'SomethingElse'.");
    }

    /**
     * Tests handling a {@code null} municipality argument,
     * expecting a non-null but empty result.
     */
    @Test
    @Order(5)
    void testFindByMunicipality_nullArg() {
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, null);
        assertNotNull(found, "The method should return a non-null empty list for null input.");
        assertTrue(found.isEmpty(), "Should return an empty list if municipality is null.");
    }

    // ------------------------------------------------------------------------
    // TESTS for findByIsland()
    // ------------------------------------------------------------------------

    /**
     * Tests finding properties in "Ilha da Madeira (Madeira)",
     * where all sample records reside.
     */
    @Test
    @Order(6)
    void testFindByIsland_match() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha da Madeira (Madeira)");
        assertEquals(3, found.size(), "Should find all 3 sample records on 'Ilha da Madeira (Madeira)'.");
        assertTrue(found.contains(rec1) && found.contains(rec2) && found.contains(rec3),
                "The returned list should contain rec1, rec2, and rec3.");
    }

    /**
     * Tests finding properties in "Ilha do Porto Santo",
     * expecting no matches.
     */
    @Test
    @Order(7)
    void testFindByIsland_noMatch() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha do Porto Santo");
        assertTrue(found.isEmpty(), "Should return empty if no records are on that island.");
    }

    /**
     * Tests handling a {@code null} island argument,
     * expecting a non-null but empty result.
     */
    @Test
    @Order(8)
    void testFindByIsland_nullArg() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, null);
        assertNotNull(found, "Should return a non-null list, even if argument is null.");
        assertTrue(found.isEmpty(), "Should return an empty list for null island.");
    }

    // ------------------------------------------------------------------------
    // TESTS for adjacency: arePropertiesAdjacent() + findAdjacentProperties()
    // ------------------------------------------------------------------------

    /**
     * Tests that {@code rec1} and {@code rec2} are detected as adjacent,
     * since they share a boundary in their WKT polygons.
     */
    @Test
    @Order(9)
    void testArePropertiesAdjacent_true() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec2);
        assertTrue(adjacent, "rec1 and rec2 should be adjacent based on their shared boundary.");
    }

    /**
     * Tests that {@code rec1} and {@code rec3} are not adjacent,
     * as they lie in separate regions.
     */
    @Test
    @Order(10)
    void testArePropertiesAdjacent_false() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec3);
        assertFalse(adjacent, "rec1 and rec3 should NOT be adjacent.");
    }

    /**
     * Tests that rec1 only returns rec2 as adjacent within {@code sampleRecords}.
     */
    @Test
    @Order(11)
    void testFindAdjacentProperties_forRec1() {
        List<PropertyRecord> adjacentList = PropertyUtils.findAdjacentProperties(rec1, sampleRecords);
        assertEquals(1, adjacentList.size(), "rec1 should be adjacent to exactly 1 property (rec2).");
        assertTrue(adjacentList.contains(rec2), "rec2 should appear in the adjacency list for rec1.");
    }

    /**
     * Tests handling of {@code null} arguments to {@code findAdjacentProperties}.
     * Ensures no exceptions and returns an empty list.
     */
    @Test
    @Order(12)
    void testFindAdjacentProperties_nullRecordOrList() {
        List<PropertyRecord> nullRecordTest = PropertyUtils.findAdjacentProperties(null, sampleRecords);
        List<PropertyRecord> nullListTest   = PropertyUtils.findAdjacentProperties(rec1, null);

        assertTrue(nullRecordTest.isEmpty(), "If the record is null, should return empty list.");
        assertTrue(nullListTest.isEmpty(),   "If the list is null, should return empty list.");
    }

    /**
     * Tests that the {@code getDistinctParishes} method identifies unique parish names
     * among {@code sampleRecords}, ignoring repeated or null values. Ensures
     * "Arco da Calheta" and "Some Parish" appear in the returned set.
     */
    @Test
    @Order(13)
    void testGetDistinctParishes() {
        Set<String> distinct = PropertyUtils.getDistinctParishes(sampleRecords);
        // rec1 => "Arco da Calheta"
        // rec2 => "Some Parish"
        // rec3 => possibly "NA" => null?

        assertTrue(distinct.contains("Arco da Calheta"),
                "Should include 'Arco da Calheta' in the set.");
        assertTrue(distinct.contains("Some Parish"),
                "Should include 'Some Parish' in the set.");
        // Additional checks as needed
    }

    /**
     * Tests that the {@code getDistinctMunicipalities} method identifies unique municipality
     * names among {@code sampleRecords}, ignoring repeated or null values. Ensures
     * "Calheta" and "Funchal" appear in the returned set and that the total count is 2.
     */
    @Test
    @Order(14)
    void testGetDistinctMunicipalities() {
        Set<String> distinct = PropertyUtils.getDistinctMunicipalities(sampleRecords);
        // rec1 => "Calheta"
        // rec2 => "Calheta"
        // rec3 => "Funchal"

        assertTrue(distinct.contains("Calheta"),
                "Should include 'Calheta' from rec1 and rec2.");
        assertTrue(distinct.contains("Funchal"),
                "Should include 'Funchal' from rec3.");
        assertEquals(2, distinct.size(),
                "Expected exactly 2 unique municipality names.");
    }

    // ------------------------------------------------------------------------
    // Additional tests for findByOwner(), average area, grouping, etc.
    // ------------------------------------------------------------------------

    /**
     * Another test for {@link PropertyUtils#findByOwner(List, int)} verifying
     * we get 2 properties for owner=93.
     */
    @Test
    @Order(15)
    void testFindByOwner_sameOwnerAsRec3() {
        // This test is placed here for clarity, but references rec3's distinct owner 999 above.
        // The logic is: rec3 alone has owner=999, so we expect exactly 1 result.
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 999);
        assertEquals(1, found.size(), "We expect exactly 1 property (rec3) for owner=999.");
        assertTrue(found.contains(rec3), "The found list should include rec3 only.");
    }

    // TESTS for calculateAverageArea() -
    // (We'll do 2 tests: one with a valid list, one with null input.)

    @Test
    @Order(16)
    void testCalculateAverageArea_valid() {
        double average = PropertyUtils.calculateAverageArea(sampleRecords);
        // rec1.area=202.0598, rec2.area=300.0, rec3.area=600.0
        // sum=1102.0598, #=3 => average=1102.0598/3=367.353266...
        // We'll allow a small delta in the assert
        assertEquals(367.35, average, 0.01,
                "Error: Expected average area ~367.35 for the sampleRecords.");
    }

    @Test
    @Order(17)
    void testCalculateAverageArea_nullInput() {
        double average = PropertyUtils.calculateAverageArea(null);
        assertEquals(0.0, average,
                "Expected average area to be 0.0 for null input.");
    }

    // TESTS for groupPropertiesByOwner()
    @Test
    @Order(18)
    void testGroupPropertiesByOwner() {
        Map<Integer, List<PropertyRecord>> grouped = PropertyUtils.groupPropertiesByOwner(sampleRecords);
        // We expect 2 owners total: 93 (rec1 & rec2), 999 (rec3).
        assertEquals(2, grouped.size(),
                "Error: Expected 2 owners in the grouped map.");

        // Check that owner=93 has 2, owner=999 has 1
        assertTrue(grouped.containsKey(93), "Should have a key for owner=93.");
        assertTrue(grouped.containsKey(999), "Should have a key for owner=999.");
        assertEquals(2, grouped.get(93).size(),
                "Owner=93 should have 2 properties (rec1, rec2).");
        assertEquals(1, grouped.get(999).size(),
                "Owner=999 should have 1 property (rec3).");
    }

    // ------------------------------------------------------------------------
    // TESTS for calculateAverageGroupedArea()
    // ------------------------------------------------------------------------

    @Test
    @Order(19)
    void testCalculateAverageGroupedArea_nullGraph() {
        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, null);
        assertEquals(0.0, average,
                "Error: Expected average grouped area to be 0.0 for null graph.");
    }

    @Test
    @Order(20)
    void testCalculateAverageGroupedArea_connected() {
        // Create a graph in which rec1 and rec2 are connected, rec3 is isolated
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);

        // Connect rec1 and rec2 (forming one connected component)
        graph.addEdge(rec1, rec2);

        // Calculate the average area of the connected groups
        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);

        // rec1 + rec2 => total area = 202.0598 + 300 = 502.0598 (one connected component)
        // rec3 => total area = 600.0 (another connected component)
        // We have 2 connected components => average = (502.0598 + 600) / 2 = 551.0299
        assertEquals(551.0299, average, 0.01,
                "Error: The average of the connected groups is incorrect.");
    }

    @Test
    @Order(21)
    void testCalculateAverageGroupedArea_disconnected() {
        // Create a graph with NO edges: all properties are isolated
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);

        // Each property is its own connected component
        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);

        // rec1 => area=202.0598
        // rec2 => area=300
        // rec3 => area=600
        // sum of group areas = 202.0598 + 300 + 600 = 1102.0598
        // # of groups = 3, so average ~ 367.353266...
        // We'll compare to 367.35 with a small tolerance
        double roundedAverage = Math.round(average * 100.0) / 100.0;

        assertEquals(367.35, roundedAverage, 0.01,
                "Error: The average area of disconnected groups is incorrect.");
    }

    /**
     * Tests a valid scenario where both the target property (rec1) and the Funchal (Sé)
     * record (with objectID=11074) exist in the provided list. Verifies that the
     * computed distance matches the expected centroid distance.
     */
    @Test
    @Order(22)
    void testDistanceToFunchal_validCase() {
        // Create a local copy of the sampleRecords so we don't permanently affect them
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // Add a property record to represent #11074 (Funchal Sé).
        // We'll place it somewhere near rec1 & rec2 or far, up to you.
        // For simplicity, let's put it at (2,2) -> (3,3).
        PropertyRecord funchalSe = new PropertyRecord(
                11074,
                999999L,
                888888L,
                0.0,
                0.0,
                "POLYGON((2 2, 3 2, 3 3, 2 3, 2 2))", // geometry near rec3
                1234,            // owner (arbitrary)
                "Funchal (Sé)",  // parish
                "Funchal",       // municipality
                "Ilha da Madeira (Madeira)"
        );
        localRecords.add(funchalSe);

        // Now call distanceToFunchal on rec1, which is at (0..1,0..1).
        // We'll compare the centroids:
        // - rec1 centroid ~ (0.5, 0.5)
        // - funchalSe centroid ~ (2.5, 2.5)
        // Distance ~ sqrt((2.5-0.5)^2 + (2.5-0.5)^2) = sqrt(8) ~ 2.8284
        double dist = PropertyUtils.distanceToFunchal(1, localRecords);

        assertFalse(Double.isNaN(dist),
                "Distance should be valid (not NaN) if both properties exist with valid geometry.");
        // Check approximate distance with a small tolerance
        assertEquals(2.8284, dist, 0.001,
                "Centroid distance between rec1 and #11074 should be around sqrt(8) ≈ 2.8284.");
    }

    /**
     * Tests the scenario where #11074 is not present in the list at all.
     * Expects the returned distance to be Double.NaN.
     */
    @Test
    @Order(23)
    void testDistanceToFunchal_missingFunchalRecord() {
        // Use sampleRecords as-is (it does NOT have #11074).
        // We expect distanceToFunchal(...) to return NaN.
        double dist = PropertyUtils.distanceToFunchal(1, sampleRecords);

        assertTrue(Double.isNaN(dist),
                "distanceToFunchal should return NaN if #11074 is not in the list.");
    }

    /**
     * Tests the scenario where the source property ID does not exist in the list,
     * but #11074 is present. Expects Double.NaN since the 'source' record is missing.
     */
    @Test
    @Order(24)
    void testDistanceToFunchal_missingSourceProperty() {
        // Create a list that has #11074 but NOT propertyId=9999 (we'll pass 9999).
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // Insert the #11074 record with some geometry
        PropertyRecord funchalSe = new PropertyRecord(
                11074,
                999999L,
                888888L,
                0.0,
                0.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))", // arbitrary shape
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        localRecords.add(funchalSe);

        // Now call distanceToFunchal with propertyId=9999, which doesn't exist
        double dist = PropertyUtils.distanceToFunchal(9999, localRecords);
        assertTrue(Double.isNaN(dist),
                "Should return NaN because the requested propertyId=9999 doesn't exist.");
    }

    /**
     * Tests the scenario where one or both of the geometries (source or #11074) are
     * invalid or blank. Expects Double.NaN in both cases, since distance cannot
     * be computed.
     */
    @Test
    @Order(25)
    void testDistanceToFunchal_invalidOrBlankGeometry() {
        // Create a local copy & add a #11074 record with BLANK geometry
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // Add #11074 with blank geometry => distance should be NaN
        PropertyRecord funchalSeBlankGeom = new PropertyRecord(
                11074,
                999999L,
                888888L,
                0.0,
                0.0,
                "",  // blank geometry
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        localRecords.add(funchalSeBlankGeom);

        // rec1 is valid, #11074 has blank geometry => distance = NaN
        double dist = PropertyUtils.distanceToFunchal(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "If #11074 has blank geometry, the computed distance should be NaN.");

        // Alternatively, remove the blank one and add an invalid WKT string
        localRecords.remove(funchalSeBlankGeom);
        PropertyRecord funchalSeBadGeom = new PropertyRecord(
                11074,
                999999L,
                888888L,
                0.0,
                0.0,
                "NOT_A_VALID_WKT", // invalid geometry
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        localRecords.add(funchalSeBadGeom);

        dist = PropertyUtils.distanceToFunchal(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "With invalid geometry for #11074, distance should also be NaN.");
    }
}
