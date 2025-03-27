package iscteiul.ista;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

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
}
