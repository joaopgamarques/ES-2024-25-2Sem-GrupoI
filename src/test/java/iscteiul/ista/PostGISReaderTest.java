package iscteiul.ista;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A JUnit 5 test class demonstrating how to test {@link PostGISReader}
 * with live PostgreSQL/PostGIS environment. Requires:
 * <ul>
 *   <li>An accessible PostgreSQL database with PostGIS enabled,
 *       e.g. "jdbc:postgresql://localhost:5432/postgres?currentSchema=propertiesdb"</li>
 *   <li>A table named "propertiesdb.properties" matching the columns in PostGISReader</li>
 *   <li>A user/password that can insert rows</li>
 * </ul>
 *
 * <p><strong>Disclaimer:</strong> This is an <em>integration test</em> that
 * depends on a live database. If such a DB is not available, it will fail.</p>
 */
public class PostGISReaderTest {

    private static final Logger logger = LoggerFactory.getLogger(PostGISReaderTest.class);

    // Adjust to match your local or test DB
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/postgres?currentSchema=propertiesdb";
    private static final String DB_USER  = "postgres";
    private static final String DB_PASS  = "ES2425GI";

    /**
     * A small utility method to clear the propertiesdb.properties table.
     * Useful for ensuring test isolation.
     *
     * @throws SQLException if there's an issue connecting or executing
     */
    private void truncatePropertiesTable() throws SQLException {
        String sql = "TRUNCATE propertiesdb.properties";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Runs before each test to clear existing rows,
     * ensuring each test starts from an empty table.
     */
    @BeforeEach
    void setUp() {
        try {
            truncatePropertiesTable();
        } catch (SQLException e) {
            logger.error("Error clearing properties table before test", e);
        }
    }

    /**
     * Example test that calls {@link PostGISReader#insertPropertyRecords(List, String, String, String)}
     * with a small list of in-memory {@link PropertyRecord} objects, then verifies the row count
     * in the database matches the inserted records (excluding any skipped due to null geometry).
     */
    @Test
    void testInsertPropertyRecords() {
        // 1) Create sample PropertyRecords
        List<PropertyRecord> sampleRecords = new ArrayList<>();

        // geometry WKT: basic MULTIPOLYGON or POLYGON example
        // For real data, ensure valid WKT matching geometry(MultiPolygon,4326)
        String wkt1 = "MULTIPOLYGON (((0 0,0 1,1 1,1 0,0 0)))";
        String wkt2 = "MULTIPOLYGON (((1 1,1 2,2 2,2 1,1 1)))";
        String wktEmpty = "";  // This one should be skipped (null geometry check)

        sampleRecords.add(new PropertyRecord(100, 101L,  999L,  50.0,  25.0,  wkt1,  500, "ParishTest", "MunicipTest", "IslandA"));
        sampleRecords.add(new PropertyRecord(101, 102L, 1000L, 60.0,  30.0,  wkt2,  501, "ParishTest", "MunicipTest", "IslandB"));
        sampleRecords.add(new PropertyRecord(102, 103L, 1001L, 70.0,  35.0,  wktEmpty, 502, "ParishTest", "MunicipTest", "IslandC"));

        // 2) Insert them
        PostGISReader.insertPropertyRecords(sampleRecords, JDBC_URL, DB_USER, DB_PASS);

        // 3) Verify we have exactly 2 rows in the DB (since one geometry was empty, it gets skipped).
        int rowCount = countRowsInPropertiesTable();
        assertEquals(2, rowCount,
                "We expected 2 inserted rows in propertiesdb.properties (skipped the empty geometry).");
    }

    /**
     * A helper method to count rows in "propertiesdb.properties".
     *
     * @return the number of rows currently in that table
     * @throws RuntimeException if an error occurs
     */
    private int countRowsInPropertiesTable() {
        String sql = "SELECT COUNT(*) FROM propertiesdb.properties";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error counting rows in propertiesdb.properties", e);
        }
    }
}
