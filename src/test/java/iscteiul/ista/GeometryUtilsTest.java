package iscteiul.ista;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended unit tests for the {@link GeometryUtils} class, verifying adjacency,
 * intersection, disjoint checks, and envelope extraction using JTS-based WKT parsing.
 *
 * <p>This suite covers:
 * <ul>
 *   <li>Edge-touching, corner-touching, and overlapping polygons</li>
 *   <li>LineString vs. Polygon scenarios</li>
 *   <li>Fully contained shapes and negative/large coordinate polygons</li>
 *   <li>Null and invalid WKT inputs</li>
 * </ul>
 *
 * <p>The goal is to thoroughly exercise {@code GeometryUtils} methods,
 * improving both line and branch coverage while demonstrating correctness.
 */
class GeometryUtilsTest {

    /**
     * Tests two squares that share a full boundary edge, verifying:
     * <ul>
     *   <li>{@code areAdjacent} = true</li>
     *   <li>{@code doIntersect} = true</li>
     *   <li>{@code areDisjoint} = false</li>
     * </ul>
     */
    @Test
    void testAreAdjacent_edgeCase() {
        // Two adjacent squares: mp1 is (0,0)-(5,5), mp2 is (5,0)-(10,5)
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp2 = "MULTIPOLYGON(((5 0,10 0,10 5,5 5,5 0)))";

        assertTrue(GeometryUtils.areAdjacent(mp1, mp2),
                "Squares sharing an edge => adjacency = true.");
        assertTrue(GeometryUtils.doIntersect(mp1, mp2),
                "Boundary contact also yields intersection in JTS.");
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2),
                "Sharing a boundary => not disjoint.");
    }

    /**
     * Tests two squares that overlap in their interior, verifying:
     * <ul>
     *   <li>{@code doIntersect} = true</li>
     *   <li>{@code areAdjacent} = false</li>
     *   <li>{@code areDisjoint} = false</li>
     * </ul>
     */
    @Test
    void testDoIntersect_overlapCase() {
        String mp1 = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        String mp2 = "MULTIPOLYGON(((4 4,9 4,9 9,4 9,4 4)))";

        assertTrue(GeometryUtils.doIntersect(mp1, mp2),
                "Overlapping squares share an interior region => intersection = true.");
        assertFalse(GeometryUtils.areAdjacent(mp1, mp2),
                "Area overlap is more than boundary contact => not merely adjacent.");
        assertFalse(GeometryUtils.areDisjoint(mp1, mp2),
                "Not disjoint since they overlap in area.");
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

        assertTrue(GeometryUtils.areDisjoint(mp1, mp3),
                "Squares placed far apart => disjoint = true.");
        assertFalse(GeometryUtils.doIntersect(mp1, mp3),
                "No overlap => no intersection.");
        assertFalse(GeometryUtils.areAdjacent(mp1, mp3),
                "No shared boundary => adjacency = false.");
    }

    /**
     * Tests that {@code null} WKT inputs return {@code false} for all geometry checks
     * without raising exceptions.
     */
    @Test
    void testNullInputs() {
        String valid = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";

        assertFalse(GeometryUtils.areAdjacent(null, valid),
                "Null input => areAdjacent returns false.");
        assertFalse(GeometryUtils.doIntersect(valid, null),
                "Null input => doIntersect returns false.");
        assertFalse(GeometryUtils.areDisjoint(null, null),
                "Null input => areDisjoint returns false.");
    }

    /**
     * Ensures invalid WKT strings do not cause parse exceptions to bubble up, but
     * result in {@code false} for adjacency, intersection, and disjoint checks.
     */
    @Test
    void testInvalidWKT() {
        String invalidWKT = "NOT_A_VALID_POLYGON";
        String validWKT   = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";

        assertFalse(GeometryUtils.areAdjacent(invalidWKT, validWKT),
                "Invalid WKT => adjacency = false.");
        assertFalse(GeometryUtils.doIntersect(validWKT, invalidWKT),
                "Invalid WKT => intersection = false.");
        assertFalse(GeometryUtils.areDisjoint(invalidWKT, validWKT),
                "Invalid WKT => disjoint = false.");
    }

    /**
     * Verifies {@code GeometryUtils.getEnvelope} behavior by providing both a valid WKT polygon
     * and an invalid WKT string. Confirms that:
     * <ul>
     *   <li>A valid polygon returns the correct bounding box (Envelope) coordinates.</li>
     *   <li>An invalid WKT string produces an empty Envelope, rather than throwing an exception.</li>
     * </ul>
     */
    @Test
    void testGetEnvelope() {
        // Valid polygon from (0,0) to (5,5)
        String wkt = "MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)))";
        Envelope envelope = GeometryUtils.getEnvelope(wkt);

        assertNotNull(envelope, "Envelope should not be null for valid WKT.");
        assertEquals(0.0, envelope.getMinX(), 1e-9, "Min X should be 0.0");
        assertEquals(5.0, envelope.getMaxX(), 1e-9, "Max X should be 5.0");
        assertEquals(0.0, envelope.getMinY(), 1e-9, "Min Y should be 0.0");
        assertEquals(5.0, envelope.getMaxY(), 1e-9, "Max Y should be 5.0");

        // Invalid WKT yields an empty envelope
        Envelope invalidEnvelope = GeometryUtils.getEnvelope("INVALID_WKT");
        assertTrue(invalidEnvelope.isNull(),
                "Invalid WKT => empty envelope returned, not an exception.");
    }

    /**
     * Tests polygons that share only a single corner point. JTS usually treats corner
     * contact as "touches," meaning adjacency = true and intersection = true.
     */
    @Test
    void testCornerTouch() {
        // Polygons that meet at corner (3,3)
        String mpA = "MULTIPOLYGON(((0 0,3 0,3 3,0 3,0 0)))";
        String mpB = "MULTIPOLYGON(((3 3,6 3,6 6,3 6,3 3)))";

        assertTrue(GeometryUtils.areAdjacent(mpA, mpB),
                "Corner contact => adjacency = true.");
        assertTrue(GeometryUtils.doIntersect(mpA, mpB),
                "Corner contact also means intersection in JTS terms.");
        assertFalse(GeometryUtils.areDisjoint(mpA, mpB),
                "Shared corner => not disjoint.");
    }

    /**
     * Confirms behavior when a polygon and LINESTRING share a boundary point.
     * Even if your domain mostly uses polygons, this scenario ensures coverage
     * for different geometry types in the adjacency logic.
     */
    @Test
    void testLineStringPolygon() {
        String polygon = "POLYGON((0 0,0 5,5 5,5 0,0 0))";
        // LINESTRING that touches the polygon boundary at (5,2)
        String line    = "LINESTRING(5 2, 7 2)";

        assertTrue(GeometryUtils.areAdjacent(polygon, line),
                "Line touching polygon boundary => adjacency = true.");
        assertTrue(GeometryUtils.doIntersect(polygon, line),
                "Boundary contact => intersection in JTS.");
        assertFalse(GeometryUtils.areDisjoint(polygon, line),
                "Shared boundary => not disjoint.");
    }

    /**
     * Tests a shape fully contained inside another (no shared boundary).
     * This indicates they do intersect, but they're not "adjacent."
     */
    @Test
    void testContainedPolygon() {
        // Outer from (0,0)-(10,10), inner from (2,2)-(3,3)
        String outer = "POLYGON((0 0,0 10,10 10,10 0,0 0))";
        String inner = "POLYGON((2 2,2 3,3 3,3 2,2 2))";

        assertFalse(GeometryUtils.areAdjacent(outer, inner),
                "No boundary contact => not adjacent, just contained.");
        assertTrue(GeometryUtils.doIntersect(outer, inner),
                "Fully within => intersection = true.");
        assertFalse(GeometryUtils.areDisjoint(outer, inner),
                "They overlap in the interior => not disjoint.");
    }

    /**
     * Checks large negative coordinate polygons overlapping partially,
     * ensuring bounding boxes and geometry checks behave correctly beyond
     * simple positive coordinates.
     */
    @Test
    void testLargeAndNegativeCoordinates() {
        // Big negative polygon from (-100,-100) to (100,100)
        String bigNeg = "POLYGON((-100 -100,-100 100,100 100,100 -100,-100 -100))";
        // Another polygon that partially overlaps
        String partialOverlap = "POLYGON((-50 -50,-50 50,50 50,50 -50,-50 -50))";

        // Overlap => intersection = true, not adjacency
        assertFalse(GeometryUtils.areAdjacent(bigNeg, partialOverlap),
                "They overlap in area => not just touching => adjacency = false.");
        assertTrue(GeometryUtils.doIntersect(bigNeg, partialOverlap),
                "Significant overlap => doIntersect = true.");
        assertFalse(GeometryUtils.areDisjoint(bigNeg, partialOverlap),
                "Overlap => not disjoint.");

        // Check bounding box for bigNeg
        Envelope envNeg = GeometryUtils.getEnvelope(bigNeg);
        assertEquals(-100.0, envNeg.getMinX(), 1e-9,
                "Expected minX = -100 for bigNeg polygon.");
        assertEquals(100.0, envNeg.getMaxX(), 1e-9,
                "Expected maxX = 100 for bigNeg polygon.");
        assertEquals(-100.0, envNeg.getMinY(), 1e-9,
                "Expected minY = -100 for bigNeg polygon.");
        assertEquals(100.0, envNeg.getMaxY(), 1e-9,
                "Expected maxY = 100 for bigNeg polygon.");
    }
}
