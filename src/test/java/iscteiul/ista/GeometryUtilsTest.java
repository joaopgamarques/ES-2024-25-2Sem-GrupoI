package iscteiul.ista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link GeometryUtils} class.
 */
class GeometryUtilsTest {

    /**
     * Simple test where two squares share an edge (touch), so areAdjacent = true, doIntersect = true.
     */
    @Test
    void testAreAdjacent_edgeCase() {
        // mp1 is a square from (0,0) to (5,5).
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        // mp2 is a square from (5,0) to (10,5). They share a boundary at x=5.
        String mp2 = "MULTIPOLYGON(((5 0,10 0,10 5,5 5,5 0)))";

        assertTrue(GeometryUtils.areAdjacent(mp1, mp2), "Squares sharing an edge should be adjacent.");
        // Because they share a boundary, doIntersect is also true in JTS (touches is a subset of intersects).
        assertTrue(GeometryUtils.doIntersect(mp1, mp2), "They intersect at boundary as well.");
        // They obviously are not disjoint if they touch
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2), "Not disjoint if they share a boundary.");
    }

    /**
     * Test where two squares overlap in an area, so doIntersect = true, areAdjacent = false.
     */
    @Test
    void testDoIntersect_overlapCase() {
        // mp1 from (0,0) to (5,5).
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        // mp2 from (4,4) to (9,9). Overlaps with mp1's top-right corner, not just touching the edge.
        String mp2 = "MULTIPOLYGON(((4 4,9 4,9 9,4 9,4 4)))";

        assertTrue(GeometryUtils.doIntersect(mp1, mp2), "Overlapping squares should intersect.");
        // Because they overlap, they are not just touching at a boundary => areAdjacent should be false.
        assertFalse(GeometryUtils.areAdjacent(mp1, mp2), "They overlap in an area, so not merely adjacent.");
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2), "They are not disjoint since they share interior points.");
    }

    /**
     * Test where two squares do not overlap or touch at all, so areDisjoint = true.
     */
    @Test
    void testAreDisjoint() {
        // mp1 from (0,0) to (5,5).
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        // mp3 from (6,6) to (10,10). No shared boundary or overlap.
        String mp3 = "MULTIPOLYGON(((6 6,10 6,10 10,6 10,6 6)))";

        assertTrue(GeometryUtils.areDisjoint(mp1, mp3), "These squares don't touch or overlap, so disjoint should be true.");
        assertFalse(GeometryUtils.doIntersect(mp1, mp3), "No intersection if they're disjoint.");
        assertFalse(GeometryUtils.areAdjacent(mp1, mp3), "They are not even touching, so not adjacent.");
    }

    /**
     * Test for null WKT inputs to ensure the methods handle or log warnings gracefully.
     */
    @Test
    void testNullInputs() {
        // One or both WKT strings are null => Should return false and log a warning
        assertFalse(GeometryUtils.areAdjacent(null, "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))"));
        assertFalse(GeometryUtils.doIntersect("MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))", null));
        assertFalse(GeometryUtils.areDisjoint(null, null));
    }

    /**
     * Test for invalid WKT format. The methods should catch ParseException and return false.
     */
    @Test
    void testInvalidWKT() {
        // Invalid WKT string
        String invalidWKT = "NOT_A_VALID_POLYGON";
        String validWKT   = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";

        assertFalse(GeometryUtils.areAdjacent(invalidWKT, validWKT), "Invalid WKT should result in false adjacency.");
        assertFalse(GeometryUtils.doIntersect(validWKT, invalidWKT), "Invalid WKT => false for intersection.");
        assertFalse(GeometryUtils.areDisjoint(invalidWKT, validWKT), "Should also return false for disjoint.");
    }
}
