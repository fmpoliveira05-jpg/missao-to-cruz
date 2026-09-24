package tocruz.motor;

import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import java.util.Iterator;
import java.util.Random;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.personagens.Inimigo;

/**
 * Fase de movimento dos inimigos.
 *
 * <p>Regra do enunciado: a cada iteração os inimigos mudam de divisão ao acaso, até duas divisões a
 * partir da posição em que estão, sem nunca se afastarem mais de duas ligações da divisão que
 * protegem. Os inimigos que estão em combate com o Tó Cruz não se movem.</p>
 *
 * <p>Estruturas usadas: as divisões candidatas vêm de uma BFS limitada ({@link Edificio#divisoesAte})
 * e ficam numa lista com acesso por índice, para se poder escolher uma ao acaso em O(1). A versão
 * anterior empilhava os vizinhos numa pilha e desempilhava até chegar à posição sorteada, e movia
 * os inimigos sem limite de distância à divisão que protegiam.</p>
 *
 * @author Francisco Oliveira
 */
public class MovimentoInimigos {

    /**
     * Distância máxima, em ligações, de cada movimento e da divisão protegida.
     */
    public static final int DISTANCIA_MAXIMA = 2;

    private final Random aleatorio;

    /**
     * @param aleatorio gerador de números aleatórios (um gerador com semente torna o jogo reproduzível)
     */
    public MovimentoInimigos(Random aleatorio) {
        this.aleatorio = aleatorio;
    }

    /**
     * Move todos os inimigos que não estão em combate.
     *
     * @param edificio edifício da simulação
     * @return frases a descrever os movimentos, para o relatório do turno
     */
    public UnorderedListADT<String> moverInimigos(Edificio edificio) {
        UnorderedListADT<String> relato = new LinearLinkedUnorderedList<>();

        // Primeiro recolhem-se todos os inimigos a mover; se fossem movidos durante a iteração, um
        // inimigo que chegasse a uma divisão ainda por visitar mover-se-ia duas vezes no mesmo turno.
        UnorderedListADT<Inimigo> aMover = new LinearLinkedUnorderedList<>();
        UnorderedListADT<Divisao> origens = new LinearLinkedUnorderedList<>();
        Iterator<Divisao> divisoes = edificio.IteratorMapa();
        while (divisoes.hasNext()) {
            Divisao divisao = divisoes.next();
            if (!divisao.haveConfronto()) {
                for (Inimigo inimigo : divisao.getInimigos()) {
                    aMover.addToRear(inimigo);
                    origens.addToRear(divisao);
                }
            }
        }

        while (!aMover.isEmpty()) {
            Inimigo inimigo = aMover.removeFirst();
            Divisao origem = origens.removeFirst();
            Divisao destino = escolherDestino(edificio, origem, inimigo);

            if (!destino.equals(origem)) {
                origem.removeInimigo(inimigo);
                destino.addInimigo(inimigo);
                edificio.atualizarCustoEntrada(origem);
                edificio.atualizarCustoEntrada(destino);
                relato.addToRear(inimigo.getNome() + " passou de " + origem.getName() + " para " + destino.getName() + ".");
            }
        }

        return relato;
    }

    /**
     * Escolhe ao acaso uma divisão a até duas ligações da posição atual que também esteja a até
     * duas ligações da divisão protegida. Ficar onde está é uma das opções.
     */
    private Divisao escolherDestino(Edificio edificio, Divisao origem, Inimigo inimigo) {
        ArrayUnorderedADT<Divisao> candidatas = edificio.divisoesAte(origem, DISTANCIA_MAXIMA);
        Divisao guardada = inimigo.getDivisaoGuardada() == null ? null : edificio.getDivisao(inimigo.getDivisaoGuardada());

        if (guardada != null) {
            ArrayUnorderedADT<Divisao> zona = edificio.divisoesAte(guardada, DISTANCIA_MAXIMA);
            UnorderedListADT<Divisao> foraDaZona = new LinearLinkedUnorderedList<>();
            for (Divisao candidata : candidatas) {
                if (!zona.contains(candidata)) {
                    foraDaZona.addToRear(candidata);
                }
            }
            while (!foraDaZona.isEmpty()) {
                candidatas.remove(foraDaZona.removeFirst());
            }
        }

        if (candidatas.isEmpty()) {
            return origem;
        }
        return candidatas.find(aleatorio.nextInt(candidatas.size()));
    }
}
