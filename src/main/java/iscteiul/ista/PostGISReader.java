package iscteiul.ista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * A utility class that inserts {@link PropertyRecord} objects into a
 * PostgreSQL/PostGIS database. It assumes you have:
 * <ul>
 *   <li>A database named "postgres" (or another name) with {@code CREATE EXTENSION postgis;}</li>
 *   <li>A schema named "propertiesdb" (or your choice) inside that database</li>
 *   <li>A table named "propertiesdb.properties" with matching columns:
 *     <pre>objectid, parcelid, parcelnumber, shapelength, shapearea,
 * geometry, owner, parish, municipality, island</pre>
 *   </li>
 *   <li>A geometry column declared as {@code geometry(MultiPolygon,4326)} (or your geometry type)</li>
 * </ul>
 */
public final class PostGISReader {

    /**
     * SLF4J logger for reporting successes, warnings, and errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(PostGISReader.class);

    /**
     * Private constructor to prevent instantiation, as this class is
     * intended only for static insertion methods.
     *
     * @throws AssertionError always, since instantiation is disallowed
     */
    private PostGISReader() {
        throw new AssertionError("Utility class - do not instantiate.");
    }

    /**
     * Inserts each {@link PropertyRecord} into a PostGIS table named
     * {@code propertiesdb.properties}. The table must have columns:
     * <pre>
     *   objectid      (INT)
     *   parcelid      (BIGINT)
     *   parcelnumber  (BIGINT)
     *   shapelength   (DOUBLE PRECISION)
     *   shapearea     (DOUBLE PRECISION)
     *   geometry      (geometry(MultiPolygon,4326)) or your geometry type
     *   owner         (INT)
     *   parish        (TEXT)
     *   municipality  (TEXT)
     *   island        (TEXT)
     * </pre>
     *
     * <p>The geometry is inserted by calling
     * {@code public.ST_GeomFromText(?::text, 4326)}, thus casting the WKT
     * string to {@code text} and specifying SRID 4326.</p>
     *
     * <p>Ensure you run
     * {@code CREATE EXTENSION IF NOT EXISTS postgis;} in your database
     * beforehand.</p>
     *
     * @param records a List of {@link PropertyRecord} objects, presumably parsed from CSV
     * @param jdbcUrl a JDBC connection string, e.g.
     *                {@code "jdbc:postgresql://localhost:5432/postgres?currentSchema=propertiesdb"}
     *                so you're in the {@code postgres} database and
     *                the search path is set to {@code propertiesdb}.
     * @param dbUser  the PostgreSQL user, typically "postgres" or another DB user
     * @param dbPass  the password for {@code dbUser}
     */
    public static void insertPropertyRecords(
            List<PropertyRecord> records,
            String jdbcUrl,
            String dbUser,
            String dbPass
    ) {

        // SQL statement referencing the fully qualified function: public.ST_GeomFromText
        // and casting ? to text => ?::text
        // so the table name is "propertiesdb.properties"
        // geometry is derived by ST_GeomFromText(?::text, 4326)
        final String insertSql =
                "INSERT INTO propertiesdb.properties (" +
                        " objectid, parcelid, parcelnumber, shapelength, shapearea," +
                        " geometry, owner, parish, municipality, island" +
                        ") VALUES (" +
                        " ?, ?, ?, ?, ?," +
                        " public.ST_GeomFromText(?::text, 4326), ?, ?, ?, ?" +
                        ")";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            int insertedCount = 0;

            for (PropertyRecord rec : records) {
                String wkt = rec.getGeometry();

                // If geometry is missing or invalid, you can skip or handle differently
                if (wkt == null || wkt.trim().isEmpty()) {
                    logger.warn("Skipping objectID={} due to null/empty geometry", rec.getObjectID());
                    continue;
                }

                // Prepare the parameters for each column in order:
                pstmt.setInt(1, rec.getObjectID());
                pstmt.setLong(2, rec.getParcelID());
                pstmt.setLong(3, rec.getParcelNumber());
                pstmt.setDouble(4, rec.getShapeLength());
                pstmt.setDouble(5, rec.getShapeArea());
                // geometry: pass WKT as text => ?::text
                pstmt.setString(6, wkt);

                pstmt.setInt(7, rec.getOwner());
                pstmt.setString(8, rec.getParish());
                pstmt.setString(9, rec.getMunicipality());
                pstmt.setString(10, rec.getIsland());

                pstmt.executeUpdate();
                insertedCount++;
            }

            logger.info("Successfully inserted {} PropertyRecord(s) into PostGIS!", insertedCount);

        } catch (SQLException e) {
            logger.error("Database insertion error", e);
        }
    }
}
