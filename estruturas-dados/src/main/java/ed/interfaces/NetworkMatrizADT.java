package ed.interfaces;

import java.util.Iterator;

/**
 * Interface que define as operações para uma rede baseada em matriz.
 * Esta interface estende a {@link NetworkADT} para redes que utilizam uma matriz de adjacência.
 *
 * @param <T> o tipo dos elementos armazenados na rede
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanográfico: 8230148
 * @version 1.0
 */
public interface NetworkMatrizADT<T> extends NetworkADT<T> {

    /**
     * Retorna o peso da aresta entre dois vértices especificados.
     *
     * @param vertex  o primeiro vértice
     * @param vertex2 o segundo vértice
     * @return o peso da aresta entre os dois vértices
     */
    public double getWeightEdge(T vertex, T vertex2);

    /**
     * Define o custo de entrar no vértice indicado: todas as arestas que chegam a ele passam a ter
     * este peso (as que saem dele não mudam).
     *
     * @param vertex o vértice de destino
     * @param weight o novo custo de entrada
     */
    public void updateWeightEdge(T vertex, double weight);

    /**
     * Retorna um iterador que representa o caminho mais curto entre dois vértices.
     * O caminho é calculado a partir do vértice inicial até o vértice final.
     *
     * @param startVertex o vértice de início
     * @param finalVertex o vértice final
     * @return um iterador que contém o caminho mais curto entre os dois vértices
     */
    public Iterator<T> shortestPath(T startVertex, T finalVertex);

    /**
     * Retorna o peso total da aresta entre dois vértices, considerando o caminho mais curto.
     *
     * @param startVertex o vértice de início
     * @param finalVertex o vértice final
     * @return o peso total do caminho mais curto entre os dois vértices
     */
    public double shortestPathArest(T startVertex, T finalVertex);
}
