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
     * <p>
     * Since all methods in this class are static and no instance fields
     * exist, there is no purpose in creating an object of this class.
     * Consequently, calling this constructor always results in an
     * {@link AssertionError}.
     *
     * @throws AssertionError always, since this constructor should never be called
     */
    private PropertyUtils() {
        throw new AssertionError("Utility class - do not instantiate.");
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

        double totalArea = properties.stream()
                .mapToDouble(PropertyRecord::getShapeArea)
                .sum();

        return totalArea / properties.size();
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
        // If the full graph is null, we cannot proceed.
        if (fullGraph == null) {
            return 0.0;
        }

        // 1) Group properties by owner.
        Map<Integer, List<PropertyRecord>> ownerGroups = groupPropertiesByOwner(records);

        // We'll store the total area of each connected component (for all owners) in this list.
        List<Double> allGroupedAreas = new ArrayList<>();

        // 2) For each owner, create a subgraph using only that owner's properties.
        for (Map.Entry<Integer, List<PropertyRecord>> entry : ownerGroups.entrySet()) {
            List<PropertyRecord> ownerProperties = entry.getValue();

            // Create a new empty subgraph for this owner's properties.
            org.jgrapht.Graph<PropertyRecord, DefaultEdge> subgraph = new SimpleGraph<>(DefaultEdge.class);

            // Add each property as a vertex.
            for (PropertyRecord p : ownerProperties) {
                subgraph.addVertex(p);
            }

            // Check which pairs of these properties are connected in the full graph,
            // and add the corresponding edges to the subgraph.
            for (PropertyRecord p1 : ownerProperties) {
                for (PropertyRecord p2 : ownerProperties) {
                    if (!p1.equals(p2) && fullGraph.containsEdge(p1, p2)) {
                        // Add an undirected edge (DefaultEdge) to the subgraph.
                        subgraph.addEdge(p1, p2);
                    }
                }
            }

            // 3) Use JGraphT's ConnectivityInspector to find connected sets (components).
            ConnectivityInspector<PropertyRecord, DefaultEdge> inspector = new ConnectivityInspector<>(subgraph);
            List<Set<PropertyRecord>> connectedSets = inspector.connectedSets();

            // 4) For each connected component, sum the shape areas of the properties in that component.
            for (Set<PropertyRecord> group : connectedSets) {
                double totalArea = group.stream()
                        .mapToDouble(PropertyRecord::getShapeArea)
                        .sum();
                // Store this group's total area in a list so we can average them later.
                allGroupedAreas.add(totalArea);
            }
        }

        // 5) Compute the average of all connected-group area sums, or 0.0 if the list is empty.
        return allGroupedAreas.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Computes the distance between the centroids of the given property (by its objectID)
     * and the Funchal (Sé) reference property with {@code objectID=11074}, using
     * the WKT geometries in memory. Both properties must be present in the provided list.
     *
     * <p><strong>Important:</strong> This uses {@code geometry.getCentroid()} from JTS,
     * which returns the centroid in the same coordinate system as the geometry. If your
     * data is lat/lon (EPSG:4326), you'll get the result in degrees; for meters, you
     * must have a projected coordinate system.</p>
     *
     * @param propertyId    the objectID of the source property whose centroid distance to #11074 we want
     * @param allProperties a list of PropertyRecord objects, including #11074
     * @return the distance between centroids as a double; {@code Double.NaN} if #11074 or
     *         the source property is not found, or if the geometry parse fails
     */
    public static double distanceToFunchal(int propertyId, List<PropertyRecord> allProperties) {
        // 1) Find the source property record by propertyId
        PropertyRecord source = allProperties.stream()
                .filter(pr -> pr.getObjectID() == propertyId)
                .findFirst()
                .orElse(null);

        // 2) Find the Funchal (Sé) property record with objectID=11074
        PropertyRecord funchalSe = allProperties.stream()
                .filter(pr -> pr.getObjectID() == 11074)
                .findFirst()
                .orElse(null);

        // If either property was not found, return NaN
        if (source == null || funchalSe == null) {
            return Double.NaN;
        }

        // If either geometry is null or blank, return NaN
        String wktSource = source.getGeometry();
        String wktFunchal = funchalSe.getGeometry();
        if (wktSource == null || wktFunchal == null ||
                wktSource.isBlank() || wktFunchal.isBlank()) {
            return Double.NaN;
        }

        try {
            // 3) Parse WKT with JTS
            org.locationtech.jts.geom.Geometry geomSource =
                    new org.locationtech.jts.io.WKTReader().read(wktSource);
            org.locationtech.jts.geom.Geometry geomFunchal =
                    new org.locationtech.jts.io.WKTReader().read(wktFunchal);

            // 4) Get centroids
            org.locationtech.jts.geom.Point centroidSource = geomSource.getCentroid();
            org.locationtech.jts.geom.Point centroidFunchal = geomFunchal.getCentroid();

            // 5) Compute and return distance between centroids
            return centroidSource.distance(centroidFunchal);

        } catch (org.locationtech.jts.io.ParseException e) {
            // If the geometry fails to parse, return NaN
            return Double.NaN;
        }
    }
}
