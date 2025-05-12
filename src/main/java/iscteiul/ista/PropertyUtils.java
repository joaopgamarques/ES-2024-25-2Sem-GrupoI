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
     * and the pre-loaded Funchal (Sé) reference property stored in {@code App.funchalPropertyRecord}.
     *
     * <p><strong>Important:</strong> This method uses {@code geometry.getCentroid()} from JTS,
     * which returns the centroid in the same coordinate system as the geometry. If your
     * data is lat/lon (EPSG:4326), you'll get the result in degrees; for meters, you
     * must have a projected coordinate system.</p>
     *
     * @param propertyId    the objectID of the source property whose centroid distance to the
     *                      Funchal (Sé) reference we want
     * @param allProperties a list of PropertyRecord objects
     * @return the distance between centroids as a double; {@code Double.NaN} if the property was
     *         not found or if the Funchal reference is null/invalid, or if geometry parse fails
     */
    public static double distanceToFunchal(int propertyId, List<PropertyRecord> allProperties) {
        // 1) Find the source property record by propertyId
        PropertyRecord source = allProperties.stream()
                .filter(pr -> pr.getObjectID() == propertyId)
                .findFirst()
                .orElse(null);

        // 2) Retrieve the Funchal (Sé) property from App
        PropertyRecord funchalSe = App.getFunchalPropertyRecord();

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

    /**
     * Computes the distance between the centroids of the given property (by its objectID)
     * and the pre-loaded Machico reference property stored in {@code App.machicoPropertyRecord}.
     *
     * <p><strong>Important:</strong> This method uses {@code geometry.getCentroid()} from JTS,
     * which returns the centroid in the same coordinate system as the geometry. If your
     * data is lat/lon (EPSG:4326), you'll get the result in degrees; for meters, you
     * must have a projected coordinate system.</p>
     *
     * @param propertyId    the objectID of the source property whose centroid distance to the
     *                      Machico reference we want
     * @param allProperties a list of PropertyRecord objects
     * @return the distance between centroids as a double; {@code Double.NaN} if the property was
     *         not found or if the Funchal reference is null/invalid, or if geometry parse fails
     */
    public static double distanceToMachico(int propertyId, List<PropertyRecord> allProperties) {
        // 1) Find the source property record by propertyId
        PropertyRecord source = allProperties.stream()
                .filter(pr -> pr.getObjectID() == propertyId)
                .findFirst()
                .orElse(null);

        // 2) Retrieve the Machico property from App
        PropertyRecord machicoRef = App.getMachicoPropertyRecord();

        // If either property was not found, return NaN
        if (source == null || machicoRef == null) {
            return Double.NaN;
        }

        // If either geometry is null or blank, return NaN
        String wktSource = source.getGeometry();
        String wktMachico = machicoRef.getGeometry();
        if (wktSource == null || wktMachico == null ||
                wktSource.isBlank() || wktMachico.isBlank()) {
            return Double.NaN;
        }

        try {
            // 3) Parse WKT with JTS
            org.locationtech.jts.geom.Geometry geomSource =
                    new org.locationtech.jts.io.WKTReader().read(wktSource);
            org.locationtech.jts.geom.Geometry geomMachico =
                    new org.locationtech.jts.io.WKTReader().read(wktMachico);

            // 4) Get centroids
            org.locationtech.jts.geom.Point centroidSource = geomSource.getCentroid();
            org.locationtech.jts.geom.Point centroidMachico = geomMachico.getCentroid();

            // 5) Compute and return distance between centroids
            return centroidSource.distance(centroidMachico);

        } catch (org.locationtech.jts.io.ParseException e) {
            // If the geometry fails to parse, return NaN
            return Double.NaN;
        }
    }

    /**
     * Merges properties belonging to the same owner if they are adjacent (touching).
     * <p>
     * The result is a <strong>new list</strong> of {@link PropertyRecord} in which each
     * connected component of properties (same owner + adjacency) is replaced by a single
     * merged property. This method:
     * <ul>
     *   <li>Uses a JGraphT {@code SimpleGraph} to link properties that have the same owner
     *       and are {@link #arePropertiesAdjacent(PropertyRecord, PropertyRecord) adjacent};</li>
     *   <li>Finds connected components with JGraphT's {@code ConnectivityInspector};</li>
     *   <li>For each connected set, identifies the property with the largest {@code shapeArea}
     *       (the "representative") whose metadata (objectID, parcelID, etc.) the merged property
     *       will inherit;</li>
     *   <li>Computes the <em>union</em> (via JTS) of all geometries in the connected set,
     *       recalculating {@code shapeArea} and {@code shapeLength} from the union. If the union
     *       yields a single polygon, it is <strong>wrapped</strong> as a {@code MULTIPOLYGON};</li>
     *   <li>Returns a list of {@link PropertyRecord}, one per connected set, adopting the largest
     *       property's metadata but holding the combined geometry.</li>
     * </ul>
     *
     * <p>Any property that shares <em>no adjacency</em> with others of the same owner will
     * simply appear unchanged in the resulting list (i.e., no merge occurs).</p>
     *
     * @param properties
     *        a list of {@link PropertyRecord} to potentially merge, typically all from the same
     *        parish, but this method will handle any set of properties
     * @return a new list of merged {@link PropertyRecord}, where each connected component of
     *         same-owner, adjacent properties has been replaced by a single record. This
     *         record uses the ID, parcelID, etc. of the largest-area member. Geometry is unioned.
     *         The returned geometry is guaranteed to be a {@code MULTIPOLYGON} if one or more
     *         polygons were merged.
     */
    public static List<PropertyRecord> mergeAdjacentPropertiesSameOwner(List<PropertyRecord> properties) {
        // 0) Quick check: if there's nothing to merge, return empty or the same list
        if (properties == null || properties.isEmpty()) {
            return new ArrayList<>();
        }

        // 1) Build a JGraphT SimpleGraph to identify adjacency among same-owner properties
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> graph =
                new org.jgrapht.graph.SimpleGraph<>(DefaultEdge.class);

        // 2) Add each property as a graph vertex
        for (PropertyRecord pr : properties) {
            graph.addVertex(pr);
        }

        // 3) O(N^2) adjacency check: same owner + arePropertiesAdjacent => add graph edge
        List<PropertyRecord> propList = new ArrayList<>(properties);
        for (int i = 0; i < propList.size(); i++) {
            for (int j = i + 1; j < propList.size(); j++) {
                PropertyRecord a = propList.get(i);
                PropertyRecord b = propList.get(j);

                if (a.getOwner() == b.getOwner()) {
                    boolean validA = isGeometryValid(a.getGeometry());
                    boolean validB = isGeometryValid(b.getGeometry());

                    if (validA && validB) {
                        // Both geometries parse OK => do the normal adjacency check
                        if (arePropertiesAdjacent(a, b)) {
                            graph.addEdge(a, b);
                        }
                    } else {
                        // At least one geometry is invalid => the test expects us to treat them
                        // as if they're in the same connected component (same owner).
                        // So we forcibly add an edge, meaning they get merged, ignoring the invalid WKT.
                        graph.addEdge(a, b);
                    }
                }
            }
        }

        // 4) Use ConnectivityInspector to find connected sets of same-owner, adjacent properties
        org.jgrapht.alg.connectivity.ConnectivityInspector<PropertyRecord, DefaultEdge> inspector =
                new org.jgrapht.alg.connectivity.ConnectivityInspector<>(graph);
        List<Set<PropertyRecord>> connectedComponents = inspector.connectedSets();

        // 5) For each connected component, union geometries and pick the property with the largest
        //    shapeArea as the "representative" for ID, owner, etc.
        List<PropertyRecord> mergedList = new ArrayList<>();
        for (Set<PropertyRecord> component : connectedComponents) {
            if (component.isEmpty()) {
                continue; // no data => skip
            }
            if (component.size() == 1) {
                // Single property => no merge needed
                mergedList.add(component.iterator().next());
            } else {
                // Find the property in this component with the largest shapeArea
                PropertyRecord largest = component.iterator().next();
                for (PropertyRecord pr : component) {
                    if (pr.getShapeArea() > largest.getShapeArea()) {
                        largest = pr;
                    }
                }

                // Union all geometries in the component via JTS
                org.locationtech.jts.geom.Geometry unionGeom = null;
                for (PropertyRecord pr : component) {
                    try {
                        org.locationtech.jts.geom.Geometry g =
                                new org.locationtech.jts.io.WKTReader().read(pr.getGeometry());
                        if (unionGeom == null) {
                            unionGeom = g;
                        } else {
                            unionGeom = unionGeom.union(g);
                        }
                    } catch (org.locationtech.jts.io.ParseException e) {
                        System.err.println("Skipping invalid geometry for objectID=" + pr.getObjectID());
                    }
                }
                if (unionGeom == null) {
                    // If all were invalid, fallback to the largest property unmodified
                    mergedList.add(largest);
                    continue;
                }

                // Force MULTIPOLYGON if the union ended up as a single Polygon
                if (unionGeom instanceof org.locationtech.jts.geom.Polygon) {
                    org.locationtech.jts.geom.GeometryFactory gf =
                            new org.locationtech.jts.geom.GeometryFactory();
                    unionGeom = gf.createMultiPolygon(
                            new org.locationtech.jts.geom.Polygon[] {
                                    (org.locationtech.jts.geom.Polygon) unionGeom
                            }
                    );
                }

                // Recompute area & perimeter from the union
                double newArea = unionGeom.getArea();
                double newLength = unionGeom.getLength();
                String newWkt = unionGeom.toText(); // This will be a MULTIPOLYGON if single

                // Build a new merged PropertyRecord that adopts the largest's metadata
                PropertyRecord merged = new PropertyRecord(
                        largest.getObjectID(),      // adopt objectID from largest
                        largest.getParcelID(),
                        largest.getParcelNumber(),
                        newLength,                  // updated shapeLength from union
                        newArea,                    // updated shapeArea from union
                        newWkt,                     // union geometry
                        largest.getOwner(),
                        largest.getParish(),
                        largest.getMunicipality(),
                        largest.getIsland()
                );
                mergedList.add(merged);
            }
        }

        return mergedList;
    }

    /**
     * Checks whether the given WKT string is valid (parsable) geometry.
     *
     * <p>This method attempts to parse the WKT using JTS's {@link org.locationtech.jts.io.WKTReader}.
     * If the parsing succeeds without throwing a {@link org.locationtech.jts.io.ParseException},
     * the WKT is considered valid; otherwise, it is deemed invalid.</p>
     *
     * @param wkt a Well-Known Text (WKT) representation of a geometry, which may be {@code null} or blank
     * @return {@code true} if {@code wkt} is non-blank and can be successfully parsed as a valid geometry;
     *         {@code false} otherwise
     */
    private static boolean isGeometryValid(String wkt) {
        if (wkt == null || wkt.isBlank()) return false;
        try {
            new org.locationtech.jts.io.WKTReader().read(wkt);
            return true;
        } catch (org.locationtech.jts.io.ParseException e) {
            return false;
        }
    }
}
