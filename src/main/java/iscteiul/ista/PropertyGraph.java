package iscteiul.ista;

import iscteiul.ista.GeometryUtils;
import iscteiul.ista.PropertyRecord;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.locationtech.jts.index.strtree.STRtree;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class  PropertyGraph{
    private Graph<PropertyRecord, DefaultEdge> graph;
    private STRtree spatialIndex; // Índice espacial para busca rápida

    public PropertyGraph() {
        this.graph = new SimpleGraph<>(DefaultEdge.class);
        this.spatialIndex = new STRtree(); // R-tree para otimização
    }

public void buildGraph(List<PropertyRecord> properties) {
    ConcurrentHashMap<Integer, PropertyRecord> propertyMap = new ConcurrentHashMap<>();

    // Passo 1: Adicionar todos os vértices primeiro
    properties.forEach(property -> {
        synchronized (this) {
            graph.addVertex(property);
            spatialIndex.insert(GeometryUtils.getEnvelope(property.getGeometry()), property);
            propertyMap.put(property.getObjectID(), property);
        }
    });

    // Passo 2: Adicionar as arestas com base na adjacência
    properties.forEach(property -> {
        // Apenas consultar vizinhos próximos usando o índice espacial.
        List<?> neighbors = spatialIndex.query(GeometryUtils.getEnvelope(property.getGeometry()));

        // Adicionar arestas para as propriedades adjacentes encontradas
        neighbors.forEach(obj -> {
            PropertyRecord neighbor = (PropertyRecord) obj;
            if (!property.equals(neighbor) && PropertyUtils.arePropertiesAdjacent(property, neighbor)) { // Usando PropertyUtils
                synchronized (this) {
                    if (graph.containsVertex(property) && graph.containsVertex(neighbor)) {
                        graph.addEdge(property, neighbor);
                    } else {
                        System.err.println("Erro: Tentando adicionar aresta com vértice inexistente -> " + property + " <-> " + neighbor);
                    }
                }
            }
        });
    });

    System.out.println("Grafo construído com " + graph.vertexSet().size() + " vértices e " + graph.edgeSet().size() + " arestas.");
}





public Graph<PropertyRecord, DefaultEdge> getGraph() {
        return graph;
    }
}
