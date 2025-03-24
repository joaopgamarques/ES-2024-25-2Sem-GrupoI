package iscteiul.ista;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for parsing WKT geometry strings and checking spatial relationships using JTS.
 * <p>
 * This class focuses on simple checks such as adjacency (touch), intersection, and disjoint
 * on WKT (Well-Known Text) polygons, multipolygons, etc. It relies on the LocationTech JTS
 * library for parsing and geometric operations. All parse errors are logged via SLF4J.
 */
public class GeometryUtils {

    /**
     * SLF4J logger for this class, used for reporting parsing failures and debug info.
     */
    private static final Logger logger = LoggerFactory.getLogger(GeometryUtils.class);

    /**
     * A shared WKTReader to parse WKT strings into JTS {@link Geometry} objects.
     */
    private static final WKTReader WKT_READER = new WKTReader();

    /**
     * Determines if two geometries (in WKT form) "touch" each other. "Touching" means they
     * share a boundary but do not overlap in their interior.
     * <p>
     * Example usage: if two multipolygons share only an edge or a point (but no overlapping area),
     * {@code areAdjacent} returns true.
     *
     * @param wktA the WKT representation of geometry A
     * @param wktB the WKT representation of geometry B
     * @return {@code true} if they share a boundary without overlapping interior points,
     *         {@code false} otherwise (including if either WKT is null or invalid)
     */
    public static boolean areAdjacent(String wktA, String wktB) {
        if (wktA == null || wktB == null) {
            logger.warn("One or both WKT strings are null. Cannot determine adjacency.");
            return false;
        }
        try {
            Geometry geometryA = WKT_READER.read(wktA);
            Geometry geometryB = WKT_READER.read(wktB);
            return geometryA.touches(geometryB);
        } catch (ParseException e) {
            logger.error("Error parsing WKT in areAdjacent: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if two WKT geometries intersect in any way (their boundaries or interiors overlap).
     * <p>
     * In JTS terms, this is equivalent to calling {@code geometryA.intersects(geometryB)}.
     * Any shared point will result in {@code true}, including mere boundary contacts
     * (which also yields {@code true}).
     *
     * @param wktA the WKT representation of geometry A
     * @param wktB the WKT representation of geometry B
     * @return {@code true} if there's any intersection, {@code false} otherwise
     */
    public static boolean doIntersect(String wktA, String wktB) {
        if (wktA == null || wktB == null) {
            logger.warn("One or both WKT strings are null. Cannot determine intersection.");
            return false;
        }
        try {
            Geometry geometryA = WKT_READER.read(wktA);
            Geometry geometryB = WKT_READER.read(wktB);
            return geometryA.intersects(geometryB);
        } catch (ParseException e) {
            logger.error("Error parsing WKT in doIntersect: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determines if two WKT geometries are completely disjoint, meaning they have no
     * boundary or interior points in common.
     *
     * @param wktA the WKT representation of geometry A
     * @param wktB the WKT representation of geometry B
     * @return {@code true} if they share no common points at all; {@code false} otherwise
     */
    public static boolean areDisjoint(String wktA, String wktB) {
        if (wktA == null || wktB == null) {
            logger.warn("One or both WKT strings are null. Cannot determine disjoint status.");
            return false;
        }
        try {
            Geometry geometryA = WKT_READER.read(wktA);
            Geometry geometryB = WKT_READER.read(wktB);
            return geometryA.disjoint(geometryB);
        } catch (ParseException e) {
            logger.error("Error parsing WKT in areDisjoint: {}", e.getMessage(), e);
            return false;
        }
    }
}
