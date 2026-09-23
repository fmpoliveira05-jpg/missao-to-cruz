package ed.graph;

import ed.interfaces.NetworkMatrizADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;

import java.util.Iterator;

/**
 * A classe Network estende a classe NetworkMatrizAdjacencia e implementa a interface NetworkMatrizADT.
 * Representa uma rede de grafos com funcionalidades específicas para manipulação de pesos em arestas
 * e cálculo de caminhos mais curtos entre vértices.
 *
 * @param <T> O tipo dos vértices da rede.
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanográfico: 8230148
 * @version 1.0
 */
public class Network<T> extends NetworkMatrizAdjacencia<T> implements NetworkMatrizADT<T> {

    /**
     * Construtor default da classe Network.
     * Inicializa a rede chamando o construtor da classe pai.
     */
    public Network() {
        super();
    }

    /**
     * Retorna o peso da aresta entre dois vértices do grafo.
     *
     * @param vertex O primeiro vértice.
     * @param vertex2 O segundo vértice.
     * @return O peso da aresta entre os dois vértices, ou infinito positivo se não houver aresta.
     */
    @Override
    public double getWeightEdge(T vertex, T vertex2) {
        int ind_vertex = getIndex(vertex);
        int ind_vertex2 = getIndex(vertex2);

        return getWeightEdge(ind_vertex, ind_vertex2);
    }

    /**
     * Retorna o peso da aresta entre dois vértices do grafo, dado seus índices.
     *
     * @param vertex O índice do primeiro vértice.
     * @param vertex2 O índice do segundo vértice.
     * @return O peso da aresta entre os dois vértices, ou infinito positivo se não houver aresta.
     */
    private double getWeightEdge(int vertex, int vertex2) {
        double weight = 0;

        if (indexIsValid(vertex) && indexIsValid(vertex2)) {
            weight = this.adjMatrix[vertex][vertex2];
        } else {
            weight = Double.POSITIVE_INFINITY;
        }

        return weight;
    }

    /**
     * Define o custo de <b>entrar</b> no vértice fornecido, vindo de qualquer vizinho.
     *
     * <p>Só as arestas que chegam ao vértice ({@code vizinho → vertex}) são alteradas; as que saem
     * dele mantêm o custo de entrar nos vizinhos. No jogo, o custo de uma divisão é o poder total dos
     * inimigos que lá estão: é esse o dano que o Tó Cruz sofre ao entrar. A versão original escrevia o
     * mesmo valor nos dois sentidos, pelo que o custo de uma ligação dependia da última divisão
     * atualizada e sair de uma sala perigosa custava tanto como entrar nela.</p>
     *
     * @param vertex O vértice cujo custo de entrada é atualizado.
     * @param weight O novo custo de entrada.
     */
    @Override
    public void updateWeightEdge(T vertex, double weight) {
        int destino = getIndex(vertex);

        if (indexIsValid(destino)) {
            for (int origem = 0; origem < numVertices; origem++) {
                if (this.adjMatrix[origem][destino] != Double.POSITIVE_INFINITY) {
                    this.adjMatrix[origem][destino] = weight;
                }
            }
        }
    }

    /**
     * Calcula o caminho de menor custo entre dois vértices (algoritmo de Dijkstra).
     *
     * @param startVertex O vértice de origem.
     * @param finalVertex O vértice de destino.
     * @return Um iterador com os vértices do caminho, da origem ao destino,
     * ou um iterador vazio se não houver caminho.
     */
    @Override
    public Iterator<T> shortestPath(T startVertex, T finalVertex) {
        UnorderedListADT<T> caminho = new LinearLinkedUnorderedList<>();
        int startIndex = getIndex(startVertex);
        int finalIndex = getIndex(finalVertex);

        if (!indexIsValid(startIndex) || !indexIsValid(finalIndex)) {
            return caminho.iterator();
        }

        CaminhosMinimos minimos = dijkstra(startIndex);
        if (minimos.distancias[finalIndex] == Double.MAX_VALUE) {
            return caminho.iterator();
        }

        for (int passo = finalIndex; passo != -1; passo = minimos.antecessores[passo]) {
            caminho.addToFront(this.vertices[passo]);
        }

        return caminho.iterator();
    }

    /**
     * Número de arestas do caminho de menor custo entre dois vértices.
     *
     * @param startVertex O vértice de origem.
     * @param finalVertex O vértice de destino.
     * @return O número de arestas desse caminho, ou {@link Double#MAX_VALUE} se não houver caminho.
     */
    @Override
    public double shortestPathArest(T startVertex, T finalVertex) {
        int startIndex = getIndex(startVertex);
        int finalIndex = getIndex(finalVertex);

        if (!indexIsValid(startIndex) || !indexIsValid(finalIndex)) {
            return Double.MAX_VALUE;
        }

        CaminhosMinimos minimos = dijkstra(startIndex);
        if (minimos.distancias[finalIndex] == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        return minimos.arestas[finalIndex];
    }
}
