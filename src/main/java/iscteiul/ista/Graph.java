package iscteiul.ista;

import java.util.*;

/**
 * A Graph representation where each node is a property (with selected attributes),
 * and edges connect properties that are adjacent (touching) based on their geometry.
 */
public class Graph {

    /**
     * An inner class representing a "node" in the graph.
     * Each node corresponds to one PropertyRecord, storing:
     * - The key attributes you want (objectID, parcelID, etc.)
     * - A list (or set) of adjacent nodes
     */
    public static class GraphNode {
        private final int objectID;
        private final long parcelID;
        private final double shapeLength;
        private final double shapeArea;
        private final int owner;
        private final String parish;
        private final String municipality;
        private final String island;

        // You can store the geometry if you want to re-check adjacency later.
        private final String geometry;
        private final double centroidX;
        private final double centroidY;

        // For the adjacency list, we store the neighboring nodes.
        private final List<GraphNode> neighbors = new ArrayList<>();

        /**
         * Constructs a {@link GraphNode} based on the data from the given {@link PropertyRecord}.
         * <p>
         * This includes attempting to parse the WKT geometry to compute a centroid (if valid).
         *
         * @param record the source property record used to populate this node's attributes
         */
        public GraphNode(PropertyRecord record) {
            this.objectID = record.getObjectID();
            this.parcelID = record.getParcelID();
            this.shapeLength = record.getShapeLength();
            this.shapeArea = record.getShapeArea();
            this.owner = record.getOwner();
            this.parish = record.getParish();
            this.municipality = record.getMunicipality();
            this.island = record.getIsland();
            this.geometry = record.getGeometry();

            // Compute centroid from WKT, if valid.
            double tmpX = Double.NaN;
            double tmpY = Double.NaN;
            if (this.geometry != null) {
                try {
                    // Use JTS to parse the WKT and get centroid.
                    org.locationtech.jts.geom.Geometry geom =
                            new org.locationtech.jts.io.WKTReader().read(this.geometry);
                    org.locationtech.jts.geom.Point centroid = geom.getCentroid();
                    tmpX = centroid.getX();
                    tmpY = centroid.getY();
                } catch (org.locationtech.jts.io.ParseException e) {
                    System.err.println("Error parsing WKT for objectID=" + objectID + ": " + e.getMessage());
                }
            }
            this.centroidX = tmpX;
            this.centroidY = tmpY;
        }

        /**
         * Returns the unique ID (objectID) for this property node.
         *
         * @return the object's unique integer ID
         */
        public int getObjectID() {
            return objectID;
        }

        /**
         * Returns the parcel ID of this property.
         *
         * @return the parcel ID as a long
         */
        public long getParcelID() {
            return parcelID;
        }

        /**
         * Returns the length of this property's shape.
         *
         * @return the shape length in the same units as stored
         */
        public double getShapeLength() {
            return shapeLength;
        }

        /**
         * Returns the area of this property.
         *
         * @return the shape area in square units
         */
        public double getShapeArea() {
            return shapeArea;
        }

        /**
         * Returns the owner's integer ID for this property.
         *
         * @return the owner's ID
         */
        public int getOwner() {
            return owner;
        }

        /**
         * Returns the name of the parish where the property is located,
         * or {@code null} if not applicable.
         *
         * @return the parish name, or {@code null}
         */
        public String getParish() {
            return parish;
        }

        /**
         * Returns the name of the municipality where the property is located,
         * or {@code null} if not applicable.
         *
         * @return the municipality name, or {@code null}
         */
        public String getMunicipality() {
            return municipality;
        }

        /**
         * Returns the name of the island where the property is located,
         * or {@code null} if not applicable.
         *
         * @return the island name, or {@code null}
         */
        public String getIsland() {
            return island;
        }

        /**
         * Returns the raw WKT geometry string representing this property's boundaries,
         * or {@code null} if not provided.
         *
         * @return the property's WKT geometry, or {@code null}
         */
        public String getGeometry() {
            return geometry;
        }

        /**
         * Returns the x-coordinate of this property's centroid
         * (computed from the geometry), or {@code NaN} if invalid.
         *
         * @return the x-coordinate of the centroid
         */
        public double getCentroidX() {
            return centroidX;
        }

