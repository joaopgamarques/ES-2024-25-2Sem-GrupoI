package iscteiul.ista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link GeometryUtils} class, verifying adjacency,
 * intersection, and disjoint checks using JTS-based WKT parsing.
 *
 * <p>This test suite ensures:
 * <ul>
 *   <li>Squares that share an edge are considered adjacent and intersecting.</li>
 *   <li>Overlapping polygons intersect but are not merely adjacent.</li>
 *   <li>Fully separated polygons are disjoint.</li>
 *   <li>{@code null} and invalid WKT strings produce false for all checks.</li>
 * </ul>
 *
 * <p>The geometry strings in these tests are simplified square polygons,
 * intended to highlight specific relationships such as edge-touching and overlaps.
 */
class GeometryUtilsTest {

    /**
     * Tests two squares that share an edge (touch), verifying:
     * <ul>
     *   <li>{@code areAdjacent} = true</li>
     *   <li>{@code doIntersect} = true</li>
     *   <li>{@code areDisjoint} = false</li>
     * </ul>
     */
    @Test
    void testAreAdjacent_edgeCase() {
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp2 = "MULTIPOLYGON(((5 0,10 0,10 5,5 5,5 0)))";

        assertTrue(GeometryUtils.areAdjacent(mp1, mp2), "Squares sharing an edge should be adjacent.");
        assertTrue(GeometryUtils.doIntersect(mp1, mp2), "They intersect at the shared boundary.");
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2), "Not disjoint since they touch.");
    }

    /**
     * Tests two squares that overlap by some area, verifying:
     * <ul>
     *   <li>{@code doIntersect} = true</li>
     *   <li>{@code areAdjacent} = false (they intersect, not just touch)</li>
     *   <li>{@code areDisjoint} = false</li>
     * </ul>
     */
    @Test
    void testDoIntersect_overlapCase() {
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp2 = "MULTIPOLYGON(((4 4,9 4,9 9,4 9,4 4)))";

        assertTrue(GeometryUtils.doIntersect(mp1, mp2), "Overlapping squares intersect.");
        assertFalse(GeometryUtils.areAdjacent(mp1, mp2), "Overlapping region is more than a shared boundary.");
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2), "They share interior points, so not disjoint.");
    }

    /**
     * Tests two squares that neither touch nor overlap, verifying:
     * <ul>
     *   <li>{@code areDisjoint} = true</li>
     *   <li>{@code doIntersect} = false</li>
     *   <li>{@code areAdjacent} = false</li>
     * </ul>
     */
    @Test
    void testAreDisjoint() {
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp3 = "MULTIPOLYGON(((6 6,10 6,10 10,6 10,6 6)))";

        assertTrue(GeometryUtils.areDisjoint(mp1, mp3), "They are fully separated, so disjoint.");
        assertFalse(GeometryUtils.doIntersect(mp1, mp3), "No intersection if disjoint.");
        assertFalse(GeometryUtils.areAdjacent(mp1, mp3), "No shared boundary => not adjacent.");
    }

    /**
     * Tests that {@code null} WKT inputs yield false for all geometry checks
     * (adjacency, intersection, disjoint) without throwing exceptions.
     */
    @Test
    void testNullInputs() {
        String valid = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";

        assertFalse(GeometryUtils.areAdjacent(null, valid), "Null input => false adjacency.");
        assertFalse(GeometryUtils.doIntersect(valid, null), "Null input => false intersection.");
        assertFalse(GeometryUtils.areDisjoint(null, null), "Null input => false disjoint.");
    }

    /**
     * Tests invalid WKT strings to confirm they produce false for all checks
     * and do not throw parse exceptions to the caller.
     */
    @Test
    void testInvalidWKT() {
        String invalidWKT = "NOT_A_VALID_POLYGON";
        String validWKT   = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";

        assertFalse(GeometryUtils.areAdjacent(invalidWKT, validWKT), "Invalid WKT => false adjacency.");
        assertFalse(GeometryUtils.doIntersect(validWKT, invalidWKT), "Invalid WKT => false intersection.");
        assertFalse(GeometryUtils.areDisjoint(invalidWKT, validWKT), "Invalid WKT => false disjoint.");
    }
}
