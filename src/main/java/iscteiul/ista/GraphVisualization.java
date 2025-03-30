package iscteiul.ista;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.jgrapht.graph.DefaultEdge;

public class GraphVisualization {

    public static void visualizeGraph(PropertyGraph propertyGraph) {
        // Criar o grafo do GraphStream
        Graph graphStreamGraph = new SingleGraph("Property Graph");
        System.setProperty("org.graphstream.ui", "swing");

        // Adicionar nós
        for (PropertyRecord property : propertyGraph.getGraph().vertexSet()) {
            Node node = graphStreamGraph.addNode(String.valueOf(property.getObjectID()));
            node.setAttribute("ui.label", property.getParcelID()); // Mostrar parcelID como legenda
        }

        // Adicionar arestas
        for (DefaultEdge edge : propertyGraph.getGraph().edgeSet()) {
            PropertyRecord source = propertyGraph.getGraph().getEdgeSource(edge);
            PropertyRecord target = propertyGraph.getGraph().getEdgeTarget(edge);

            graphStreamGraph.addEdge(
                    source.getObjectID() + "-" + target.getObjectID(),
                    String.valueOf(source.getObjectID()),
                    String.valueOf(target.getObjectID())
            );
        }

        // Definir estilos diferentes para nós conectados e isolados
        for (Node node : graphStreamGraph) {
            if (node.getDegree() > 0) {
                node.setAttribute("ui.style", "fill-color: green; size: 10px; text-size: 12px;");
            } else {
                node.setAttribute("ui.style", "fill-color: red; size: 10px; text-size: 12px;");
            }
        }

        // Melhorar visualização com layout automático
        graphStreamGraph.setAttribute("ui.quality");
        graphStreamGraph.setAttribute("ui.antialias");

        // Criar e exibir a visualização interativa do grafo
        Viewer viewer = graphStreamGraph.display();
        viewer.enableAutoLayout();
    }
}
