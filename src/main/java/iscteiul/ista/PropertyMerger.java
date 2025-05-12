package iscteiul.ista;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for merging properties of the same owner that share boundaries,
 * resulting in one "merged" property per connected component.
 *
 * <p>Typically used to reduce fragmentation among properties that an owner
 * already holds. After this step, each owner has fewer, larger polygons.
 */
public final class PropertyMerger {

    private PropertyMerger() {
        // No instantiation
    }

    /**
     * Merges properties of the same owner that are adjacent (touch each other).
     * <p>
     * For each owner, we build a subgraph linking their properties that are
     * {@link GeometryUtils#areAdjacent(String, String) adjacent}. Then each
     * connected component is merged into one {@link PropertyRecord} whose geometry
     * is the union of all polygons in that component, with updated area, shapeLength, etc.
     *
     * @param properties the raw list of properties (usually from a single parish)
     * @return a new list of merged properties
     */
    public static List<PropertyRecord> mergeSameOwner(List<PropertyRecord> properties) {
        if (properties == null || properties.isEmpty()) {
            return new ArrayList<>();
        }

        // Build a graph of the input properties:
        // Vertices = each property, Edges = adjacency if same owner + touching
        SimpleGraph<PropertyRecord, DefaultEdge> graph =
                new SimpleGraph<>(DefaultEdge.class);

        // add all properties as vertices
        for (PropertyRecord p : properties) {
            graph.addVertex(p);
        }

        // check adjacency
        for (int i = 0; i < properties.size(); i++) {
            for (int j = i + 1; j < properties.size(); j++) {
                PropertyRecord a = properties.get(i);
                PropertyRecord b = properties.get(j);
                if (a.getOwner() == b.getOwner()) {
                    // If they have the same owner, check adjacency
                    if (GeometryUtils.areAdjacent(a.getGeometry(), b.getGeometry())) {
                        graph.addEdge(a, b);
                    }
                }
            }
        }

        // connectedSets
        ConnectivityInspector<PropertyRecord, DefaultEdge> inspector =
                new ConnectivityInspector<>(graph);
        List<Set<PropertyRecord>> connectedComponents = inspector.connectedSets();

        List<PropertyRecord> merged = new ArrayList<>();
        for (Set<PropertyRecord> component : connectedComponents) {
            // If only 1 property in that connected set, no merge needed
            if (component.size() == 1) {
                merged.add(component.iterator().next());
            } else {
                // Merge them into 1
                merged.add(unionComponent(component));
            }
        }

        return merged;
    }

    /**
     * Unions all polygons in the component, returning a new {@code PropertyRecord}
     * that adopts the metadata from the largest property in the set.
     */
    private static PropertyRecord unionComponent(Set<PropertyRecord> component) {
        // find the property with largest shapeArea in this component
        PropertyRecord largest = null;
        double maxArea = -1;
        for (PropertyRecord pr : component) {
            if (pr.getShapeArea() > maxArea) {
                maxArea = pr.getShapeArea();
                largest = pr;
            }
        }
        if (largest == null) {
            // fallback
            largest = component.iterator().next();
        }

        // union geometry
        Geometry unionGeom = null;
        WKTReader reader = new WKTReader();
        for (PropertyRecord pr : component) {
            try {
                Geometry g = reader.read(pr.getGeometry());
                if (unionGeom == null) {
                    unionGeom = g;
                } else {
                    unionGeom = unionGeom.union(g);
                }
            } catch (Exception e) {
                System.err.println("Skipping invalid geometry for " + pr.getObjectID());
            }
        }
        if (unionGeom == null) {
            // if all invalid, just return largest as-is
            return largest;
        }

        // If union ended up a single Polygon => wrap in MultiPolygon
        if (unionGeom instanceof Polygon) {
            GeometryFactory gf = new GeometryFactory();
            unionGeom = gf.createMultiPolygon(new Polygon[]{(Polygon) unionGeom});
        }

        // recalc area and perimeter
        double mergedArea = unionGeom.getArea();
        double mergedLength = unionGeom.getLength();
        String mergedWKT = unionGeom.toText();

        // build a new PropertyRecord adopting the largest's ID, etc.
        PropertyRecord merged = new PropertyRecord(
                largest.getObjectID(),
                largest.getParcelID(),
                largest.getParcelNumber(),
                mergedLength,
                mergedArea,
                mergedWKT,
                largest.getOwner(),
                largest.getParish(),
                largest.getMunicipality(),
                largest.getIsland()
        );
        return merged;
    }
}
