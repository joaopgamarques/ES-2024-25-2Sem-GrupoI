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
}
