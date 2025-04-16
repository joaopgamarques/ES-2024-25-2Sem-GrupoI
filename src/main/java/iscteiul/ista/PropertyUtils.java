package iscteiul.ista;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
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

    /**
     * Returns a set of distinct parish names among the given PropertyRecord list.
     * Null parish fields are excluded.
     *
     * @param records the list of PropertyRecord objects
     * @return a Set of unique parish strings, or an empty set if records is null or no parishes
     */
    public static Set<String> getDistinctParishes(List<PropertyRecord> records) {
        if (records == null) {
            return Collections.emptySet();
        }
        return records.stream()
                .map(PropertyRecord::getParish)
                .filter(Objects::nonNull)  // skip null
                .collect(Collectors.toSet());
    }

    /**
     * Returns a set of distinct municipality names among the given PropertyRecord list.
     * Null municipality fields are excluded.
     *
     * @param records the list of PropertyRecord objects
     * @return a Set of unique municipality strings, or an empty set if records is null or no municipalities
     */
    public static Set<String> getDistinctMunicipalities(List<PropertyRecord> records) {
        if (records == null) {
            return Collections.emptySet();
        }
        return records.stream()
                .map(PropertyRecord::getMunicipality)
                .filter(Objects::nonNull)  // skip null
                .collect(Collectors.toSet());
    }

    // You can add more utility methods here (e.g., merging contiguous properties,
    // generating swap suggestions, etc.) as needed.

    /**
     * Filters a list of {@link PropertyRecord} objects by the specified parish name.
     *
     * @param records the list of all {@link PropertyRecord} objects
     * @param parish the parish name to filter by; case-sensitive
     * @return a new list of {@link PropertyRecord} objects in the specified parish
     */
    public static List<PropertyRecord> findByParish(List<PropertyRecord> records, String parish) {
        if (records == null || parish == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(r -> parish.equals(r.getParish()))
                .collect(Collectors.toList());
    }


    /**
     * Calculates the average area of the given list of {@link PropertyRecord} objects.
     *
     * @param properties the list of {@link PropertyRecord} objects
     * @return the average area, or 0.0 if the list is null or empty
     */
    public static double calculateAverageArea(List<PropertyRecord> properties) {
        if (properties == null || properties.isEmpty()) return 0.0;

        double soma = properties.stream()
                .mapToDouble(PropertyRecord::getShapeArea)
                .sum();

        return soma / properties.size();
    }

    /**
     * Groups a list of {@link PropertyRecord} objects by their owner ID.
     *
     * @param properties the list of {@link PropertyRecord} objects
     * @return a map where the key is the owner ID and the value is a list of properties owned by that owner
     */
    public static Map<Integer, List<PropertyRecord>> groupPropertiesByOwner(List<PropertyRecord> properties) {
        if (properties == null) return new HashMap<>();

        return properties.stream()
                .collect(Collectors.groupingBy(PropertyRecord::getOwner));
    }

    /**
     * Calculates the average area of connected property groups for each owner.
     *
     * <p>For each owner, builds a subgraph of their properties, identifies connected groups,
     * and computes the total area for each group. Returns the average of these group areas.
     *
     * @param records the list of {@link PropertyRecord} objects
     * @param fullGraph the full graph of properties and their connections
     * @return the average area of connected property groups, or 0.0 if no groups are found
     */
    public static double calculateAverageGroupedArea(List<PropertyRecord> records, org.jgrapht.Graph<PropertyRecord, DefaultEdge> fullGraph) {
        Map<Integer, List<PropertyRecord>> ownerGroups = groupPropertiesByOwner(records);
        List<Double> allGroupedAreas = new ArrayList<>();

        for (Map.Entry<Integer, List<PropertyRecord>> entry : ownerGroups.entrySet()) {
            List<PropertyRecord> ownerProperties = entry.getValue();

            // Subgrafo só com propriedades deste dono
            org.jgrapht.Graph<PropertyRecord, DefaultEdge> subgraph = new SimpleGraph<>(DefaultEdge.class);
            for (PropertyRecord p : ownerProperties) {
                subgraph.addVertex(p);
            }

            for (PropertyRecord p1 : ownerProperties) {
                for (PropertyRecord p2 : ownerProperties) {
                    if (!p1.equals(p2) && fullGraph.containsEdge(p1, p2)) {
                        subgraph.addEdge(p1, p2);
                    }
                }
            }

            ConnectivityInspector<PropertyRecord, DefaultEdge> inspector = new ConnectivityInspector<>(subgraph);
            List<Set<PropertyRecord>> connectedSets = inspector.connectedSets();

            for (Set<PropertyRecord> group : connectedSets) {
                double totalArea = group.stream()
                        .mapToDouble(PropertyRecord::getShapeArea)
                        .sum();
                allGroupedAreas.add(totalArea);
            }
        }

        return allGroupedAreas.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}
