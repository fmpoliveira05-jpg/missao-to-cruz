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
 * <p>Regra do enunciado (níveis Recruta e Agente): cada inimigo que não está em combate escolhe ao
 * acaso uma divisão a até {@code passos} ligações da posição atual que também esteja a até duas
 * ligações da divisão que guarda. Ficar onde está é uma das opções.</p>
 *
 * <p>No nível Elite, um inimigo que esteja a {@link Dificuldade#RAIO_DETECAO} ligações ou menos do
 * Tó Cruz (ou a {@link Dificuldade#RAIO_DETECAO_ALARME} durante o alarme) ouve-o e avança uma
 * divisão pelo caminho com menos ligações até ele. Sem alarme, não se afasta mais de
 * {@link #DISTANCIA_PERSEGUICAO} ligações da sala que guarda; com o alarme, persegue-o por todo o
 * edifício. Quando deixa de o ouvir, regressa à sua zona.</p>
 *
 * @author Francisco Oliveira
 */
public class MovimentoInimigos {

    /** Distância máxima (em ligações) entre um inimigo e a divisão que guarda, numa patrulha. */
    public static final int DISTANCIA_MAXIMA = 2;

    /** Distância máxima à sala que guarda até onde um inimigo persegue o Tó Cruz (sem alarme). */
    public static final int DISTANCIA_PERSEGUICAO = 3;

    /** Divisões que um inimigo avança por turno quando vai ao encontro do Tó Cruz (avança com cautela). */
    public static final int PASSOS_PERSEGUICAO = 1;

    private final Random aleatorio;
    private final Dificuldade dificuldade;

    /**
     * Movimento com as regras do enunciado (nível Agente).
     *
     * @param aleatorio gerador de números aleatórios (um gerador com semente torna o jogo reproduzível)
     */
    public MovimentoInimigos(Random aleatorio) {
        this(aleatorio, Dificuldade.AGENTE);
    }

    /**
     * @param aleatorio gerador de números aleatórios
     * @param dificuldade nível que define quantas divisões os inimigos andam e se perseguem o Tó Cruz
     */
    public MovimentoInimigos(Random aleatorio, Dificuldade dificuldade) {
        this.aleatorio = aleatorio;
        this.dificuldade = dificuldade;
    }

    /**
     * Move todos os inimigos que não estão em combate, sem saber onde está o Tó Cruz.
     *
     * @param edificio edifício da simulação
     * @return um evento por inimigo que mudou de divisão
     */
    public UnorderedListADT<Evento> moverInimigos(Edificio edificio) {
        return moverInimigos(edificio, null, false);
    }

    /**
     * Move todos os inimigos que não estão em combate.
     *
     * @param edificio edifício da simulação
     * @param posicaoToCruz divisão onde está o Tó Cruz (pode ser {@code null})
     * @param alarme se o alarme está a tocar (aumenta a distância a que o ouvem)
     * @return um evento por inimigo que mudou de divisão
     */
    public UnorderedListADT<Evento> moverInimigos(Edificio edificio, Divisao posicaoToCruz, boolean alarme) {
        UnorderedListADT<Evento> eventos = new LinearLinkedUnorderedList<>();

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

        int raio = alarme ? Dificuldade.RAIO_DETECAO_ALARME : Dificuldade.RAIO_DETECAO;
        while (!aMover.isEmpty()) {
            Inimigo inimigo = aMover.removeFirst();
            Divisao origem = origens.removeFirst();

            ArrayUnorderedADT<Divisao> caminho = null;
            boolean persegue = false;
            if (dificuldade.temPerseguicao() && posicaoToCruz != null) {
                ArrayUnorderedADT<Divisao> ateToCruz = edificio.caminhoEmLigacoes(origem, posicaoToCruz);
                int distancia = ateToCruz.size() - 1;
                if (distancia >= 1 && distancia <= raio) {
                    ArrayUnorderedADT<Divisao> passo = inicio(ateToCruz, PASSOS_PERSEGUICAO);
                    // Sem alarme, um guarda não abandona o seu setor: persegue só até
                    // DISTANCIA_PERSEGUICAO ligações da sala que guarda.
                    if (alarme || dentroDoSetor(edificio, passo.last(), inimigo)) {
                        caminho = passo;
                        persegue = true;
                    }
                }
            }
            if (caminho == null) {
                caminho = regressoAoPosto(edificio, origem, inimigo);
            }
            if (caminho == null) {
                Divisao destino = escolherDestino(edificio, origem, inimigo);
                caminho = edificio.caminhoEmLigacoes(origem, destino);
            }

            Divisao destino = caminho.last();
            if (!destino.equals(origem)) {
                origem.removeInimigo(inimigo);
                destino.addInimigo(inimigo);
                edificio.atualizarCustoEntrada(origem);
                edificio.atualizarCustoEntrada(destino);

                String[] nomes = new String[caminho.size()];
                for (int i = 0; i < nomes.length; i++) {
                    nomes[i] = caminho.find(i).getName();
                }
                eventos.addToRear(new Evento(persegue ? Evento.Tipo.PERSEGUICAO : Evento.Tipo.MOVIMENTO_INIMIGO,
                        inimigo.getNome(), origem.getName(), destino.getName(), nomes, 0, 0));
            }
        }

        return eventos;
    }

    /**
     * Um inimigo que se afastou da zona que guarda (a perseguir o Tó Cruz) volta para lá.
     *
     * @return o caminho de regresso, ou {@code null} se o inimigo já estiver na sua zona
     */
    private ArrayUnorderedADT<Divisao> regressoAoPosto(Edificio edificio, Divisao origem, Inimigo inimigo) {
        Divisao guardada = inimigo.getDivisaoGuardada() == null ? null : edificio.getDivisao(inimigo.getDivisaoGuardada());
        if (guardada == null || edificio.divisoesAte(guardada, DISTANCIA_MAXIMA).contains(origem)) {
            return null;
        }
        return inicio(edificio.caminhoEmLigacoes(origem, guardada), dificuldade.getPassosInimigos());
    }

    private static boolean dentroDoSetor(Edificio edificio, Divisao divisao, Inimigo inimigo) {
        Divisao guardada = inimigo.getDivisaoGuardada() == null ? null : edificio.getDivisao(inimigo.getDivisaoGuardada());
        return guardada == null || edificio.divisoesAte(guardada, DISTANCIA_PERSEGUICAO).contains(divisao);
    }

    /** As primeiras {@code passos} ligações de um caminho. */
    private static ArrayUnorderedADT<Divisao> inicio(ArrayUnorderedADT<Divisao> caminho, int passos) {
        ArrayUnorderedADT<Divisao> parte = new ed.arraylist.ArrayUnordered<>();
        for (int i = 0; i < caminho.size() && i <= passos; i++) {
            parte.addToRear(caminho.find(i));
        }
        return parte;
    }

    /**
     * Escolhe ao acaso uma divisão a até {@code passos} ligações da posição atual que também esteja
     * a até duas ligações da divisão protegida. Ficar onde está é uma das opções.
     */
    private Divisao escolherDestino(Edificio edificio, Divisao origem, Inimigo inimigo) {
        ArrayUnorderedADT<Divisao> candidatas = edificio.divisoesAte(origem, dificuldade.getPassosInimigos());
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
