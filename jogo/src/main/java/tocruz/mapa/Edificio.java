package tocruz.mapa;

import ed.arraylist.ArrayUnordered;
import ed.graph.Network;
import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.QueueADT;
import ed.queue.LinkedQueue;
import tocruz.interfaces.EdificoInt;
import ed.interfaces.NetworkMatrizADT;
import tocruz.personagens.Inimigo;
import tocruz.personagens.ToCruz;

import java.util.Iterator;
import java.util.Objects;

/**
 * Classe que representa um edifício no jogo.
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 1.0
 */
public class Edificio implements EdificoInt {

    /**
     * Contador para atribuir IDs únicos aos edifícios.
     */
    private static int ID_EDIFICIO_CONT = 0;

    /**
     * Nome padrão para o edifício caso não seja fornecido um.
     */
    private static final String NAME_DEFAULT = "CIA Headquarters";

    /**
     * Identificador único do edifício.
     */
    private int id;

    /**
     * Nome do edifício.
     */
    private String name;

    /**
     * Grafo pesado que representa as divisões e respetivas conexões no edifício.
     */
    private NetworkMatrizADT<Divisao> planta_edificio;

    /**
     * Construtor default do edifício.
     * Inicializa o nome com o valor default e a planta do edifício como um grafo pesado vazio.
     */
    public Edificio() {
        this.id = ID_EDIFICIO_CONT++;
        this.name = NAME_DEFAULT;
        this.planta_edificio = new Network<>();
    }

    /**
     * Cria uma cópia independente de um edifício, usada para que cada simulação comece com os
     * inimigos, itens e alvo no estado original da missão.
     *
     * <p>As divisões são copiadas (com os seus inimigos, item e alvo) e depois são recriadas apenas
     * as ligações que existem no original. A versão anterior chamava {@code addEdge} para todos os
     * pares de divisões, incluindo os que não estavam ligados, e copiava o peso de um só sentido
     * para os dois; os custos de entrada são agora recalculados a partir dos inimigos.</p>
     *
     * @param id_edificio identificador do edifício
     * @param name nome do edifício
     * @param planta_edificio planta a copiar
     */
    public Edificio(int id_edificio, String name, NetworkMatrizADT<Divisao> planta_edificio) {
        this.id = id_edificio;
        this.name = name;
        this.planta_edificio = new Network<>();

        for (Divisao divisao : planta_edificio) {
            this.planta_edificio.addVertex(new Divisao(divisao.getId_divisao(), divisao.getName(),
                    divisao.isEntrada_saida(), divisao.getAlvo(), divisao.getItem(),
                    divisao.getInimigos(), divisao.getToCruz()));
        }

        // As divisões copiadas são iguais (equals por nome) às originais, por isso o grafo novo
        // encontra-as a partir das originais.
        for (Divisao divisao : planta_edificio) {
            Iterator<Divisao> vizinhos = planta_edificio.iteratorNextVertexs(divisao);
            while (vizinhos.hasNext()) {
                this.planta_edificio.addEdge(divisao, vizinhos.next(), 0);
            }
        }

        atualizarCustos();
    }

    public int getId() {
        return id;
    }

    /**
     * Retorna o nome do edifício.
     *
     * @return Nome do edifício.
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna a planta do edifício (grafo pesado de divisões).
     *
     * @return Grafo pesado que representa as divisões e conexões do edifício.
     */
    public NetworkMatrizADT<Divisao> getPlantaEdificio() {
        return planta_edificio;
    }

    /**
     * Retorna o caminho mais curto entre duas divisões, considerando o peso das arestas.
     *
     * @param div_inicial Divisão inicial.
     * @param div_final Divisão final.
     * @return Peso do caminho mais curto entre as divisões.
     */
    @Override
    public double getShortestPath(Divisao div_inicial, Divisao div_final) {
        return this.planta_edificio.shortestPathWeight(div_inicial, div_final);
    }

    /**
     * Retorna o número de arestas no caminho mais curto entre duas divisões.
     *
     * @param div_inicial Divisão inicial.
     * @param div_final Divisão final.
     * @return Número de arestas no caminho mais curto entre as divisões.
     */
    @Override
    public double getShortestPathNumArestas(Divisao div_inicial, Divisao div_final) {
        return this.planta_edificio.shortestPathArest(div_inicial, div_final);
    }

    /**
     * Retorna um iterador para o caminho mais curto entre duas divisões.
     *
     * @param div_inicial Divisão inicial.
     * @param div_final Divisão final.
     * @return Iterador para o caminho mais curto.
     */
    @Override
    public Iterator<Divisao> shortesPathIt(Divisao div_inicial, Divisao div_final) {
        return this.planta_edificio.shortestPath(div_inicial, div_final);
    }

