package iscteiul.ista;

import org.junit.jupiter.api.Test;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de teste para a classe {@link GraphVisualization}.
 * <p>
 * Esta classe contém métodos de teste para verificar a funcionalidade da classe {@link GraphVisualization}.
 * </p>
 * <p>
 * Autor: bfaae
 * Data: 2024-03-05
 * </p>
 */
class GraphVisualizationTest {

    /**
     * Testa o método {@link GraphVisualization#visualizeGraph(PropertyGraph)}.
     * <p>
     * Complexidade ciclomática: 1
     * </p>
     */
    @Test
    void visualizeGraph() {
        // Cria um grafo de exemplo
        PropertyGraph propertyGraph = new PropertyGraph();
        PropertyRecord record1 = new PropertyRecord(1, 1L, 1L, 100.0, 200.0, "MULTIPOLYGON(((0 0, 1 0, 1 1, 0 1, 0 0)))", 1, "Parish1", "Municipality1", "Island1");
        PropertyRecord record2 = new PropertyRecord(2, 2L, 2L, 150.0, 250.0, "MULTIPOLYGON(((1 1, 2 1, 2 2, 1 2, 1 1)))", 2, "Parish2", "Municipality2", "Island2");
        propertyGraph.buildGraph(Arrays.asList(record1, record2));

        // Chama o método a ser testado
        GraphVisualization.visualizeGraph(propertyGraph);

        // Verifica se o grafo foi visualizado corretamente
        // (Aqui você pode adicionar verificações específicas dependendo da implementação do método visualizeGraph)
        assertNotNull(propertyGraph.getGraph(), "O grafo não deveria ser nulo.");
        assertTrue(propertyGraph.getGraph().containsVertex(record1), "O vértice record1 deveria estar presente no grafo.");
        assertTrue(propertyGraph.getGraph().containsVertex(record2), "O vértice record2 deveria estar presente no grafo.");
    }
}