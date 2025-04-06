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

        public int getObjectID() {
            return objectID;
        }

        public long getParcelID() {
            return parcelID;
        }

        public double getShapeLength() {
            return shapeLength;
        }

        public double getShapeArea() {
            return shapeArea;
        }

        public int getOwner() {
            return owner;
        }

        public String getParish() {
            return parish;
        }

        public String getMunicipality() {
            return municipality;
        }

        public String getIsland() {
            return island;
        }

        public String getGeometry() {
            return geometry;
        }

        public double getCentroidX() {
            return centroidX;
        }

        public double getCentroidY() {
            return centroidY;
        }

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
     */
    public Collection<GraphNode> getAllNodes() {
        return nodesById.values();
    }

    /**
     * Returns the node for a given objectID, or null if none.
     */
    public GraphNode getNodeByObjectID(int objectID) {
        return nodesById.get(objectID);
    }

    /**
     * Returns the adjacency list (neighbors) for a given objectID,
     * or an empty list if objectID is not found.
     */
    public List<GraphNode> getNeighbors(int objectID) {
        GraphNode node = nodesById.get(objectID);
        if (node != null) {
            return node.getNeighbors();
        }
        return Collections.emptyList();
    }
}
