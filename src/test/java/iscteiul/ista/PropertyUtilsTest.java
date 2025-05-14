package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PropertyUtilsTest</h2>
 *
 * <p>
 * This test class covers the functionality in {@link PropertyUtils}, including:
 * <ul>
 *   <li>Filtering {@link PropertyRecord} by owner, municipality, island, or parish</li>
 *   <li>Checking adjacency (touching) of properties using WKT polygons</li>
 *   <li>Computing average areas, grouped areas, and merging properties owned by the same owner</li>
 *   <li>Distance calculations to Funchal (Sé) and Machico reference properties via
 *       {@link App#getFunchalPropertyRecord()} and {@link App#getMachicoPropertyRecord()}</li>
 * </ul>
 *
 * <p>This test suite uses a small set of sample records to exercise each method,
 * ensuring correct behavior for valid, invalid, and edge-case scenarios.</p>
 *
 * @see PropertyUtils
 * @see PropertyRecord
 * @see App
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PropertyUtilsTest {

    /**
     * A shared list of sample {@link PropertyRecord} objects for testing.
     * Populated in {@link #setUpBeforeAll()} and cleared in {@link #tearDownAfterAll()}.
     */
    private static List<PropertyRecord> sampleRecords;

    /** A sample property (#1) that is adjacent to {@code rec2}. */
    private static PropertyRecord rec1;

    /** A sample property (#2) that is adjacent to {@code rec1}. */
    private static PropertyRecord rec2;

    /** A sample property (#3) placed far away (non-adjacent to the others). */
    private static PropertyRecord rec3;

    /**
     * Initializes a few sample properties before any tests run,
     * assigning them into {@link #sampleRecords} and also populating
     * {@code App.propertyRecords} so that distance-related methods
     * (e.g., {@link PropertyUtils#distanceToFunchal(int)}) won't
     * encounter a null list.
     */
    @BeforeAll
    static void setUpBeforeAll() {
        sampleRecords = new ArrayList<>();

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

        // rec3: placed far away at (10,10)-(11,11).
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

        // *** IMPORTANT: Populate App.propertyRecords so distanceToFunchal(...) won't fail with NPE. ***
        App.setPropertyRecords(sampleRecords);
    }

    /**
     * Clears the shared {@link #sampleRecords} list after all tests finish.
     */
    @AfterAll
    static void tearDownAfterAll() {
        sampleRecords.clear();
        // Optionally also clear App.propertyRecords if you want:
        // App.setPropertyRecords(null);
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
                "The returned list should contain rec1 and rec2 for owner=93.");
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
        assertNotNull(found, "Should return a non-null empty list for null input.");
        assertTrue(found.isEmpty(), "Should return an empty list if municipality is null.");
    }

    // ------------------------------------------------------------------------
    // TESTS for findByIsland()
    // ------------------------------------------------------------------------
    @Test
    @Order(6)
    void testFindByIsland_match() {
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha da Madeira (Madeira)");
        assertEquals(3, found.size(), "Should find all 3 sample records for that island name.");
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
        assertTrue(found.isEmpty(), "Should return an empty list for null island input.");
    }

    // ------------------------------------------------------------------------
    // TESTS for adjacency: arePropertiesAdjacent() + findAdjacentProperties()
    // ------------------------------------------------------------------------
    @Test
    @Order(9)
    void testArePropertiesAdjacent_true() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec2);
        assertTrue(adjacent, "rec1 and rec2 share a boundary => should be adjacent.");
    }

    @Test
    @Order(10)
    void testArePropertiesAdjacent_false() {
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec3);
        assertFalse(adjacent, "rec1 and rec3 are far apart => not adjacent.");
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
                "Should include 'Arco da Calheta' in the set of parishes.");
        assertTrue(distinct.contains("Some Parish"),
                "Should include 'Some Parish' in the set of parishes.");
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
                "Expected exactly 2 unique municipality names: Calheta & Funchal.");
    }

    // ------------------------------------------------------------------------
    // Additional tests for findByOwner(), average area, grouping, etc.
    // ------------------------------------------------------------------------
    @Test
    @Order(15)
    void testFindByOwner_sameOwnerAsRec3() {
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 999);
        assertEquals(1, found.size(), "We expect exactly 1 property (rec3) for owner=999.");
        assertTrue(found.contains(rec3), "The found list should include only rec3 for owner=999.");
    }

    @Test
    @Order(16)
    void testCalculateAverageArea_valid() {
        double average = PropertyUtils.calculateAverageArea(sampleRecords);
        // rec1.area=202.0598, rec2.area=300.0, rec3.area=600.0
        // sum=1102.0598, count=3 => average ~367.353266
        assertEquals(367.35, average, 0.01,
                "Expected average area ~367.35 for the sampleRecords.");
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
        // We expect 2 owners: 93 (rec1, rec2), 999 (rec3).
        assertEquals(2, grouped.size(), "Expected exactly 2 owners in the grouped map: 93 and 999.");
        assertTrue(grouped.containsKey(93),  "Should have a key for owner=93.");
        assertTrue(grouped.containsKey(999), "Should have a key for owner=999.");
        assertEquals(2, grouped.get(93).size(),
                "Owner=93 should have 2 properties (rec1 and rec2).");
        assertEquals(1, grouped.get(999).size(),
                "Owner=999 should have 1 property (rec3).");
    }

    @Test
    @Order(19)
    void testCalculateAverageGroupedArea_nullGraph() {
        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, null);
        assertEquals(0.0, average,
                "Expected average grouped area to be 0.0 with a null graph input.");
    }

    @Test
    @Order(20)
    void testCalculateAverageGroupedArea_connected() {
        // Build a small graph that connects rec1 and rec2, leaving rec3 disconnected
        SimpleGraph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);
        graph.addEdge(rec1, rec2);  // connect these two

        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);
        // rec1 + rec2 => total area = 202.0598 + 300 = 502.0598
        // rec3 => 600.0
        // 2 connected components => average => (502.0598 + 600) / 2 = 551.0299
        assertEquals(551.0299, average, 0.01,
                "The average of the 2 connected groups is incorrect.");
    }

    @Test
    @Order(21)
    void testCalculateAverageGroupedArea_disconnected() {
        // No edges => rec1, rec2, rec3 each is a separate connected component
        SimpleGraph<PropertyRecord, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(rec1);
        graph.addVertex(rec2);
        graph.addVertex(rec3);

        double average = PropertyUtils.calculateAverageGroupedArea(sampleRecords, graph);
        // sum=1102.0598 / 3 => ~367.3532
        double roundedAverage = Math.round(average * 100.0) / 100.0;
        assertEquals(367.35, roundedAverage, 0.01,
                "The average area of disconnected groups is incorrect.");
    }

    // ------------------------------------------------------------------------
    // TESTS for distanceToFunchal (no-arg version calls App.getPropertyRecords())
    // ------------------------------------------------------------------------
    @Test
    @Order(22)
    void testDistanceToFunchal_validCase() {
        // define a valid "funchal" record => centroid(2.5,2.5)
        PropertyRecord funchalSe = new PropertyRecord(
                11074,
                999999L,
                888888L,
                0.0,
                0.0,
                "POLYGON((2 2,3 2,3 3,2 3,2 2))", // centroid(2.5,2.5)
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        // set it in App
        App.setFunchalPropertyRecord(funchalSe);

        // Now call distance => rec1 has centroid(0.5,0.5)
        double dist = PropertyUtils.distanceToFunchal(1);

        // => distance ~ sqrt((2.0)^2 + (2.0)^2)=2.8284
        assertFalse(Double.isNaN(dist),
                "Distance should be valid with both reference & source geometry valid.");
        assertEquals(2.8284, dist, 1e-3,
                "Distance between (0.5,0.5) & (2.5,2.5) => ~2.8284");
    }

    @Test
    @Order(23)
    void testDistanceToFunchal_missingFunchalRecord() {
        // Nullify the reference => simulate "Funchal not found"
        App.setFunchalPropertyRecord(null);

        // distance => should be NaN
        double dist = PropertyUtils.distanceToFunchal(1);
        assertTrue(Double.isNaN(dist),
                "distanceToFunchal must be NaN if the Funchal reference is null.");
    }

    @Test
    @Order(24)
    void testDistanceToFunchal_missingSourceProperty() {
        // define a valid Funchal reference
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

        // propertyId=9999 doesn't exist in sampleRecords => distance => NaN
        double dist = PropertyUtils.distanceToFunchal(9999);
        assertTrue(Double.isNaN(dist),
                "Should be NaN because propertyId=9999 isn't in sampleRecords.");
    }

    @Test
    @Order(25)
    void testDistanceToFunchal_invalidOrBlankGeometry() {
        // (a) blank geometry
        PropertyRecord blankGeomFunchal = new PropertyRecord(
                11074, 999999L, 888888L,
                0.0, 0.0,
                "", // blank
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        App.setFunchalPropertyRecord(blankGeomFunchal);

        double dist = PropertyUtils.distanceToFunchal(1);
        assertTrue(Double.isNaN(dist),
                "Blank geometry => distance must be NaN.");

        // (b) invalid geometry
        PropertyRecord invalidGeomFunchal = new PropertyRecord(
                11074, 999999L, 888888L,
                0.0, 0.0,
                "NOT_A_VALID_WKT",
                1234,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira (Madeira)"
        );
        App.setFunchalPropertyRecord(invalidGeomFunchal);

        dist = PropertyUtils.distanceToFunchal(1);
        assertTrue(Double.isNaN(dist),
                "Invalid geometry => distance must be NaN.");
    }

    // ------------------------------------------------------------------------
    // TESTS for distanceToMachico (no-arg version calls App.getPropertyRecords())
    // ------------------------------------------------------------------------
    @Test
    @Order(26)
    void testDistanceToMachico_validCase() {
        // rec1 => centroid(0.5,0.5)
        // define Machico => centroid(2.5,2.5)
        PropertyRecord machicoRef = new PropertyRecord(
                11517,
                999999L,
                888888L,
                0.0,
                0.0,
                "POLYGON((2 2,3 2,3 3,2 3,2 2))",
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        App.setMachicoPropertyRecord(machicoRef);

        double dist = PropertyUtils.distanceToMachico(1);
        // => sqrt((2.0)^2 + (2.0)^2)=2.8284
        assertFalse(Double.isNaN(dist),
                "Distance should be valid with reference & source geometry valid.");
        assertEquals(2.8284, dist, 1e-3,
                "Distance (0.5,0.5)->(2.5,2.5) => 2.8284");
    }

    @Test
    @Order(27)
    void testDistanceToMachico_missingMachicoRecord() {
        // Nullify => Machico reference is absent
        App.setMachicoPropertyRecord(null);

        double dist = PropertyUtils.distanceToMachico(1);
        assertTrue(Double.isNaN(dist),
                "distanceToMachico must be NaN if reference is null.");
    }

    @Test
    @Order(28)
    void testDistanceToMachico_missingSourceProperty() {
        // define a valid Machico
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

        // 9999 not in sampleRecords => distance => NaN
        double dist = PropertyUtils.distanceToMachico(9999);
        assertTrue(Double.isNaN(dist),
                "Should be NaN if the source property is not in sampleRecords.");
    }

    @Test
    @Order(29)
    void testDistanceToMachico_invalidOrBlankGeometry() {
        // (a) blank geometry
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

        double dist = PropertyUtils.distanceToMachico(1);
        assertTrue(Double.isNaN(dist),
                "Blank geometry => distance must be NaN.");

        // (b) invalid geometry
        PropertyRecord invalidGeomMachico = new PropertyRecord(
                11517, 999999L, 888888L,
                0.0, 0.0,
                "NOT_A_VALID_WKT",
                1234,
                "Machico",
                "Machico",
                "Ilha da Madeira (Madeira)"
        );
        App.setMachicoPropertyRecord(invalidGeomMachico);

        dist = PropertyUtils.distanceToMachico(1);
        assertTrue(Double.isNaN(dist),
                "Invalid geometry => distance must be NaN.");
    }

    // ------------------------------------------------------------------------
    // TESTS for mergeAdjacentPropertiesSameOwner()
    // ------------------------------------------------------------------------
    private static final WKTReader WKT_READER = new WKTReader();

    /**
     * Merging two adjacent 1x1 squares of the same owner yields a single property with area=2.
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
                10,
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
                10,
                "ParishA",
                "MunicipalityA",
                "IslandA"
        );

        List<PropertyRecord> props = new ArrayList<>();
        props.add(recA);
        props.add(recB);

        List<PropertyRecord> mergedList = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);
        assertEquals(1, mergedList.size(), "Two adjacent squares => one merged result.");

        PropertyRecord merged = mergedList.get(0);
        Geometry geom = WKT_READER.read(merged.getGeometry());
        assertEquals(2.0, geom.getArea(), 1e-4,
                "Merged area of adjacent 1x1 squares should be 2.0");
        assertTrue(geom.getGeometryType().contains("MultiPolygon"),
                "Union of a single Polygon is forced to MultiPolygon.");
        assertEquals(10, merged.getOwner(),
                "Owner should remain the same (10).");
    }

    /**
     * Merging three 1x1 squares in a row (all same owner) => a single property with area=3.0.
     */
    @Test
    @Order(31)
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
                "Three squares in a row => single merged property.");

        Geometry unionGeom = WKT_READER.read(merged.get(0).getGeometry());
        assertEquals(3.0, unionGeom.getArea(), 1e-4,
                "Three adjacent 1x1 squares => area=3.0");
    }

    /**
     * If two same-owner properties are not adjacent, no merging occurs.
     */
    @Test
    @Order(32)
    void testNoMergeIfNotAdjacent() {
        PropertyRecord recA = new PropertyRecord(
                301, 2000L, 3001L, 4.0, 1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                77, "ParishA", "MunicipalityA", "IslandA");
        PropertyRecord recB = new PropertyRecord(
                302, 2001L, 3002L, 4.0, 1.0,
                "POLYGON((10 10,11 10,11 11,10 11,10 10))",
                77, "ParishA", "MunicipalityA", "IslandA");

        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(List.of(recA, recB));
        assertEquals(2, merged.size(),
                "No adjacency => no merge => both remain separate.");
        assertTrue(merged.contains(recA) && merged.contains(recB),
                "No changes if they are not adjacent.");
    }

    /**
     * Different owners, even if geometry is adjacent, must not be merged.
     */
    @Test
    @Order(33)
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
     * If geometry is invalid, it is skipped. If all are invalid, fallback to largest property.
     */
    @Test
    @Order(34)
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
                "Same owner => forcibly merges to 1; ignoring invalid WKT for adjacency.");

        Geometry finalGeom = WKT_READER.read(merged.get(0).getGeometry());
        assertEquals(1.0, finalGeom.getArea(), 1e-4,
                "Should be just the valid polygon’s area=1.0, ignoring the invalid geometry.");
    }

    /**
     * Null or empty input yields an empty list without errors.
     */
    @Test
    @Order(35)
    void testEmptyOrNullInput() {
        assertTrue(PropertyUtils.mergeAdjacentPropertiesSameOwner(new ArrayList<>()).isEmpty(),
                "Empty input => empty result.");
        assertTrue(PropertyUtils.mergeAdjacentPropertiesSameOwner(null).isEmpty(),
                "Null input => empty result.");
    }

    /**
     * A single property cannot be merged with anything; it remains unchanged.
     */
    @Test
    @Order(36)
    void testSinglePropertyNoMerge() {
        PropertyRecord single = new PropertyRecord(
                601, 5000L, 6001L, 4.0, 2.0,
                "POLYGON((0 0,2 0,2 1,0 1,0 0))",
                999, "SoloParish", "SoloMunicipality", "SoloIsland");
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(List.of(single));
        assertEquals(1, merged.size(),
                "Single property => no merges => returns same property.");
        assertSame(single, merged.get(0),
                "Should return the same instance if there's nothing else to merge with.");
    }

    /**
     * Valid geometry plus invalid geometry with same owner => forcibly merges,
     * adopting largest's ID but area from the valid geometry only.
     */
    @Test
    @Order(37)
    void testMergeSameOwnerOneInvalidFarAway() throws Exception {
        // Property A: valid geometry at (0..1,0..1), owner=77
        PropertyRecord validFarLeft = new PropertyRecord(
                700,
                700L,
                700L,
                0.0,
                1.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                77,
                "ParishZ", "MunicipalityZ", "IslandZ"
        );

        // Property B: invalid geometry, shapeArea=9.0 => largest.
        // They share the same owner => forced adjacency => merges
        PropertyRecord invalidFarRight = new PropertyRecord(
                701,
                701L,
                701L,
                0.0,
                9.0,
                "NOT_VALID_WKT",
                77,
                "ParishZ", "MunicipalityZ", "IslandZ"
        );

        List<PropertyRecord> props = List.of(validFarLeft, invalidFarRight);
        List<PropertyRecord> merged = PropertyUtils.mergeAdjacentPropertiesSameOwner(props);

        assertEquals(1, merged.size(),
                "Both same owner => forcibly one merged property.");
        PropertyRecord result = merged.get(0);
        assertEquals(701, result.getObjectID(),
                "Should adopt objectID from the largest property (shapeArea=9).");

        // But actual union geometry is only from the valid polygon => area=1.0
        double finalArea = result.getShapeArea();
        assertEquals(1.0, finalArea, 1e-4,
                "Union excludes invalid geometry => final area=1.0.");

        Geometry unionGeom = new WKTReader().read(result.getGeometry());
        assertTrue(unionGeom.getGeometryType().contains("MultiPolygon"),
                "Should be MultiPolygon if forcibly merged with at least one valid geometry.");
        assertEquals(1.0, unionGeom.getArea(), 1e-4,
                "Again, area=1.0 from the single valid geometry.");
    }
}