    /**
     * Adiciona uma divisão ao edifício.
     *
     * @param divisao A divisão a ser adicionada ao edifício.
     */
    @Override
    public void addDivisao(Divisao divisao) {
        this.planta_edificio.addVertex(divisao);
    }

    /**
     * Adiciona uma ligação entre duas divisões com um peso específico.
     *
     * @param vertex1 Primeira divisão.
     * @param vertex2 Segunda divisão.
     * @param weight Peso da ligação entre as divisões.
     */
    @Override
    public void addLigacao(Divisao vertex1, Divisao vertex2, double weight) {
        this.planta_edificio.addEdge(vertex1, vertex2, weight);
    }

    /**
     * Atualiza o peso de uma ligação no grafo.
     *
     * @param vertex1 A divisão cuja ligação será atualizada.
     * @param weight O novo peso da ligação.
     */
    @Override
    public void updateWeight(Divisao vertex1, double weight) {
        this.planta_edificio.updateWeightEdge(vertex1, weight);
    }

    /**
     * Retorna um iterador para as divisões adjacentes a uma divisão específica, usando BFS.
     *
     * @param divisao A divisão para a qual se deseja obter as divisões adjacentes.
     * @return Um iterador para as divisões adjacentes.
     */
    @Override
    public Iterator<Divisao> getNextDivisoes(Divisao divisao) {
        return this.planta_edificio.iteratorNextVertexs(divisao);
    }


    /**
     * Retorna um iterador para todas as divisões do edifício.
     *
     * @return Um iterador para todas as divisões.
     */
    @Override
    public Iterator<Divisao> IteratorMapa() {
        return this.planta_edificio.iterator();
    }

    /**
     * Procura uma divisão pelo nome.
     *
     * @param nome nome da divisão
     * @return a divisão, ou {@code null} se não existir
     */
    public Divisao getDivisao(String nome) {
        for (Divisao divisao : this.planta_edificio) {
            if (divisao.getName().equals(nome)) {
                return divisao;
            }
        }
        return null;
    }

    /**
     * Dano que o Tó Cruz sofre ao entrar numa divisão e eliminar os inimigos que lá estão.
     *
     * <p>Segue as regras do combate: o Tó Cruz ataca primeiro e tira {@link ToCruz#PODER} a cada
     * inimigo; os que sobrevivem atacam-no de volta. Repete-se até a divisão ficar limpa. Com
     * inimigos de 100 de vida são três ataques do Tó Cruz e duas respostas dos inimigos, por isso o
     * custo é o dobro do poder total. A versão anterior usava só a soma do poder, o que escolhia os
     * mesmos caminhos mas enganava na vida prevista para o modo automático.</p>
     *
     * @param divisao divisão
     * @return custo de entrada (0 se não houver inimigos)
     */
    public long custoEntrada(Divisao divisao) {
        int total = divisao.getInimigos().size();
        long[] vidas = new long[total];
        long[] poderes = new long[total];
        int i = 0;
        for (Inimigo inimigo : divisao.getInimigos()) {
            vidas[i] = inimigo.getVida();
            poderes[i] = inimigo.getPoder();
            i++;
        }

        long dano = 0;
        boolean haVivos = total > 0;
        while (haVivos) {
            haVivos = false;
            long resposta = 0;
            for (int j = 0; j < total; j++) {
                if (vidas[j] > 0) {
                    vidas[j] -= ToCruz.PODER;
                    if (vidas[j] > 0) {
                        haVivos = true;
                        resposta += poderes[j];
                    }
                }
            }
            dano += resposta;
        }
        return dano;
    }

    /**
     * Atualiza no grafo o custo de entrar numa divisão (chamar sempre que os inimigos dela mudam).
     *
     * @param divisao divisão cujos inimigos mudaram
     */
    public void atualizarCustoEntrada(Divisao divisao) {
        this.planta_edificio.updateWeightEdge(divisao, custoEntrada(divisao));
    }

    /**
     * Recalcula o custo de entrada de todas as divisões.
     */
    public void atualizarCustos() {
        for (Divisao divisao : this.planta_edificio) {
            atualizarCustoEntrada(divisao);
        }
    }

