package iscteiul.ista;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.jgrapht.graph.DefaultEdge;

/**
 * The {@code GraphVisualization} class provides methods to visualize
 * a {@link PropertyGraph} using the GraphStream library.
 *
 * <p>It transforms the internal JGraphT structure into a GraphStream
 * graph, enabling interactive visual inspection of adjacency relationships
 * between {@link PropertyRecord} objects.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // 1. Construct or retrieve an existing PropertyGraph
 * PropertyGraph propertyGraph = new PropertyGraph();
 * propertyGraph.buildGraph(propertyList);
 *
 * // 2. Pass it to the visualizeGraph method
 * GraphVisualization.visualizeGraph(propertyGraph);
 * }</pre>
 */
public class GraphVisualization {

    /**
     * Visualizes the given {@link PropertyGraph} in an interactive GraphStream window.
     * <ul>
     *   <li>Converts each {@code PropertyRecord} from the JGraphT graph into a GraphStream node,
     *       labeling it with the property's {@code parcelID}.</li>
     *   <li>Establishes edges between nodes for every {@link DefaultEdge} in the JGraphT graph.</li>
     *   <li>Applies basic styling: connected nodes appear green, isolated nodes appear red.</li>
     *   <li>Enables quality settings (antialiasing) for a smoother visualization and starts
     *       auto-layout to position nodes automatically.</li>
     * </ul>
     *
     * @param propertyGraph the {@link PropertyGraph} to be visualized. Must not be null.
     */
    public static void visualizeGraph(PropertyGraph propertyGraph) {
        // Create a new GraphStream graph instance
        Graph graphStreamGraph = new SingleGraph("Property Graph");
        System.setProperty("org.graphstream.ui", "swing");

        // Add a GraphStream node for each PropertyRecord (vertex in JGraphT).
        for (PropertyRecord property : propertyGraph.getGraph().vertexSet()) {
            Node node = graphStreamGraph.addNode(String.valueOf(property.getObjectID()));
            node.setAttribute("ui.label", property.getParcelID());
        }

        // Add GraphStream edges for each adjacency edge in the JGraphT graph.
        for (DefaultEdge edge : propertyGraph.getGraph().edgeSet()) {
            PropertyRecord source = propertyGraph.getGraph().getEdgeSource(edge);
            PropertyRecord target = propertyGraph.getGraph().getEdgeTarget(edge);

            graphStreamGraph.addEdge(
                    source.getObjectID() + "-" + target.getObjectID(),
                    String.valueOf(source.getObjectID()),
                    String.valueOf(target.getObjectID())
            );
        }

        // Apply color styling: green for connected nodes, red for isolated ones.
        for (Node node : graphStreamGraph) {
            if (node.getDegree() > 0) {
                node.setAttribute("ui.style", "fill-color: green; size: 10px; text-size: 12px;");
            } else {
                node.setAttribute("ui.style", "fill-color: red; size: 10px; text-size: 12px;");
            }
        }

        // Enable anti-aliasing and quality settings.
        graphStreamGraph.setAttribute("ui.quality");
        graphStreamGraph.setAttribute("ui.antialias");

        // Display the graph in an interactive viewer with auto-layout.
        Viewer viewer = graphStreamGraph.display();
        viewer.enableAutoLayout();
    }
}
