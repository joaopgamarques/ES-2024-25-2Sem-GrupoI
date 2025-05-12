package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

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

    @Test
    @Order(1)
    void testFindByOwner_existingOwner() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 93);
        assertEquals(2, found.size(), "Should find 2 properties owned by 93.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list should contain rec1 and rec2.");
    }

    @Test
    @Order(2)
    void testFindByOwner_nonExistingOwner() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 555);
        assertTrue(found.isEmpty(), "Should return an empty list if the owner doesn't exist.");
    }

    // ------------------------------------------------------------------------
    // TESTS for findByMunicipality()
    // ------------------------------------------------------------------------

    @Test
    @Order(3)
    void testFindByMunicipality_match() {
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "Calheta");
        assertEquals(2, found.size(), "Calheta should have 2 properties in the sample data.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list for Calheta should contain rec1 and rec2.");
    }

    @Test
    @Order(4)
    void testFindByMunicipality_noMatch() {
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "SomethingElse");
        assertTrue(found.isEmpty(), "No property should match municipality 'SomethingElse'.");
    }

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

    @Test
    @Order(6)
    void testFindByIsland_match() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha da Madeira (Madeira)");
        assertEquals(3, found.size(), "Should find all 3 sample records on 'Ilha da Madeira (Madeira)'.");
        assertTrue(found.contains(rec1) && found.contains(rec2) && found.contains(rec3),
                "The returned list should contain rec1, rec2, and rec3.");
    }

    @Test
    @Order(7)
    void testFindByIsland_noMatch() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha do Porto Santo");
        assertTrue(found.isEmpty(), "Should return empty if no records are on that island.");
    }

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

    @Test
    @Order(9)
    void testArePropertiesAdjacent_true() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec2);
        assertTrue(adjacent, "rec1 and rec2 should be adjacent based on their shared boundary.");
    }

    @Test
    @Order(10)
    void testArePropertiesAdjacent_false() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec3);
        assertFalse(adjacent, "rec1 and rec3 should NOT be adjacent.");
    }

    @Test
    @Order(11)
    void testFindAdjacentProperties_forRec1() {
        List<PropertyRecord> adjacentList = PropertyUtils.findAdjacentProperties(rec1, sampleRecords);
        assertEquals(1, adjacentList.size(), "rec1 should be adjacent to exactly 1 property (rec2).");
        assertTrue(adjacentList.contains(rec2), "rec2 should appear in the adjacency list for rec1.");
    }

    @Test
    @Order(12)
    void testFindAdjacentProperties_nullRecordOrList() {
        List<PropertyRecord> nullRecordTest = PropertyUtils.findAdjacentProperties(null, sampleRecords);
        List<PropertyRecord> nullListTest   = PropertyUtils.findAdjacentProperties(rec1, null);

        assertTrue(nullRecordTest.isEmpty(), "If the record is null, should return empty list.");
        assertTrue(nullListTest.isEmpty(),   "If the list is null, should return empty list.");
    }

    @Test
    @Order(13)
    void testGetDistinctParishes() {
        Set<String> distinct = PropertyUtils.getDistinctParishes(sampleRecords);
        assertTrue(distinct.contains("Arco da Calheta"),
                "Should include 'Arco da Calheta' in the set.");
        assertTrue(distinct.contains("Some Parish"),
                "Should include 'Some Parish' in the set.");
    }

    @Test
    @Order(14)
    void testGetDistinctMunicipalities() {
        Set<String> distinct = PropertyUtils.getDistinctMunicipalities(sampleRecords);
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

    @Test
    @Order(15)
    void testFindByOwner_sameOwnerAsRec3() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 999);
        assertEquals(1, found.size(), "We expect exactly 1 property (rec3) for owner=999.");
        assertTrue(found.contains(rec3), "The found list should include rec3 only.");
    }

    @Test
    @Order(16)
    void testCalculateAverageArea_valid() {
        double average = PropertyUtils.calculateAverageArea(sampleRecords);
        // rec1.area=202.0598, rec2.area=300.0, rec3.area=600.0
        // sum=1102.0598, #=3 => average=367.353266...
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

    @Test
    @Order(18)
    void testGroupPropertiesByOwner() {
        Map<Integer, List<PropertyRecord>> grouped = PropertyUtils.groupPropertiesByOwner(sampleRecords);
        // We expect 2 owners total: 93 (rec1 & rec2), 999 (rec3).
        assertEquals(2, grouped.size(), "Error: Expected 2 owners in the grouped map.");
        assertTrue(grouped.containsKey(93), "Should have a key for owner=93.");
        assertTrue(grouped.containsKey(999), "Should have a key for owner=999.");
        assertEquals(2, grouped.get(93).size(),
                "Owner=93 should have 2 properties (rec1, rec2).");
        assertEquals(1, grouped.get(999).size(),
                "Owner=999 should have 1 property (rec3).");
    }

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
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);
        // Connect rec1 and rec2
        graph.addEdge(rec1, rec2);

        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);
        // rec1 + rec2 => total area = 202.0598 + 300 = 502.0598
        // rec3 => 600.0
        // 2 connected components => (502.0598 + 600) / 2 = 551.0299
        assertEquals(551.0299, average, 0.01,
                "Error: The average of the connected groups is incorrect.");
    }

    @Test
    @Order(21)
    void testCalculateAverageGroupedArea_disconnected() {
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);
        // No edges => all disconnected
        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);

        // sum=1102.0598 / 3 => ~367.3532
        double roundedAverage = Math.round(average * 100.0) / 100.0;
        assertEquals(367.35, roundedAverage, 0.01,
                "Error: The average area of disconnected groups is incorrect.");
    }

    /**
     * Verifies we get a valid (non-NaN) distance if both:
     * - The source property #1 has valid geometry
     * - App.funchalPropertyRecord is also valid
     */
    @Test
    @Order(22)
    void testDistanceToFunchal_validCase() {
        // 1) local copy of sampleRecords
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // 2) define a valid "funchal" record, objectID=11074
        // geometry => a 1x1 square at (2..3,2..3) => centroid(2.5,2.5)
        PropertyRecord funchalSe = new PropertyRecord(
                11074,    // the "Funchal" ID
                999999L,  // arbitrary
                888888L,  // arbitrary
                0.0,
                0.0,
                "POLYGON((2 2,3 2,3 3,2 3,2 2))", // centroid(2.5,2.5)
                1234,     // owner
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );

        // 3) set in App
        App.setFunchalPropertyRecord(funchalSe);

        // 4) call distanceToFunchal(1, localRecords)
        double dist = PropertyUtils.distanceToFunchal(1, localRecords);

        // => centroid #1 is (0.5,0.5); centroid #11074 is (2.5,2.5)
        // => distance = sqrt((2.0)^2 + (2.0)^2)=2.8284
        assertFalse(Double.isNaN(dist),
                "Distance should be valid if both reference & source have valid geometry.");
        assertEquals(2.8284, dist, 1e-3,
                "Distance ~ sqrt((2.5-0.5)^2 + (2.5-0.5)^2)=2.8284");
    }

    /**
     * Verifies that distance is NaN if the Funchal reference in App is null.
     */
    @Test
    @Order(23)
    void testDistanceToFunchal_missingFunchalRecord() {
        // 1) clear the reference
        App.setFunchalPropertyRecord(null);

        // 2) property #1 is in sampleRecords
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // 3) distance => should be NaN because we have no Funchal reference
        double dist = PropertyUtils.distanceToFunchal(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "distanceToFunchal should be NaN if the Funchal reference is null.");
    }

    /**
     * Verifies that distance is NaN if the requested source property is missing.
     */
    @Test
    @Order(24)
    void testDistanceToFunchal_missingSourceProperty() {
        // 1) define a valid Funchal reference
        PropertyRecord funchalSe = new PropertyRecord(
                11074, 999999L, 888888L,
                0.0, 0.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        App.setFunchalPropertyRecord(funchalSe);

        // 2) localRecords does NOT contain propertyId=9999 => missing
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);
        // sampleRecords only has ID=1 (and maybe others if you added them)

        // 3) request distance => should be NaN
        double dist = PropertyUtils.distanceToFunchal(9999, localRecords);
        assertTrue(Double.isNaN(dist),
                "Should return NaN because propertyId=9999 doesn't exist in localRecords.");
    }

    /**
     * Verifies that distance is NaN if the Funchal reference geometry is blank or invalid.
     */
    @Test
    @Order(25)
    void testDistanceToFunchal_invalidOrBlankGeometry() {
        // 1) localRecords => has property #1 => geometry is valid
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // 1a) define a blank-geometry record for funchal
        PropertyRecord blankGeomFunchal = new PropertyRecord(
                11074, 999999L, 888888L,
                0.0, 0.0,
                "", // blank geometry
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        App.setFunchalPropertyRecord(blankGeomFunchal);

        double dist = PropertyUtils.distanceToFunchal(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "If Funchal geometry is blank, distance must be NaN.");

        // 2) define an invalid-geometry record
        PropertyRecord invalidGeomFunchal = new PropertyRecord(
                11074, 999999L, 888888L,
                0.0, 0.0,
                "NOT_A_VALID_WKT", // invalid
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        App.setFunchalPropertyRecord(invalidGeomFunchal);

        dist = PropertyUtils.distanceToFunchal(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "With invalid geometry, distance must also be NaN.");
    }

    /**
     * Verifies a valid (non-NaN) distance if both:
     * - The source property (#1) has valid geometry
     * - App.machicoPropertyRecord is also valid
     */
    @Test
    @Order(26)
    void testDistanceToMachico_validCase() {
        // local copy for the source property
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // Define a valid "machico" record (ID=11517) => 1x1 square at (2..3,2..3) => centroid(2.5,2.5)
        PropertyRecord machicoRef = new PropertyRecord(
                11517,
                999999L,
                888888L,
                0.0,
                0.0,
                "POLYGON((2 2,3 2,3 3,2 3,2 2))", // centroid(2.5,2.5)
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        // 1) Set the Machico reference
        App.setMachicoPropertyRecord(machicoRef);

        // 2) Call distanceToMachico(1, localRecords)
        // source property #1 => centroid(0.5,0.5), reference => (2.5,2.5)
        double dist = PropertyUtils.distanceToMachico(1, localRecords);

        // distance => sqrt(2^2 + 2^2) = 2.8284
        assertFalse(Double.isNaN(dist),
                "Distance should be valid if both reference & source have valid geometry.");
        assertEquals(2.8284, dist, 1e-3,
                "Distance between (0.5,0.5) & (2.5,2.5) => ~2.8284");
    }

    /**
     * Verifies distance is NaN if the Machico reference in App is null.
     */
    @Test
    @Order(27)
    void testDistanceToMachico_missingMachicoRecord() {
        // Clear the reference => simulate not found
        App.setMachicoPropertyRecord(null);

        // localRecords still has property #1
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // distance => NaN because we have no Machico reference
        double dist = PropertyUtils.distanceToMachico(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "Should be NaN if Machico reference is null.");
    }

    /**
     * Verifies distance is NaN if the requested source property is missing.
     */
    @Test
    @Order(28)
    void testDistanceToMachico_missingSourceProperty() {
        // Define a valid Machico reference
        PropertyRecord machicoRef = new PropertyRecord(
                11517, 999999L, 888888L,
                0.0, 0.0,
                "POLYGON((2 2,3 2,3 3,2 3,2 2))",
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        App.setMachicoPropertyRecord(machicoRef);

        // localRecords does NOT contain propertyId=9999 => missing
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // distance => NaN
        double dist = PropertyUtils.distanceToMachico(9999, localRecords);
        assertTrue(Double.isNaN(dist),
                "Should be NaN because propertyId=9999 doesn't exist in localRecords.");
    }

    /**
     * Verifies distance is NaN if Machico reference geometry is blank or invalid.
     */
    @Test
    @Order(29)
    void testDistanceToMachico_invalidOrBlankGeometry() {
        // localRecords => has property #1 => valid
        List<PropertyRecord> localRecords = new ArrayList<>(sampleRecords);

        // (a) define Machico with blank geometry
        PropertyRecord blankGeomMachico = new PropertyRecord(
                11517, 999999L, 888888L,
                0.0, 0.0,
                "", // blank
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        App.setMachicoPropertyRecord(blankGeomMachico);

        double dist = PropertyUtils.distanceToMachico(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "If Machico geometry is blank, distance must be NaN.");

        // (b) define an invalid-geometry Machico
        PropertyRecord invalidGeomMachico = new PropertyRecord(
                11517, 999999L, 888888L,
                0.0, 0.0,
                "NOT_A_VALID_WKT", // invalid
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        App.setMachicoPropertyRecord(invalidGeomMachico);

        dist = PropertyUtils.distanceToMachico(1, localRecords);
        assertTrue(Double.isNaN(dist),
                "With invalid geometry, distance must also be NaN.");
    }

    // ------------------------------------------------------------------------
    // TESTS for mergeAdjacentPropertiesSameOwner()
    // ------------------------------------------------------------------------
    private static final WKTReader WKT_READER = new WKTReader();

    /**
     * Test merging two adjacent 1x1 squares of the same owner.
     * Result should be a single merged record with total area=2.
     */
    @Test
    @Order(30)
    void testMergeTwoAdjacentSameOwner() throws Exception {
        PropertyRecord recA = new PropertyRecord(
                101,
                999L,
                1111L,
                4.0,
                1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                10,   // Owner=10
                "ParishA",
                "MunicipalityA",
                "IslandA"
        );
        PropertyRecord recB = new PropertyRecord(
                102,
                998L,
                1112L,
                4.0,
                1.0,
                "POLYGON((1 0,2 0,2 1,1 1,1 0))",
                10,   // Same owner
                "ParishA",
                "MunicipalityA",
                "IslandA"
        );

        List<PropertyRecord> props = new ArrayList<>();
        props.add(recA);
        props.add(recB);

        List<PropertyRecord> mergedList = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);
        assertEquals(1, mergedList.size(),
                "Two adjacent squares of the same owner => one merged result.");

        PropertyRecord merged = mergedList.get(0);
        Geometry geom = WKT_READER.read(merged.getGeometry());
        assertEquals(2.0, geom.getArea(), 0.0001,
                "Merged area of two adjacent 1x1 squares should be 2.0");
        assertTrue(geom.getGeometryType().contains("MultiPolygon"),
                "Should be a MultiPolygon per the forcing logic if union is a single polygon.");
        assertEquals(10, merged.getOwner(),
                "Owner should match the 'largest' or same property since both are area=1.");
    }

    /**
     * Test merging three 1x1 squares in a row for the same owner.
     * Expect a single merged shape with area=3.0.
     */
    @Test
    @Order(27)
    void testMergeThreePropertiesSameOwnerAllAdjacent() throws Exception {
        PropertyRecord rec1 = new PropertyRecord(
                201, 1000L, 2001L, 4.0, 1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                50, "ParishX", "MunicipalityX", "IslandX");
        PropertyRecord rec2 = new PropertyRecord(
                202, 1001L, 2002L, 4.0, 1.0,
                "POLYGON((1 0,2 0,2 1,1 1,1 0))",
                50, "ParishX", "MunicipalityX", "IslandX");
        PropertyRecord rec3 = new PropertyRecord(
                203, 1002L, 2003L, 4.0, 1.0,
                "POLYGON((2 0,3 0,3 1,2 1,2 0))",
                50, "ParishX", "MunicipalityX", "IslandX");

        List<PropertyRecord> props = List.of(rec1, rec2, rec3);
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);
        assertEquals(1, merged.size(),
                "All three squares in a row => one merged property.");

        Geometry unionGeom = WKT_READER.read(merged.get(0).getGeometry());
        assertEquals(3.0, unionGeom.getArea(), 0.0001,
                "3 adjacent 1x1 squares horizontally => area=3.0");
    }

    /**
     * Tests no merge occurs when two same-owner properties are not adjacent.
     */
    @Test
    @Order(28)
    void testNoMergeIfNotAdjacent() {
        PropertyRecord recA = new PropertyRecord(
                301, 2000L, 3001L, 4.0, 1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                77, "ParishA", "MunicipalityA", "IslandA");
        // Far away
        PropertyRecord recB = new PropertyRecord(
                302, 2001L, 3002L, 4.0, 1.0,
                "POLYGON((10 10,11 10,11 11,10 11,10 10))",
                77, "ParishA", "MunicipalityA", "IslandA");

        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(List.of(recA, recB));
        assertEquals(2, merged.size(),
                "No adjacency => no merge => same list size=2.");
        assertTrue(merged.contains(recA) && merged.contains(recB),
                "They remain unmodified.");
    }

    /**
     * Tests that different owners, even if adjacent, never get merged.
     */
    @Test
    @Order(29)
    void testNoMergeAcrossDifferentOwners() {
        PropertyRecord owner10 = new PropertyRecord(
                401, 3000L, 4001L, 4.0, 1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                10, "ParishM", "MunicipalityM", "IslandM");
        PropertyRecord owner20 = new PropertyRecord(
                402, 3001L, 4002L, 4.0, 1.0,
                "POLYGON((1 0,2 0,2 1,1 1,1 0))",
                20, "ParishM", "MunicipalityM", "IslandM");

        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(List.of(owner10, owner20));
        assertEquals(2, merged.size(),
                "Different owners => no merge => remain separate.");
    }

    /**
     * Tests that invalid geometry is skipped. If all are invalid, fallback to the largest property,
     * if at least one is valid, union only the valid ones.
     */
    @Test
    @Order(30)
    void testInvalidGeometrySkipped() throws Exception {
        PropertyRecord valid = new PropertyRecord(
                501, 4000L, 5001L, 4.0, 1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                123, "ParishZ", "MunicipalityZ", "IslandZ");
        PropertyRecord invalid = new PropertyRecord(
                502, 4001L, 5002L, 4.0, 1.5,
                "NOT_A_VALID_WKT",  // invalid
                123, "ParishZ", "MunicipalityZ", "IslandZ");

        List<PropertyRecord> props = List.of(valid, invalid);
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);
        assertEquals(1, merged.size(),
                "Same owner + adjacency => merges to 1, ignoring invalid geometry.");

        Geometry finalGeom = WKT_READER.read(merged.get(0).getGeometry());
        assertEquals(1.0, finalGeom.getArea(), 0.0001,
                "Union ignoring invalid geometry => just the valid area=1.0");
    }

    /**
     * Tests empty or null inputs. Expect empty results with no exceptions.
     */
    @Test
    @Order(31)
    void testEmptyOrNullInput() {
        assertTrue(PropertyUtils.mergeAdjacentPropertiesSameOwner(new ArrayList<>()).isEmpty(),
                "Empty list => no merges => empty result.");
        assertTrue(PropertyUtils.mergeAdjacentPropertiesSameOwner(null).isEmpty(),
                "Null => empty result (code checks for null/empty).");
    }

    /**
     * Single property => no adjacency, so same single property is returned.
     */
    @Test
    @Order(32)
    void testSinglePropertyNoMerge() {
        PropertyRecord single = new PropertyRecord(
                601, 5000L, 6001L, 4.0, 2.0,
                "POLYGON((0 0,2 0,2 1,0 1,0 0))",
                999, "SoloParish", "SoloMunicipality", "SoloIsland");
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(List.of(single));
        assertEquals(1, merged.size(),
                "Single property => no merges => one item remains.");
        assertSame(single, merged.get(0),
                "Should return the exact same property instance.");
    }

    @Test
    @Order(33)
    void testMergeSameOwnerOneInvalidFarAway() throws Exception {
        // Property A: valid geometry at (0..1,0..1), owner=77
        PropertyRecord validFarLeft = new PropertyRecord(
                700,       // objectID
                700L,      // parcelID
                700L,      // parcelNumber
                0.0,       // shapeLength placeholder
                1.0,       // shapeArea placeholder
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                77,        // same owner
                "ParishZ", "MunicipalityZ", "IslandZ"
        );

        // Property B: invalid WKT, physically we'd consider it at (10..11,10..11),
        // but the geometry won't parse => invalid. Same owner=77 => forced merge.
        PropertyRecord invalidFarRight = new PropertyRecord(
                701,
                701L,
                701L,
                0.0,
                9.0,         // bigger shapeArea to see who is "largest"
                "NOT_VALID_WKT",
                77,          // same owner => forced adjacency if invalid
                "ParishZ", "MunicipalityZ", "IslandZ"
        );

        List<PropertyRecord> props = List.of(validFarLeft, invalidFarRight);

        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);
        assertEquals(1, merged.size(),
                "Despite being far apart (if both were valid), they must forcibly merge because one WKT is invalid and owners match.");

        // In your method, you pick the property with the "largest shapeArea" as the final metadata.
        // invalidFarRight has shapeArea=9.0 vs validFarLeft=1.0 => "largest" is invalidFarRight => objectID=701
        PropertyRecord result = merged.get(0);
        assertEquals(701, result.getObjectID(),
                "Should adopt the largest property’s ID (invalidFarRight) after merge, since shapeArea=9.0 > 1.0.");

        // The code attempts to union the geometry of each.
        // For invalid geometry, parse fails => unionGeom remains from the valid geometry only.
        // So the final shape should be the valid polygon’s area=1.0,
        // but we keep the metadata from 'largest' property => objectID=701, shapeArea=???
        // Let's confirm your method sets shapeArea to unioned geometry area.
        // The union geometry is just the one valid polygon => area=1.0.

        double finalArea = result.getShapeArea();
        assertEquals(1.0, finalArea, 0.0001,
                "Union must be just the valid polygon, so final area=1.0 even though largest had area=9.0 initially.");

        // Confirm the geometry is that single polygon as a MULTIPOLYGON
        org.locationtech.jts.geom.Geometry unionGeom =
                new org.locationtech.jts.io.WKTReader().read(result.getGeometry());
        assertTrue(unionGeom.getGeometryType().contains("MultiPolygon"),
                "Should be a MultiPolygon in final WKT if it was originally one polygon or forced multi.");
        assertEquals(1.0, unionGeom.getArea(), 0.0001,
                "Again, area=1.0 from the single valid geometry, ignoring the invalid one.");
    }

}
