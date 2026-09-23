package ed.graph;

import ed.graph.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;

public class TestNetwork {

    private Network<String> graph;

    @BeforeEach
    public void setUp() {
        graph = new Network<>();
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
    }

    @Test
    public void testGetWeightEdge() {
        graph.addEdge("A", "B", 5.0);

        double weight = graph.getWeightEdge("A", "B");
        assertEquals(5.0, weight, "O peso da aresta deve ser 5.0.");

        weight = graph.getWeightEdge("A", "C");
        assertEquals(Double.POSITIVE_INFINITY, weight, "O peso da aresta inexistente deve ser infinito.");
    }

    @Test
    public void testUpdateWeightEdgeChangesOnlyTheCostOfEntering() {
        graph.addEdge("A", "B", 5.0);
        graph.updateWeightEdge("A", 10.0);

        assertEquals(10.0, graph.getWeightEdge("B", "A"), "Entrar em A passa a custar 10.");
        assertEquals(5.0, graph.getWeightEdge("A", "B"), "Sair de A para B continua a custar o mesmo.");
    }

    @Test
    public void testUpdateWeightEdgeDoesNotDependOnUpdateOrder() {
        graph.addEdge("A", "B", 0.0);
        graph.updateWeightEdge("B", 30.0);
        graph.updateWeightEdge("A", 0.0);

        assertEquals(30.0, graph.getWeightEdge("A", "B"), "Atualizar A não pode apagar o custo de entrar em B.");
    }

    /**
     * Grafo em que fechar os vértices por ordem alfabética (e não pela distância) dá um caminho
     * errado: A-B-D custa 20, mas A-Z-D custa 2 e "Z" só seria visitado depois de "D".
     */
    @Test
    public void testShortestPathPicksTheCheapestRouteNotTheFirstVertices() {
        graph.addVertex("D");
        graph.addVertex("Z");
        graph.addEdge("A", "B", 10.0);
        graph.addEdge("B", "D", 10.0);
        graph.addEdge("A", "Z", 1.0);
        graph.addEdge("Z", "D", 1.0);

        Iterator<String> path = graph.shortestPath("A", "D");
        assertEquals("A", path.next());
        assertEquals("Z", path.next());
        assertEquals("D", path.next());
        assertFalse(path.hasNext());
        assertEquals(2.0, graph.shortestPathWeight("A", "D"));
        assertEquals(2.0, graph.shortestPathArest("A", "D"));
    }

    @Test
    public void testShortestPathWithEntryCosts() {
        graph.addVertex("D");
        graph.addEdge("A", "B", 0.0);
        graph.addEdge("B", "D", 0.0);
        graph.addEdge("A", "C", 0.0);
        graph.addEdge("C", "D", 0.0);
        graph.updateWeightEdge("B", 40.0);
        graph.updateWeightEdge("C", 15.0);

        Iterator<String> path = graph.shortestPath("A", "D");
        path.next();
        assertEquals("C", path.next(), "Deve evitar a divisão com mais inimigos.");
        assertEquals(15.0, graph.shortestPathWeight("A", "D"));
    }

    @Test
    public void testShortestPath() {
        graph.addEdge("A", "B", 5.0);
        graph.addEdge("B", "C", 2.0);

        Iterator<String> path = graph.shortestPath("A", "C");
        assertTrue(path.hasNext(), "Deve haver um caminho mais curto entre A e C.");
        assertEquals("A", path.next(), "O primeiro vértice do caminho deve ser A.");
        assertEquals("B", path.next(), "O segundo vértice do caminho deve ser B.");
        assertEquals("C", path.next(), "O terceiro vértice do caminho deve ser C.");
    }

    @Test
    public void testShortestPathNoPath() {
        Iterator<String> path = graph.shortestPath("A", "B");
        assertFalse(path.hasNext(), "Não deve haver caminho entre A e B.");
    }

    @Test
    public void testShortestPathArest() {
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");

        graph.addEdge("A", "B", 5.0);
        graph.addEdge("B", "C", 2.0);
        double numEdges = graph.shortestPathArest("A", "C");
        assertEquals(2.0, numEdges, "O número mínimo de arestas entre A e C deve ser 2.");
        assertEquals(2.0, numEdges, "O número mínimo de arestas entre A e C deve ser 2.");
    }

    @Test
    public void testShortestPathArestNoPath() {
        double numEdges = graph.shortestPathArest("A", "B");
        assertEquals(Double.MAX_VALUE, numEdges, "Se não houver caminho, o número de arestas deve ser infinito.");
    }
}
