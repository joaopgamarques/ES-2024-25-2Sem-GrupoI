package iscteiul.ista;

import org.junit.jupiter.api.Test;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de teste para a classe {@link PropertyGraph}.
 * <p>
 * Esta classe contém métodos de teste para verificar a funcionalidade da classe {@link PropertyGraph}.
 * </p>
 * <p>
 * Autor: bfaae
 * Data: 2024-03-05
 * </p>
 */
class PropertyGraphTest {

    /**
     * Testa o método {@link PropertyGraph#buildGraph(List)}.
     * <p>
     * Complexidade ciclomática: 2
     * </p>
     */
    @Test
    void buildGraph1() {
        PropertyGraph propertyGraph = new PropertyGraph();
        PropertyRecord record1 = new PropertyRecord(1, 1L, 1L, 100.0, 200.0, "MULTIPOLYGON(((0 0, 1 0, 1 1, 0 1, 0 0)))", 1, "Parish1", "Municipality1", "Island1");
        PropertyRecord record2 = new PropertyRecord(2, 2L, 2L, 150.0, 250.0, "MULTIPOLYGON(((1 1, 2 1, 2 2, 1 2, 1 1)))", 2, "Parish2", "Municipality2", "Island2");
        List<PropertyRecord> records = Arrays.asList(record1, record2);

        propertyGraph.buildGraph(records);
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Verifica se os vértices foram adicionados corretamente
        assertTrue(graph.containsVertex(record1), "O vértice record1 deveria estar presente no grafo.");
        assertTrue(graph.containsVertex(record2), "O vértice record2 deveria estar presente no grafo.");
    }

    @Test
    void buildGraph2() {
        PropertyGraph propertyGraph = new PropertyGraph();
        PropertyRecord record1 = new PropertyRecord(1, 1L, 1L, 100.0, 200.0, "MULTIPOLYGON(((0 0, 1 0, 1 1, 0 1, 0 0)))", 1, "Parish1", "Municipality1", "Island1");
        PropertyRecord record2 = new PropertyRecord(2, 2L, 2L, 150.0, 250.0, "MULTIPOLYGON(((1 1, 2 1, 2 2, 1 2, 1 1)))", 2, "Parish2", "Municipality2", "Island2");
        PropertyRecord record3 = new PropertyRecord(3, 3L, 3L, 200.0, 300.0, "MULTIPOLYGON(((2 2, 3 2, 3 3, 2 3, 2 2)))", 3, "Parish3", "Municipality3", "Island3");
        List<PropertyRecord> records = Arrays.asList(record1, record2, record3);

        propertyGraph.buildGraph(records);
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Verifica se as arestas foram adicionadas corretamente
        assertTrue(graph.containsEdge(record1, record2) || graph.containsEdge(record2, record1), "A aresta entre record1 e record2 deveria estar presente no grafo.");
        assertTrue(graph.containsEdge(record2, record3) || graph.containsEdge(record3, record2), "A aresta entre record2 e record3 deveria estar presente no grafo.");
    }

    /**
     * Testa o método {@link PropertyGraph#getGraph()}.
     * <p>
     * Complexidade ciclomática: 1
     * </p>
     */
    @Test
    void getGraph() {
        PropertyGraph propertyGraph = new PropertyGraph();
        Graph<PropertyRecord, DefaultEdge> graph = propertyGraph.getGraph();

        // Verifica se o grafo não é nulo
        assertNotNull(graph, "O grafo não deveria ser nulo.");
    }
}