    /**
     * Divisões a, no máximo, {@code maxLigacoes} ligações de uma origem (incluindo a própria origem),
     * obtidas por uma pesquisa em largura (BFS) limitada.
     *
     * <p>Usa uma fila para a BFS e uma lista de acesso por índice para o resultado, porque quem a usa
     * (o movimento dos inimigos) precisa de escolher um elemento ao acaso.</p>
     *
     * @param origem divisão de partida
     * @param maxLigacoes distância máxima, em número de ligações
     * @return as divisões alcançáveis dentro dessa distância
     */
    public ArrayUnorderedADT<Divisao> divisoesAte(Divisao origem, int maxLigacoes) {
        ArrayUnorderedADT<Divisao> alcancadas = new ArrayUnordered<>();
        QueueADT<Divisao> fila = new LinkedQueue<>();
        QueueADT<Integer> distancias = new LinkedQueue<>();

        alcancadas.addToRear(origem);
        fila.enqueue(origem);
        distancias.enqueue(0);

        while (!fila.isEmpty()) {
            Divisao atual = fila.dequeue();
            int distancia = distancias.dequeue();

            if (distancia < maxLigacoes) {
                Iterator<Divisao> vizinhos = getNextDivisoes(atual);
                while (vizinhos.hasNext()) {
                    Divisao vizinho = vizinhos.next();
                    if (!alcancadas.contains(vizinho)) {
                        alcancadas.addToRear(vizinho);
                        fila.enqueue(vizinho);
                        distancias.enqueue(distancia + 1);
                    }
                }
            }
        }

        return alcancadas;
    }

    /**
     * Caminho com menos ligações entre duas divisões (pesquisa em largura), sem olhar aos inimigos.
     * É o caminho que um inimigo faz quando vai ao encontro do Tó Cruz.
     *
     * @param origem divisão de partida
     * @param destino divisão de chegada
     * @return as divisões do caminho, da origem ao destino (vazio se não houver caminho)
     */
    public ArrayUnorderedADT<Divisao> caminhoEmLigacoes(Divisao origem, Divisao destino) {
        ArrayUnorderedADT<Divisao> visitadas = new ArrayUnordered<>();
        ArrayUnorderedADT<Integer> anterior = new ArrayUnordered<>();
        QueueADT<Integer> fila = new LinkedQueue<>();
        visitadas.addToRear(origem);
        anterior.addToRear(-1);
        fila.enqueue(0);

        int encontrado = -1;
        while (!fila.isEmpty() && encontrado < 0) {
            int indice = fila.dequeue();
            Divisao atual = visitadas.find(indice);
            if (atual.equals(destino)) {
                encontrado = indice;
            } else {
                Iterator<Divisao> vizinhos = getNextDivisoes(atual);
                while (vizinhos.hasNext()) {
                    Divisao vizinho = vizinhos.next();
                    if (!visitadas.contains(vizinho)) {
                        visitadas.addToRear(vizinho);
                        anterior.addToRear(indice);
                        fila.enqueue(visitadas.size() - 1);
                    }
                }
            }
        }

        // O caminho reconstrói-se do destino para trás, inserindo cada divisão à frente.
        ArrayUnorderedADT<Divisao> caminho = new ArrayUnordered<>();
        for (int i = encontrado; i >= 0; i = anterior.find(i)) {
            caminho.addToFront(visitadas.find(i));
        }
        return caminho;
    }

    /**
     * Desenha o mapa do edifício, exibindo as suas divisões e conexões.
     */
    public void drawMapa() {
        Iterator<Divisao> itrDiv = this.planta_edificio.iterator();

        while (itrDiv.hasNext()) {
            Divisao div = itrDiv.next();
            System.out.println("Lista de Divisoes vizinhas a divisao: " + div.getName());
            System.out.println();

            Iterator<Divisao> itr_adj = this.planta_edificio.iteratorNextVertexs(div);

            while (itr_adj.hasNext()) {
                Divisao div_adj = itr_adj.next();
                System.out.println(div_adj.drawnDivisao());
            }

            System.out.println();
        }
    }

    /**
     * Retorna uma representação em string do edifício, incluindo o seu ID, nome e planta.
     *
     * @return String a representar o edifício.
     */
    @Override
    public String toString() {
        return "Edificio{" +
                "id=" + id +
                ", name='" + name +
                ", planta_edificio=" + planta_edificio.toString() +
                '}';
    }

    /**
     * Compara este edifício com outro objeto para verificar se são iguais.
     *
     * @param o O objeto a ser comparado.
     * @return true se os edifícios forem iguais, false caso contrário.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Edificio edificio = (Edificio) o;
        if (id == edificio.id) {
            return true;
        }

        return Objects.equals(name, edificio.name) && Objects.equals(planta_edificio, edificio.planta_edificio);
    }

    /**
     * Gera um código de hash para o edifício, utilizando o seu nome e planta.
     *
     * @return O código de hash do edifício.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
