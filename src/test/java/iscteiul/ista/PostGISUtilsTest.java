package iscteiul.ista;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PostGISUtilsTest &mdash; Integration Tests for {@link PostGISUtils}</h2>
 *
 * <p>This test class demonstrates end-to-end verification of the
 * {@link PostGISUtils} functionality in a real PostgreSQL/PostGIS environment.
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
 *       update the constants in this file (and in {@link PostGISUtils}) to match
 *       your DB schema or credentials.</li>
 * </ul>
 *
 * @author
 *   Integration-Test Team (2025) &mdash; Update to your actual team name/author
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostGISUtilsTest {

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
    private static final Logger LOG = LoggerFactory.getLogger(PostGISUtilsTest.class);

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
        PostGISUtils.insertPropertyRecords(SAMPLE);
        LOG.info("Test data (#101, #102, #103) inserted.");
    }

    /**
     * Truncates the <strong>public.properties</strong> table to remove all test data,
     * then re-imports the original CSV file so that the table is restored to a known
     * initial state for subsequent usage.
     *
     * @throws SQLException if any database error occurs while truncating or re-inserting
     *                      the data
     */
    @AfterAll
    static void tearDownOnce() throws SQLException {
        // 1) Open a connection and truncate the table
        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = c.createStatement()) {

            stmt.execute("TRUNCATE TABLE public.properties RESTART IDENTITY CASCADE");
            LOG.info("All data truncated from 'public.properties'. Table structure retained.");

        } catch (SQLException e) {
            LOG.error("Error truncating table after tests: ", e);
        }

        // 2) Re-import data from CSV into the now-empty table
        List<PropertyRecord> rows = new CSVFileReader().importData("/Madeira-Moodle-1.1.csv");
        LOG.info("CSV rows read: {}", rows.size());

        PostGISUtils.insertPropertyRecords(rows);
        LOG.info("Re-imported CSV data into 'public.properties' after test suite completion.");
    }

    /**
     * Tests the result of {@link PostGISUtils#findTouching(int)} for object #101.
     * Expects it to return exactly one neighbor: object #102.
     */
    @Test
    @Order(1)
    @DisplayName("ST_Touches → 101 ↔ 102")
    void touchesShouldFind102() {
        Set<Integer> ids = toObjectIds(PostGISUtils.findTouching(101));
        assertEquals(
                Set.of(102),
                ids,
                "Object #101 must touch exactly #102 (and nothing else)."
        );
    }

    /**
     * Tests the result of {@link PostGISUtils#findIntersecting(int)} for object #101.
     * Since adjacency along a shared boundary also counts as intersection,
     * #102 should appear in the results, and #103 should not.
     */
    @Test
    @Order(2)
    @DisplayName("ST_Intersects → 101 ↔ 102 (boundary counts)")
    void intersectsShouldFind102() {
        Set<Integer> ids = toObjectIds(PostGISUtils.findIntersecting(101));
        assertEquals(
                Set.of(102),
                ids,
                "Object #101 must intersect exactly #102 (shared boundary)."
        );
    }

    /**
     * Tests {@link PostGISUtils#findOverlapping(int)} for object #101.
     * Because #101 and #102 share only a boundary (and do not overlap in
     * the interior), this query should return an empty list.
     */
    @Test
    @Order(3)
    @DisplayName("ST_Overlaps → none for 101")
    void overlapsShouldBeEmpty() {
        var found = PostGISUtils.findOverlapping(101);
        assertTrue(
                found.isEmpty(),
                "#101 shares only a boundary with #102, so no overlap; #103 is far away."
        );
    }

    /**
     * Tests {@link PostGISUtils#findContained(int)} for object #101.
     * Neither #102 nor #103 is fully inside #101's polygon, so the
     * list of contained objects should be empty.
     */
    @Test
    @Order(4)
    @DisplayName("ST_Contains → none for 101")
    void containsShouldBeEmpty() {
        var found = PostGISUtils.findContained(101);
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
        String touchLine = PostGISUtils.intersection(101, 102);
        assertNotNull(touchLine, "Intersection of touching squares must not be null");
        assertTrue(touchLine.contains("LINESTRING") || touchLine.contains("MULTILINESTRING"),
                "Expected a line-string, got: " + touchLine);

        String empty = PostGISUtils.intersection(101, 103);
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
        String merged = PostGISUtils.unionByOwner(1);
        assertNotNull(merged, "Union for an existing owner must not be null");
        assertTrue(merged.startsWith("POLYGON") || merged.startsWith("MULTIPOLYGON"),
                "Unexpected WKT for union: " + merged);

        assertNull(PostGISUtils.unionByOwner(9999),
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
     * Verifies {@link PostGISUtils#distance(int, int)}:
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
        Double dTouch = PostGISUtils.distance(101, 102);
        assertNotNull(dTouch);
        assertEquals(0.0, dTouch, 1e-6,
                "Touching squares should have zero distance");

        Double dFar = PostGISUtils.distance(101, 103);
        assertNotNull(dFar);
        assertTrue(dFar > 12.0 && dFar < 13.0,
                "Expected √162 ≈ 12.73; got " + dFar);
    }

    /**
     * Verifies {@link PostGISUtils#withinDistance(int, int, double)}:
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
        Boolean near   = PostGISUtils.withinDistance(101, 102, 0.1);
        Boolean farOff = PostGISUtils.withinDistance(101, 103, 5.0);

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
            Double a = PostGISUtils.area(id);
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
        String wkt = PostGISUtils.centroid(objectId);
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

    /**
     * <p><strong>Scenario:</strong> Both the source property (e.g., #101) and the
     * "Funchal (Sé)" property (#11074) exist with valid geometries.</p>
     *
     * <p><strong>Expected:</strong> {@code distanceToFunchal(101)} returns a non-null
     * number (in degrees if EPSG:3763, or meters if projected). Ensures successful lookup
     * of both properties and a positive distance. We optionally remove #11074 afterward
     * to keep the DB in a known state for subsequent tests.</p>
     */
    @Test
    @Order(11)
    @DisplayName("distanceToFunchal → valid scenario (source + #11074 present)")
    void testDistanceToFunchal_validCase() throws SQLException {
        // Insert or update #11074 with a known geometry near (2,2)-(3,3)
        insertOrUpdateProperty(11074, "POLYGON((2 2,3 2,3 3,2 3,2 2))");

        // #101 is already in the DB as a 1x1 square at (0..1, 0..1)
        // from the SAMPLE setUp. We'll measure distance from #101 to #11074
        Double dist = PostGISUtils.distanceToFunchal(101);

        assertNotNull(dist,
                "distanceToFunchal(...) should not be null when #11074 and #101 exist with valid geometry.");
        assertTrue(dist > 0.0,
                "Expected a positive distance between #101 and #11074 (Funchal).");

        // Cleanup: remove #11074 to avoid interfering with other tests, if desired
        deleteProperty(11074);
    }

    /**
     * <p><strong>Scenario:</strong> #11074 (the Funchal parcel) is missing from
     * the table, but the source property (#101) still exists.</p>
     *
     * <p><strong>Expected:</strong> {@code distanceToFunchal(101)} yields {@code null}
     * because the method cannot find #11074 in the DB.</p>
     */
    @Test
    @Order(12)
    @DisplayName("distanceToFunchal → null when #11074 is missing")
    void testDistanceToFunchal_missingFunchal() throws SQLException {
        // Ensure #11074 is not present in the table
        deleteProperty(11074);

        // #101 still exists from SAMPLE
        Double dist = PostGISUtils.distanceToFunchal(101);
        assertNull(dist,
                "Expected null distance if #11074 is not in the DB.");
    }

    /**
     * <p><strong>Scenario:</strong> #11074 (Funchal) is present, but the requested
     * source property does not exist (e.g., #9999).</p>
     *
     * <p><strong>Expected:</strong> {@code distanceToFunchal(9999)} yields {@code null}
     * since the source property is missing from the DB.</p>
     */
    @Test
    @Order(13)
    @DisplayName("distanceToFunchal → null when source property is missing")
    void testDistanceToFunchal_missingSource() throws SQLException {
        // Insert #11074 so it's present
        insertOrUpdateProperty(11074, "POLYGON((5 5,6 5,6 6,5 6,5 5))");

        // Remove #9999 if it exists, ensuring no row with that ID
        deleteProperty(9999);

        // Attempt to measure distance from #9999 to #11074
        Double dist = PostGISUtils.distanceToFunchal(9999);
        assertNull(dist,
                "Expected null distance if the source property #9999 is missing in DB.");

        // Cleanup
        deleteProperty(11074);
    }

    /**
     * Inserts or updates a row in {@code public.properties} with the given objectID and WKT geometry.
     * If a row with this objectID exists, updates its geometry; otherwise inserts a new row.
     *
     * @param objectId  the parcel’s unique objectID in the DB
     * @param wktPolygon a valid WKT string defining the polygon geometry
     * @throws SQLException if any database I/O error occurs
     */
    private void insertOrUpdateProperty(int objectId, String wktPolygon) throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            // Check if that objectId exists
            String checkSql = "SELECT objectid FROM public.properties WHERE objectid = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, objectId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    boolean exists = rs.next();
                    if (!exists) {
                        // Insert a new row
                        String insertSql =
                                "INSERT INTO public.properties(objectid, geometry) "
                                        + "VALUES (?, ST_GeomFromText(?::text,3763))";
                        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                            ins.setInt(1, objectId);
                            ins.setString(2, wktPolygon);
                            ins.executeUpdate();
                        }
                    } else {
                        // Update existing geometry
                        String updateSql =
                                "UPDATE public.properties "
                                        + "SET geometry=ST_GeomFromText(?::text,3763) "
                                        + "WHERE objectid=?";
                        try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                            upd.setString(1, wktPolygon);
                            upd.setInt(2, objectId);
                            upd.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes a row from {@code public.properties} if it exists, identified by {@code objectId}.
     * If no such row exists, this method silently does nothing.
     *
     * @param objectId the parcel’s unique objectID in the DB
     * @throws SQLException if any database I/O error occurs
     */
    private void deleteProperty(int objectId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            String deleteSql = "DELETE FROM public.properties WHERE objectid=?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setInt(1, objectId);
                ps.executeUpdate();
            }
        }
    }
}
