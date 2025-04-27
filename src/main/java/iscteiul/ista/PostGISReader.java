package iscteiul.ista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * <h2>PostGISReader &ndash; Bulk Loader and Neighbor Finder</h2>
 *
 * <p>This <strong>static-only</strong> helper class provides functionality to:</p>
 * <ol>
 *   <li><strong>Insert</strong> a list of {@link PropertyRecord} objects into a PostGIS table
 *       (<em>see {@link #TABLE_SCHEMA}</em> and {@link #TABLE_NAME} for details).</li>
 *   <li><strong>Query</strong> that table for neighboring parcels using
 *       standard PostGIS spatial predicates:
 *       <ul>
 *         <li>{@code ST_Touches}</li>
 *         <li>{@code ST_Intersects}</li>
 *         <li>{@code ST_Overlaps}</li>
 *         <li>{@code ST_Contains}</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>Pre-requisites</h3>
 * <ol>
 *   <li><strong>PostGIS Extension</strong>:
 *       {@code CREATE EXTENSION IF NOT EXISTS postgis;}</li>
 *   <li><strong>Table Definition</strong>: The table
 *       {@value #TABLE_NAME} (adjust schema if not using {@code public}):
 *       <pre>{@code
 *       CREATE TABLE IF NOT EXISTS public.properties (
 *           objectid      INT PRIMARY KEY,
 *           parcelid      BIGINT,
 *           parcelnumber  BIGINT,
 *           shapelength   DOUBLE PRECISION,
 *           shapearea     DOUBLE PRECISION,
 *           geometry      geometry(MultiPolygon,4326),
 *           owner         INT,
 *           parish        TEXT,
 *           municipality  TEXT,
 *           island        TEXT
 *       );
 *       }</pre>
 *   </li>
 * </ol>
 *
 * <p>Adjust the constants {@link #TABLE_SCHEMA}, {@link #TABLE_NAME},
 * and the connection details if your database settings differ.</p>
 *
 * <p><strong>Author:</strong> Your Name (2025)</p>
 */
public final class PostGISReader {

    /** The schema containing the "properties" table. Adjust as needed. */
    private static final String TABLE_SCHEMA = "public";

    /** Fully-qualified table name "<schema>.properties". */
    private static final String TABLE_NAME = TABLE_SCHEMA + ".properties";

    /**
     * A JDBC URL pointing to the <em>postgres</em> database with the given
     * schema set as the <em>search_path</em>. Update host/port/db name if needed.
     */
    private static final String JDBC_URL =
            "jdbc:postgresql://localhost:5432/postgres?currentSchema=" + TABLE_SCHEMA;

    /**
     * Postgres user and password, which must have the privileges necessary
     * to create and query geometry columns in the specified schema.
     */
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "ES2425GI";

    /** SLF4J Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostGISReader.class);

    /**
     * Private constructor prevents instantiation. This is a static utility class.
     *
     * @throws AssertionError always (if called by reflection or otherwise)
     */
    private PostGISReader() {
        throw new AssertionError("Utility class - do not instantiate.");
    }

    /**
     * Inserts each {@link PropertyRecord} into the PostGIS table {@value #TABLE_NAME}.
     * The geometry column is populated by invoking {@code ST_GeomFromText(?::text, 4326)}.
     *
     * <p>Records that lack a non-empty {@link PropertyRecord#getGeometry() geometry} field
     * are skipped with a warning. Adjust logic if you need to handle those cases differently.</p>
     *
     * @param records a list of {@link PropertyRecord} objects to be inserted
     *                (each must have valid WKT geometry in {@link PropertyRecord#getGeometry()})
     */
    public static void insertPropertyRecords(List<PropertyRecord> records) {
        final String sql =
                "INSERT INTO " + TABLE_NAME + " (" +
                        "    objectid, parcelid, parcelnumber, shapelength, shapearea," +
                        "    geometry, owner, parish, municipality, island" +
                        ") VALUES (" +
                        "    ?, ?, ?, ?, ?," +
                        "    ST_GeomFromText(?::text,4326), ?, ?, ?, ?" +
                        ")";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int inserted = 0;
            for (PropertyRecord record : records) {
                // If geometry is missing or blank, skip the row.
                String wkt = record.getGeometry();
                if (wkt == null || wkt.isBlank()) {
                    LOGGER.warn("Skipping objectID={} due to null/empty geometry",
                            record.getObjectID());
                    continue;
                }

                // Fill the placeholders in the PreparedStatement.
                ps.setInt(1,    record.getObjectID());
                ps.setLong(2,   record.getParcelID());
                ps.setLong(3,   record.getParcelNumber());
                ps.setDouble(4, record.getShapeLength());
                ps.setDouble(5, record.getShapeArea());
                ps.setString(6, wkt); // geometry => ST_GeomFromText(?::text,4326)
                ps.setInt(7,    record.getOwner());
                ps.setString(8, record.getParish());
                ps.setString(9, record.getMunicipality());
                ps.setString(10,record.getIsland());

                ps.addBatch();
                inserted++;
            }

            // Execute all batched inserts at once.
            ps.executeBatch();
            LOGGER.info("Inserted {} rows into {}", inserted, TABLE_NAME);

        } catch (SQLException e) {
            LOGGER.error("Bulk insert into {} failed.", TABLE_NAME, e);
        }
    }

    /**
     * Merges (UNION) every parcel that belongs to a given owner into a single
     * MultiPolygon and returns it as WKT text.
     *
     * @param ownerId  the owner column value
     * @return         WKT of the merged geometry, or {@code null} if the owner
     *                 has no rows (or on error)
     */
    public static String unionByOwner(int ownerId) {
        final String sql =
                "SELECT ST_AsText(ST_Union(geometry)) AS merged " +
                        "  FROM " + TABLE_NAME +
                        " WHERE owner = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return (rs.next() ? rs.getString("merged") : null);
            }
        } catch (SQLException e) {
            LOGGER.error("ST_Union failed for owner={}", ownerId, e);
            return null;
        }
    }

    /**
     * Returns the geometric intersection of two parcels as WKT.
     *
     * @param objectIdA  first parcel’s objectID
     * @param objectIdB  second parcel’s objectID
     * @return           WKT of the intersection polygon, or {@code null} if the
     *                   geometries do not intersect (or on error)
     */
    public static String intersection(int objectIdA, int objectIdB) {
        final String sql =
                "SELECT ST_AsText(ST_Intersection(a.geometry, b.geometry)) AS inter " +
                        "  FROM " + TABLE_NAME + " a, " + TABLE_NAME + " b " +
                        " WHERE a.objectid = ? AND b.objectid = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, objectIdA);
            ps.setInt(2, objectIdB);
            try (ResultSet rs = ps.executeQuery()) {
                return (rs.next() ? rs.getString("inter") : null);
            }
        } catch (SQLException e) {
            LOGGER.error("ST_Intersection failed ({} vs {})", objectIdA, objectIdB, e);
            return null;
        }
    }





    /**
     * Returns the shortest planar distance between two parcel geometries
     * (in the unit of the layer’s SRID – for EPSG : 4326 that is metres
     * when your data are in a projected CRS, otherwise degrees).
     *
     * @param objectIdA first parcel’s {@code objectid}
     * @param objectIdB second parcel’s {@code objectid}
     * @return distance as {@code double}, or {@code null} if one id is missing
     *         or a DB error occurs
     */
    public static Double distance(int objectIdA, int objectIdB) {
        final String sql =
                "SELECT ST_Distance(a.geometry, b.geometry) AS dist " +
                        "  FROM " + TABLE_NAME + " a, " + TABLE_NAME + " b " +
                        " WHERE a.objectid = ? AND b.objectid = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, objectIdA);
            ps.setInt(2, objectIdB);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("dist") : null;
            }
        } catch (SQLException e) {
            LOGGER.error("ST_Distance failed ({} ↔ {})", objectIdA, objectIdB, e);
            return null;
        }
    }

    /**
     * Convenience wrapper around {@code ST_DWithin}.  It answers the question
     * “are the two parcels closer than the given distance?”.
     *
     * @param objectIdA  first parcel
     * @param objectIdB  second parcel
     * @param distance   threshold distance in the unit of the SRID
     * @return           {@code true} / {@code false}, or {@code null} on error
     */
    public static Boolean withinDistance(int objectIdA, int objectIdB, double distance) {
        final String sql =
                "SELECT ST_DWithin(a.geometry, b.geometry, ?) AS ok " +
                        "  FROM " + TABLE_NAME + " a, " + TABLE_NAME + " b " +
                        " WHERE a.objectid = ? AND b.objectid = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDouble(1, distance);
            ps.setInt(2, objectIdA);
            ps.setInt(3, objectIdB);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean("ok") : null;
            }
        } catch (SQLException e) {
            LOGGER.error("ST_DWithin failed ({} ↔ {}, d={})",
                    objectIdA, objectIdB, distance, e);
            return null;
        }
    }

    /**
     * Calculates the planar area of a parcel, using
     * {@code ST_Area(geometry)} in the database.
     *
     * @param objectId the parcel’s {@code objectid}
     * @return         the area in square units of the layer’s SRID,
     *                 or {@code null} if the row is missing / DB error
     */
    public static Double area(int objectId) {
        final String sql =
                "SELECT ST_Area(geometry) AS a " +
                        "  FROM " + TABLE_NAME +
                        " WHERE objectid = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, objectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("a") : null;
            }
        } catch (SQLException e) {
            LOGGER.error("ST_Area failed (objectID={})", objectId, e);
            return null;
        }
    }

    /**
     * Computes the centroid of a parcel geometry and returns
     * the point as WKT text (e.g.&nbsp;{@code POINT(x y)}).
     *
     * @param objectId the parcel’s {@code objectid}
     * @return         WKT of the centroid point, or {@code null} if not found / error
     */
    public static String centroid(int objectId) {
        final String sql =
                "SELECT ST_AsText(ST_Centroid(geometry)) AS c " +
                        "  FROM " + TABLE_NAME +
                        " WHERE objectid = ?";

        try (Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, objectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("c") : null;
            }
        } catch (SQLException e) {
            LOGGER.error("ST_Centroid failed (objectID={})", objectId, e);
            return null;
        }
    }

    /**
     * Finds all parcels in the database that <em>touch</em> (share a boundary
     * but do not overlap) the parcel whose {@code objectID} is given.
     *
     * @param objectId the reference parcel's ID
     * @return a list of {@link PropertyRecord} that touch the reference parcel
     */
    public static List<PropertyRecord> findTouching(int objectId) {
        return queryNeighbours(objectId, "ST_Touches");
    }

    /**
     * Finds all parcels that <em>intersect</em> (share any point) the parcel
     * whose {@code objectID} is given.
     *
     * @param objectId the reference parcel's ID
     * @return a list of {@link PropertyRecord} that intersect the reference parcel
     */
    public static List<PropertyRecord> findIntersecting(int objectId) {
        return queryNeighbours(objectId, "ST_Intersects");
    }

    /**
     * Finds all parcels that <em>overlap</em> the parcel whose {@code objectID}
     * is given. Overlapping implies partial interior intersection but not full
     * containment.
     *
     * @param objectId the reference parcel's ID
     * @return a list of {@link PropertyRecord} that overlap the reference parcel
     */
    public static List<PropertyRecord> findOverlapping(int objectId) {
        return queryNeighbours(objectId, "ST_Overlaps");
    }

    /**
     * Finds all parcels that are <em>contained within</em> the geometry of
     * the parcel whose {@code objectID} is given. This uses {@code ST_Contains(A,B)},
     * meaning A (the reference) fully encloses B.
     *
     * @param objectId the reference parcel's ID
     * @return a list of {@link PropertyRecord} that lie inside the reference parcel
     */
    public static List<PropertyRecord> findContained(int objectId) {
        return queryNeighbours(objectId, "ST_Contains");
    }

    /**
     * A generic method to find the "neighboring" parcels (rows) in the database
     * that satisfy {@code postgisFn(a.geometry, b.geometry)=TRUE} for a given
     * reference {@code objectId}. The table is self-joined as 'a' and 'b'.
     *
     * @param objectId  the reference parcel's ID
     * @param postgisFn the name of the spatial function (e.g., "ST_Touches")
     * @return a list of matching neighbors from the DB
     */
    private static List<PropertyRecord> queryNeighbours(int objectId, String postgisFn) {
        final String sql =
                "SELECT b.objectid, b.parcelid, b.parcelnumber, b.shapelength, b.shapearea," +
                        "       ST_AsText(b.geometry) AS geometry," +
                        "       b.owner, b.parish, b.municipality, b.island " +
                        "  FROM " + TABLE_NAME + " a " +
                        "  JOIN " + TABLE_NAME + " b " +
                        "    ON " + postgisFn + "(a.geometry, b.geometry)" +
                        "   AND a.objectid <> b.objectid " +
                        " WHERE a.objectid = ?";

        List<PropertyRecord> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, objectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowToPropertyRecord(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Neighbour query failed (function={}, objectID={})",
                    postgisFn, objectId, e);
        }
        return result;
    }

    /**
     * Maps the columns of a {@link ResultSet} row into a {@link PropertyRecord}.
     *
     * @param rs the ResultSet positioned at a valid row
     * @return a new {@link PropertyRecord} constructed from row data
     * @throws SQLException if any column is missing or invalid
     */
    private static PropertyRecord mapRowToPropertyRecord(ResultSet rs) throws SQLException {
        return new PropertyRecord(
                rs.getInt   ("objectid"),
                rs.getLong  ("parcelid"),
                rs.getLong  ("parcelnumber"),
                rs.getDouble("shapelength"),
                rs.getDouble("shapearea"),
                rs.getString("geometry"),
                rs.getInt   ("owner"),
                rs.getString("parish"),
                rs.getString("municipality"),
                rs.getString("island")
        );
    }

    /**
     * Minimal command-line demonstration:
     * <ol>
     *   <li>Reads the CSV (via {@link CSVFileReader}) into memory.</li>
     *   <li>(Optional) calls {@link #insertPropertyRecords(List)} exactly once
     *       to populate the database table.</li>
     *   <li>Prompts for an <code>objectID</code> and prints neighbors for four
     *       standard predicates:
     *       {@code ST_Touches}, {@code ST_Intersects}, {@code ST_Overlaps}, {@code ST_Contains}.</li>
     * </ol>
     *
     * <p>Adapt or remove if you have a different application entry point.</p>
     *
     * @param args ignored
     */
    public static void main(String[] args) {

        // 1) Read CSV
        List<PropertyRecord> rows = new CSVFileReader()
                .importData("/Madeira-Moodle-1.1.csv");
        LOGGER.info("CSV rows read: {}", rows.size());

        // 2) Optionally, uncomment once to populate the table
        // insertPropertyRecords(rows);

        // 3) Interactive neighbor queries
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nObjectID to inspect (0 = quit): ");
                int id = sc.nextInt();
                if (id == 0) {
                    break;
                }
                printResults("TOUCHING",      findTouching(id));
                printResults("INTERSECTING",  findIntersecting(id));
                printResults("OVERLAPPING",   findOverlapping(id));
                printResults("CONTAINED",     findContained(id));

                System.out.print("Another objectID to intersect with " + id + " (0=skip): ");
                int other = sc.nextInt();
                if (other != 0) {
                    String inter = intersection(id, other);
                    System.out.println("Intersection WKT: " + (inter == null ? "(none)" : inter));
                }

                System.out.print("Owner id to UNION (0=skip): ");
                int owner = sc.nextInt();
                if (owner != 0) {
                    String merged = unionByOwner(owner);
                    System.out.println("Union for owner " + owner + ": " +
                            (merged == null ? "(no rows / error)" : merged));
                }

                System.out.print("Second objectID to measure distance to " + id + " (0=skip): ");
                int distId = sc.nextInt();
                if (distId != 0) {
                    Double dist = distance(id, distId);
                    System.out.println("Distance = " + dist);

                    System.out.print("Threshold d for ST_DWithin (metres): ");
                    double d = sc.nextDouble();
                    Boolean near = withinDistance(id, distId, d);
                    System.out.println("Within " + d + " m? " + near);
                }

                System.out.print("Show area & centroid for " + id + " (y/N)? ");
                if (sc.next().equalsIgnoreCase("y")) {
                    Double a = area(id);
                    String c = centroid(id);
                    System.out.println("Area      = " + a);
                    System.out.println("Centroid  = " + c);
                }
            }
        }
    }

    /**
     * Prints a list of {@link PropertyRecord} object IDs to the console,
     * preceded by a user-supplied label.
     *
     * @param label a short descriptive label (e.g. "TOUCHING")
     * @param list  a list of matching parcels
     */
    private static void printResults(String label, List<PropertyRecord> list) {
        System.out.println('\n' + label + " parcels: " +
                (list.isEmpty() ? "(none)" : list.size()));
        list.forEach(p -> System.out.println("  — objectID=" + p.getObjectID()));
    }
}
