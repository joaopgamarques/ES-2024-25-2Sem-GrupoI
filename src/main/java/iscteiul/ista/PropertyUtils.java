package iscteiul.ista;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A stateless utility class providing helper methods to work with PropertyRecord objects.
 * <p>
 * Typical usage: filtering properties by owner, by municipality, checking adjacency among them,
 * etc.
 * <p>
 * All methods are static (no internal state).
 */
public final class PropertyUtils {

    /**
     * Private constructor to prevent instantiation,
     * as this is intended to be a static utility class.
     */
    private PropertyUtils() {
        // no-op
    }

    /**
     * Returns a list of PropertyRecord objects owned by the specified owner.
     *
     * @param records the list of all PropertyRecord objects
     * @param ownerId the owner ID to filter by
     * @return a new List of PropertyRecord matching the given ownerId
     */
    public static List<PropertyRecord> findByOwner(List<PropertyRecord> records, int ownerId) {
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(r -> r.getOwner() == ownerId)
                .collect(Collectors.toList());
    }

    /**
     * Filters PropertyRecord objects by a given municipality name (case-sensitive).
     *
     * @param records      the list of all PropertyRecord objects
     * @param municipality the municipality to match; if null, returns empty list
     * @return a new List of PropertyRecord in that municipality
     */
    public static List<PropertyRecord> findByMunicipality(List<PropertyRecord> records, String municipality) {
        if (records == null || municipality == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(r -> municipality.equals(r.getMunicipality()))
                .collect(Collectors.toList());
    }

    /**
     * Filters PropertyRecord objects by the given island (case-sensitive).
     *
     * @param records the list of all PropertyRecord objects
     * @param island  the island to match; if null, returns empty list
     * @return a new List of PropertyRecord on that island
     */
    public static List<PropertyRecord> findByIsland(List<PropertyRecord> records, String island) {
        if (records == null || island == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(r -> island.equals(r.getIsland()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if two PropertyRecords are adjacent (touching) by delegating to GeometryUtils.
     * Returns false if either geometry is missing/null.
     *
     * @param a the first PropertyRecord
     * @param b the second PropertyRecord
     * @return true if they are adjacent (touch), false otherwise
     */
    public static boolean arePropertiesAdjacent(PropertyRecord a, PropertyRecord b) {
        if (a == null || b == null) {
            return false;
        }
        String wktA = a.getGeometry();
        String wktB = b.getGeometry();
        return GeometryUtils.areAdjacent(wktA, wktB);
    }

    /**
     * Finds all properties in the list that are adjacent to a given PropertyRecord (excluding itself).
     *
     * @param record  the reference PropertyRecord
     * @param records the list of all PropertyRecord objects to check
     * @return a List of PropertyRecords that touch the given record
     */
    public static List<PropertyRecord> findAdjacentProperties(
            PropertyRecord record, List<PropertyRecord> records) {

        List<PropertyRecord> result = new ArrayList<>();
        if (record == null || records == null) {
            return result;
        }
        for (PropertyRecord r : records) {
            if (r == record) {
                continue;  // Skip the same instance
            }
            if (arePropertiesAdjacent(record, r)) {
                result.add(r);
            }
        }
        return result;
    }

    // You can add more utility methods here (e.g., merging contiguous properties,
    // generating swap suggestions, etc.) as needed.
}
