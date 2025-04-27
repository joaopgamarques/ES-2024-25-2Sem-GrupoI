package iscteiul.ista;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PostGISReaderTest &mdash; Integration Tests for {@link PostGISReader}</h2>
 *
 * <p>This test class demonstrates end-to-end verification of the
 * {@link PostGISReader} functionality in a real PostgreSQL/PostGIS environment.
 * Specifically, it:</p>
 * <ol>
 *   <li>Clears any stray test rows from prior runs and inserts a small set of
 *       known test geometries ({@code #101}, {@code #102}, {@code #103}).</li>
 *   <li>Executes queries using the four core spatial predicates
 *       (<em>ST_Touches</em>, <em>ST_Intersects</em>, <em>ST_Overlaps</em>,
 *       <em>ST_Contains</em>) against object {@code #101}.</li>
 *   <li>Verifies that the returned neighbor object IDs match the expected
 *       set. For example, {@code #101} "touches" {@code #102}, but does not
 *       overlap it.</li>
 *   <li>Removes the test data afterward, ensuring repeated test executions
 *       remain consistent (idempotent).</li>
 * </ol>
 *
 * <p><strong>Important:</strong> To run these tests, you need:</p>
 * <ul>
 *   <li>A running PostgreSQL server with PostGIS extension enabled.</li>
 *   <li>The {@code properties} table defined in the <em>public</em> schema, or
 *       update the constants in this file (and in {@link PostGISReader}) to match
 *       your DB schema or credentials.</li>
 * </ul>
 *
 * @author
 *   Integration-Test Team (2025) &mdash; Update to your actual team name/author
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostGISReaderTest {

    /**
     * The JDBC URL to connect to the PostgreSQL database, specifying the
     * {@code public} schema as the search path.
     */
    private static final String JDBC_URL =
            "jdbc:postgresql://localhost:5432/postgres?currentSchema=public";

    /** Database username (with insert/select/delete privileges). */
    private static final String DB_USER  = "postgres";

    /** Password corresponding to {@link #DB_USER}. */
    private static final String DB_PASS  = "ES2425GI";

    /**
     * A logger for printing diagnostic information during test execution.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PostGISReaderTest.class);

    /**
     * A minimal set of sample polygons:
     * <ul>
     *   <li><strong>#101</strong> is a square from (0,0) to (1,1).</li>
     *   <li><strong>#102</strong> shares a boundary with #101 at x=1.</li>
     *   <li><strong>#103</strong> is far away, located at (10,10) to (11,11).</li>
     * </ul>
     */
    private static final List<PropertyRecord> SAMPLE = List.of(
            new PropertyRecord(
                    101,        /* objectID */
                    1L,         /* parcelID */
                    1L,         /* parcelNumber */
                    0,          /* shapeLength (unused in these tests) */
                    0,          /* shapeArea   (unused in these tests) */
                    "POLYGON((0 0,0 1,1 1,1 0,0 0))",  /* geometry (square) */
                    1,          /* owner */
                    "Parish", "Municip", "Island"
            ),
            new PropertyRecord(
                    102,
                    2L,
                    2L,
                    0,
                    0,
                    "POLYGON((1 0,1 1,2 1,2 0,1 0))",  /* geometry (touches #101) */
                    2,
                    "Parish", "Municip", "Island"
            ),
            new PropertyRecord(
                    103,
                    3L,
                    3L,
                    0,
                    0,
                    "POLYGON((10 10,10 11,11 11,11 10,10 10))", /* far away square */
                    3,
                    "Parish", "Municip", "Island"
            )
    );

    /**
     * Inserts {@code SAMPLE} data into the table before all tests, ensuring a
     * known starting state. Also cleans up any prior copies of these IDs to
     * avoid duplication across runs.
     *
     * @throws SQLException if any DB operation fails.
     */
    @BeforeAll
    static void setUpOnce() throws SQLException {
        cleanUpSampleRows();
        PostGISReader.insertPropertyRecords(SAMPLE);
        LOG.info("Test data (#101, #102, #103) inserted.");
    }

    /**
     * Removes the inserted rows from the DB after all tests have run, leaving
     * the database in a clean state. Without this cleanup step, repeated test
     * runs could accumulate duplicates or cause unexpected failures.
     *
     * @throws SQLException if any DB operation fails.
     */
    @AfterAll
    static void tearDownOnce() throws SQLException {
        cleanUpSampleRows();
        LOG.info("Test data (#101, #102, #103) removed. DB is clean.");
    }

    /**
     * Tests the result of {@link PostGISReader#findTouching(int)} for object #101.
     * Expects it to return exactly one neighbor: object #102.
     */
    @Test
    @Order(1)
    @DisplayName("ST_Touches → 101 ↔ 102")
    void touchesShouldFind102() {
        Set<Integer> ids = toObjectIds(PostGISReader.findTouching(101));
        assertEquals(
                Set.of(102),
                ids,
                "Object #101 must touch exactly #102 (and nothing else)."
        );
    }

    /**
     * Tests the result of {@link PostGISReader#findIntersecting(int)} for object #101.
     * Since adjacency along a shared boundary also counts as intersection,
     * #102 should appear in the results, and #103 should not.
     */
    @Test
    @Order(2)
    @DisplayName("ST_Intersects → 101 ↔ 102 (boundary counts)")
    void intersectsShouldFind102() {
        Set<Integer> ids = toObjectIds(PostGISReader.findIntersecting(101));
        assertEquals(
                Set.of(102),
                ids,
                "Object #101 must intersect exactly #102 (shared boundary)."
        );
    }

    /**
     * Tests {@link PostGISReader#findOverlapping(int)} for object #101.
     * Because #101 and #102 share only a boundary (and do not overlap in
     * the interior), this query should return an empty list.
     */
    @Test
    @Order(3)
    @DisplayName("ST_Overlaps → none for 101")
    void overlapsShouldBeEmpty() {
        var found = PostGISReader.findOverlapping(101);
        assertTrue(
                found.isEmpty(),
                "#101 shares only a boundary with #102, so no overlap; #103 is far away."
        );
    }

    /**
     * Tests {@link PostGISReader#findContained(int)} for object #101.
     * Neither #102 nor #103 is fully inside #101's polygon, so the
     * list of contained objects should be empty.
     */
    @Test
    @Order(4)
    @DisplayName("ST_Contains → none for 101")
    void containsShouldBeEmpty() {
        var found = PostGISReader.findContained(101);
        assertTrue(
                found.isEmpty(),
                "#101 does not fully contain #102 or #103."
        );
    }

    /**
     * Verifies that the shared border between #101 and #102 is reported
     * as a non-empty line-string and that disjoint polygons yield an
     * empty geometry.
     */
    @Test @Order(5)
    void intersectionWorks() {
        String touchLine = PostGISReader.intersection(101, 102);
        assertNotNull(touchLine, "Intersection of touching squares must not be null");
        assertTrue(touchLine.contains("LINESTRING") || touchLine.contains("MULTILINESTRING"),
                "Expected a line-string, got: " + touchLine);

        String empty = PostGISReader.intersection(101, 103);
        assertNotNull(empty, "PostGIS returns 'GEOMETRYCOLLECTION EMPTY', not null");
        assertTrue(empty.contains("EMPTY"),
                "Non-intersecting squares should result in an empty geometry");
    }

    /**
     * Owner 1 owns exactly one parcel (#101), so the union must be a
     * single polygon.  An inexistent owner should return {@code null}.
     */
    @Test @Order(6)
    void unionByOwnerWorks() {
        String merged = PostGISReader.unionByOwner(1);
        assertNotNull(merged, "Union for an existing owner must not be null");
        assertTrue(merged.startsWith("POLYGON") || merged.startsWith("MULTIPOLYGON"),
                "Unexpected WKT for union: " + merged);

        assertNull(PostGISReader.unionByOwner(9999),
                "Union for a non-existing owner should be null");
    }

    /**
     * A convenience method that converts a list of {@link PropertyRecord}
     * into a set of their {@code objectID} values. Useful for concise
     * set comparisons in test assertions.
     */
    private static Set<Integer> toObjectIds(List<PropertyRecord> records) {
        return records.stream()
                .map(PropertyRecord::getObjectID)
                .collect(Collectors.toSet());
    }

    /**
     * Deletes sample rows (objectID in {101,102,103}) from the database table,
     * so tests can run repeatedly without duplicating data. This is called
     * from both {@code @BeforeAll} and {@code @AfterAll}.
     *
     * @throws SQLException if the DELETE operation fails.
     */
    private static void cleanUpSampleRows() throws SQLException {
        String sql = "DELETE FROM public.properties WHERE objectid IN (101, 102, 103)";
        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             var stmt = c.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Verifies {@link PostGISReader#distance(int, int)}:
     * <ul>
     *   <li>Adjacent squares (#101 and #102) share a common edge, so the
     *       shortest distance between them must be exactly {@code 0.0}.</li>
     *   <li>Squares #101 and #103 are centred 10 units apart on both the
     *       X- and Y-axes, so the Euclidean separation is
     *       {@code √((10-1)² + (10-1)²) ≈ 12.73}.  The assertion simply
     *       checks the value falls in the 12–13 m band.</li>
     * </ul>
     */
    @Test @Order(7)
    @DisplayName("ST_Distance → 101↔102 = 0, 101↔103 ≈ 12.7")
    void distanceWorks() {
        Double dTouch = PostGISReader.distance(101, 102);
        assertNotNull(dTouch);
        assertEquals(0.0, dTouch, 1e-6,
                "Touching squares should have zero distance");

        Double dFar = PostGISReader.distance(101, 103);
        assertNotNull(dFar);
        assertTrue(dFar > 12.0 && dFar < 13.0,
                "Expected √162 ≈ 12.73; got " + dFar);
    }

    /**
     * Verifies {@link PostGISReader#withinDistance(int, int, double)}:
     * <ul>
     *   <li>#101 and #102 are contiguous, so they must be reported as
     *       “within 0.1 m”.</li>
     *   <li>#101 and #103 are ~12.7 m apart, well beyond 5 m; the call
     *       should therefore return {@code false}.</li>
     * </ul>
     */
    @Test @Order(8)
    @DisplayName("ST_DWithin → true for 101↔102 within 0.1, false for 101↔103 within 5")
    void dWithinWorks() {
        Boolean near   = PostGISReader.withinDistance(101, 102, 0.1);
        Boolean farOff = PostGISReader.withinDistance(101, 103, 5.0);

        assertNotNull(near);
        assertTrue(near,  "Touching parcels must be within 0.1 m");

        assertNotNull(farOff);
        assertFalse(farOff, "Squares ~12.7 m apart are not within 5 m");
    }

    /**
     * <p><strong>ST_Area test.</strong> Each of our sample parcels is a
     * 1&nbsp;×&nbsp;1 unit square, therefore its planar area must be exactly
     * {@code 1.0} (in the layer’s units).</p>
     *
     * <p>The assertion uses a tolerance of <code>1 × 10<sup>-6</sup></code>
     * to cope with floating-point rounding inside PostGIS.</p>
     */
    @Test @Order(9)
    @DisplayName("ST_Area → all three sample squares equal 1.0")
    void areaWorks() {
        for (int id : List.of(101, 102, 103)) {
            Double a = PostGISReader.area(id);
            assertNotNull(a, "Area should not be null for object " + id);
            assertEquals(1.0, a, 1e-6,
                    "Object " + id + " is a 1×1 square, expected area 1.0");
        }
    }

    /**
     * <p><strong>ST_Centroid test.</strong> The centroids for #101 and #103
     * are analytically:</p>
     *
     * <ul>
     *   <li>#101 – square (0,0)–(1,1) ⇒ centroid (0.5,&nbsp;0.5)</li>
     *   <li>#103 – square (10,10)–(11,11) ⇒ centroid (10.5,&nbsp;10.5)</li>
     * </ul>
     *
     * <p>The WKT returned by PostgreSQL is parsed to extract the numeric
     * coordinates, which are then compared to the expected values to within
     * 1 × 10<sup>-6</sup>.</p>
     */
    @Test @Order(10)
    @DisplayName("ST_Centroid → POINT(0.5 0.5) for #101, POINT(10.5 10.5) for #103")
    void centroidWorks() {
        assertCentroidEquals(101, 0.5,  0.5);
        assertCentroidEquals(103,10.5, 10.5);
    }

    private static void assertCentroidEquals(int objectId,
                                             double expX, double expY) {
        String wkt = PostGISReader.centroid(objectId);
        assertNotNull(wkt, "Centroid WKT must not be null (object " + objectId + ')');
        assertTrue(wkt.startsWith("POINT(") && wkt.endsWith(")"),
                "Unexpected WKT format: " + wkt);

        String[] xy = wkt.substring(6, wkt.length() - 1)  // strip "POINT(" and ")"
                .trim().split("\\s+");
        assertEquals(2, xy.length, "Could not parse coordinates from: " + wkt);

        double x = Double.parseDouble(xy[0]);
        double y = Double.parseDouble(xy[1]);

        assertEquals(expX, x, 1e-6, "X-coordinate mismatch for object " + objectId);
        assertEquals(expY, y, 1e-6, "Y-coordinate mismatch for object " + objectId);
    }
}