        /**
         * Returns the y-coordinate of this property's centroid
         * (computed from the geometry), or {@code NaN} if invalid.
         *
         * @return the y-coordinate of the centroid
         */
        public double getCentroidY() {
            return centroidY;
        }

        /**
         * Returns the list of adjacent (neighbor) nodes for this property node.
         * <p>
         * In an undirected property graph, these neighbors represent properties
         * that are spatially adjacent (touching boundaries) to this node.
         *
         * @return a list of adjacent {@link GraphNode} objects
         */
        public List<GraphNode> getNeighbors() {
            return neighbors;
        }

        /**
         * Adds a neighbor to the adjacency list.
         */
        private void addNeighbor(GraphNode neighbor) {
            neighbors.add(neighbor);
        }

        @Override
        public String toString() {
            return "GraphNode{" +
                    "objectID=" + objectID +
                    ", parcelID=" + parcelID +
                    ", shapeLength=" + shapeLength +
                    ", shapeArea=" + shapeArea +
                    ", owner=" + owner +
                    ", parish='" + parish + '\'' +
                    ", municipality='" + municipality + '\'' +
                    ", island='" + island + '\'' +
                    '}';
        }
    }

    /**
     * A map of objectID -> GraphNode for fast lookup by objectID.
     * Alternatively, you could store a List<GraphNode> if you prefer.
     */
    private final Map<Integer, GraphNode> nodesById;

    /**
     * Constructs the Graph from a list of PropertyRecord objects.
     *
     * @param propertyRecords The list of properties to be added as nodes in the graph.
     */
    public Graph(List<PropertyRecord> propertyRecords) {
        this.nodesById = new HashMap<>();
        buildNodes(propertyRecords);
        buildEdges(propertyRecords);
    }

    /**
     * Creates a GraphNode for each PropertyRecord and stores them in the map.
     */
    private void buildNodes(List<PropertyRecord> propertyRecords) {
        for (PropertyRecord record : propertyRecords) {
            GraphNode node = new GraphNode(record);
            nodesById.put(node.getObjectID(), node);
        }
    }

    /**
     * For each pair of PropertyRecord, checks adjacency using GeometryUtils.areAdjacent
     * and if adjacent, links their corresponding GraphNodes.
     */
    private void buildEdges(List<PropertyRecord> propertyRecords) {
        // We'll do a simple O(N^2) approach for adjacency.
        // For large datasets, consider spatial indexing.
        for (int i = 0; i < propertyRecords.size(); i++) {
            PropertyRecord recordA = propertyRecords.get(i);
            for (int j = i + 1; j < propertyRecords.size(); j++) {
                PropertyRecord recordB = propertyRecords.get(j);

                boolean adjacent = GeometryUtils.areAdjacent(
                        recordA.getGeometry(),
                        recordB.getGeometry()
                );
                if (adjacent) {
                    GraphNode nodeA = nodesById.get(recordA.getObjectID());
                    GraphNode nodeB = nodesById.get(recordB.getObjectID());
                    nodeA.addNeighbor(nodeB);
                    nodeB.addNeighbor(nodeA);  // undirected graph
                }
            }
        }
    }

    /**
     * Returns all GraphNodes in this graph.
     *
     * @return a Collection of all {@link GraphNode} objects in this graph
     */
    public Collection<GraphNode> getAllNodes() {
        return nodesById.values();
    }

    /**
     * Returns the node for a given objectID, or {@code null} if none exists.
     *
     * @param objectID the unique identifier of the property node
     * @return the {@link GraphNode} with the specified objectID, or {@code null} if no node is found
     */
    public GraphNode getNodeByObjectID(int objectID) {
        return nodesById.get(objectID);
    }

    /**
     * Returns the adjacency list (neighbors) for the specified objectID,
     * or an empty list if the objectID does not exist in the graph.
     *
     * @param objectID the unique identifier of the property node
     * @return a list of neighbor {@link GraphNode} objects, or an empty list if not found
     */
    public List<GraphNode> getNeighbors(int objectID) {
        GraphNode node = nodesById.get(objectID);
        if (node != null) {
            return node.getNeighbors();
        }
        return Collections.emptyList();
    }
}
