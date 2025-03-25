package iscteiul.ista;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Example JUnit test class for the {@link PropertyUtils} utility methods.
 *
 * <p>
 * Adjust sample data to reflect real geometry (valid WKT polygons) if you're testing
 * adjacency via {@link GeometryUtils}. The included polygons here are just an example of
 * valid WKT. Two of them share an edge (making them adjacent).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PropertyUtilsTest {

    private static List<PropertyRecord> sampleRecords;
    private static PropertyRecord rec1, rec2, rec3;

    /**
     * Set up sample records before running all tests.
     */
    @BeforeAll
    static void setUpBeforeAll() {
        sampleRecords = new ArrayList<>();

        /*
         * Below, we create three PropertyRecord objects with:
         * - distinct owners and municipalities
         * - geometry in actual WKT format so adjacency checks can work:
         *   rec1 and rec2 share a boundary, rec3 is far away.
         */

        // Example geometry #1: a 1x1 square from (0,0) to (1,1).
        rec1 = new PropertyRecord(
                1,                   // objectID
                7343148L,            // parcelID
                2996240000000L,      // parcelNumber
                57.2469,             // shapeLength
                202.0598,            // shapeArea
                "POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))",  // geometry (valid WKT)
                93,                  // owner
                "Arco da Calheta",   // parish
                "Calheta",           // municipality
                "Ilha da Madeira (Madeira)"  // island
        );

        // Example geometry #2: a 1x1 square that shares the vertical edge at x=1 with rec1.
        // That means they should be "touching" and considered adjacent if your GeometryUtils
        // adjacency check is correct.
        rec2 = new PropertyRecord(
                2,
                7343149L,
                2996240000001L,
                50.0,
                300.0,
                "POLYGON((1 0, 2 0, 2 1, 1 1, 1 0))",
                93,                   // same owner as rec1 (93)
                "Some Parish",
                "Calheta",            // same municipality as rec1
                "Ilha da Madeira (Madeira)"
        );

        // Example geometry #3: placed far away, so it's not adjacent to rec1 or rec2.
        rec3 = new PropertyRecord(
                3,
                1234567L,
                9876543210000L,
                120.5,
                600.0,
                "POLYGON((10 10, 11 10, 11 11, 10 11, 10 10))",
                999,           // different owner
                "NA",          // might represent no parish
                "Funchal",     // different municipality
                "Ilha da Madeira (Madeira)"
        );

        // Add them to the sample list.
        sampleRecords.add(rec1);
        sampleRecords.add(rec2);
        sampleRecords.add(rec3);
    }

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
        // Owner ID = 93 should return rec1 and rec2
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 93);
        assertEquals(2, found.size(), "Should find 2 properties owned by 93.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list should contain rec1 and rec2.");
    }

    @Test
    @Order(2)
    void testFindByOwner_nonExistingOwner() {
        // Owner 555 doesn't exist in the sample data
        List<PropertyRecord> found = PropertyUtils.findByOwner(sampleRecords, 555);
        assertTrue(found.isEmpty(), "Should return an empty list if the owner doesn't exist.");
    }

    // ------------------------------------------------------------------------
    // TESTS for findByMunicipality()
    // ------------------------------------------------------------------------

    @Test
    @Order(3)
    void testFindByMunicipality_match() {
        // "Calheta" is the municipality for rec1 and rec2
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "Calheta");
        assertEquals(2, found.size(), "Calheta should have 2 properties in the sample data.");
        assertTrue(found.contains(rec1) && found.contains(rec2),
                "The returned list for Calheta should contain rec1 and rec2.");
    }

    @Test
    @Order(4)
    void testFindByMunicipality_noMatch() {
        // This municipality doesn't match any record
        List<PropertyRecord> found = PropertyUtils.findByMunicipality(sampleRecords, "SomethingElse");
        assertTrue(found.isEmpty(), "No property should match municipality 'SomethingElse'.");
    }

    @Test
    @Order(5)
    void testFindByMunicipality_nullArg() {
        // Should gracefully return an empty list
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
        // All sample records have "Ilha da Madeira (Madeira)"
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha da Madeira (Madeira)");
        assertEquals(3, found.size(), "Should find all 3 sample records on 'Ilha da Madeira (Madeira)'.");
        assertTrue(found.contains(rec1) && found.contains(rec2) && found.contains(rec3),
                "The returned list should contain rec1, rec2, and rec3.");
    }

    @Test
    @Order(7)
    void testFindByIsland_noMatch() {
        // This island doesn't match any record
        List<PropertyRecord> found = PropertyUtils.findByIsland(sampleRecords, "Ilha do Porto Santo");
        assertTrue(found.isEmpty(), "Should return empty if no records are on that island.");
    }

    @Test
    @Order(8)
    void testFindByIsland_nullArg() {
        // Should gracefully return empty list
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
        // rec1 and rec2 share a boundary in their WKT polygons
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec2);
        assertTrue(adjacent, "rec1 and rec2 should be adjacent based on their shared boundary.");
    }

    @Test
    @Order(10)
    void testArePropertiesAdjacent_false() {
        // rec1 is far from rec3
        boolean adjacent = PropertyUtils.arePropertiesAdjacent(rec1, rec3);
        assertFalse(adjacent, "rec1 and rec3 should NOT be adjacent.");
    }

    @Test
    @Order(11)
    void testFindAdjacentProperties_forRec1() {
        // rec1 is only adjacent to rec2
        List<PropertyRecord> adjacentList = PropertyUtils.findAdjacentProperties(rec1, sampleRecords);
        assertEquals(1, adjacentList.size(), "rec1 should be adjacent to exactly 1 property (rec2).");
        assertTrue(adjacentList.contains(rec2), "rec2 should appear in the adjacency list for rec1.");
    }

    @Test
    @Order(12)
    void testFindAdjacentProperties_nullRecordOrList() {
        // If we pass a null record or null list, we expect an empty result
        List<PropertyRecord> nullRecordTest = PropertyUtils.findAdjacentProperties(null, sampleRecords);
        List<PropertyRecord> nullListTest   = PropertyUtils.findAdjacentProperties(rec1, null);

        assertTrue(nullRecordTest.isEmpty(), "If the record is null, should return empty list.");
        assertTrue(nullListTest.isEmpty(),   "If the list is null, should return empty list.");
    }
}
