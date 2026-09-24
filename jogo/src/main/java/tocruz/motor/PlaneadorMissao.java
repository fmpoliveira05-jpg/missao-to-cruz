package tocruz.motor;

import ed.arraylist.ArrayUnordered;
import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.StackADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import ed.stacks.LinkedStack;
import java.util.Iterator;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;

/**
 * Modo automático: escolhe a entrada, o caminho até ao alvo e o caminho de volta que deixam o Tó
 * Cruz com mais pontos de vida.
 *
 * <p>Cada divisão tem como custo de entrada o dano que o Tó Cruz sofre a eliminar os inimigos que
 * lá estão, por isso o caminho mais seguro entre duas divisões é o caminho de menor custo no grafo
 * (Dijkstra). O planeador experimenta todas as combinações entrada → alvo → saída (o regresso já
 * conta com as divisões limpas na ida) e simula a vida ao longo do percurso, somando coletes e
 * usando os kits de vida do caminho. Os inimigos são considerados nas
 * posições atuais: como se movem ao acaso, o plano é uma previsão e não uma garantia.</p>
 *
 * @author Francisco Oliveira
 */
public class PlaneadorMissao {

    /** Vida do Tó Cruz no início da missão. */
    public static final long VIDA_INICIAL = 100;

    /** Número máximo de kits na mochila. */
    public static final int CAPACIDADE_MOCHILA = 3;

    /**
     * Calcula o melhor plano para o estado atual do edifício.
     *
     * @param edificio edifício da missão
     * @return o plano com mais vida no fim (ou, se nenhum for viável, o que chega ao alvo com mais vida)
     * @throws IllegalStateException se o edifício não tiver alvo ou entradas
     */
    public Plano planear(Edificio edificio) {
        UnorderedListADT<Divisao> entradas = new LinearLinkedUnorderedList<>();
        Divisao alvo = null;
        Iterator<Divisao> divisoes = edificio.IteratorMapa();
        while (divisoes.hasNext()) {
            Divisao divisao = divisoes.next();
            if (divisao.isEntrada_saida()) {
                entradas.addToRear(divisao);
            }
            if (divisao.getAlvo() != null) {
                alvo = divisao;
            }
        }
        if (alvo == null || entradas.isEmpty()) {
            throw new IllegalStateException("O edifício precisa de um alvo e de pelo menos uma entrada.");
        }

        Plano melhor = null;
        for (Divisao entrada : entradas) {
            ArrayUnorderedADT<Divisao> ida = caminho(edificio, entrada, alvo);
            if (ida.isEmpty()) {
                continue;
            }
            // No regresso, as divisões limpas na ida já não custam nada: o caminho de volta é
            // calculado numa cópia do edifício sem os inimigos que o Tó Cruz eliminou.
            Edificio depoisDaIda = semInimigos(edificio, ida);
            Divisao alvoNaCopia = depoisDaIda.getDivisao(alvo.getName());
            for (Divisao saida : entradas) {
                ArrayUnorderedADT<Divisao> volta = caminho(depoisDaIda, alvoNaCopia, depoisDaIda.getDivisao(saida.getName()));
                if (!volta.isEmpty()) {
                    Plano candidato = simular(edificio, ida, volta);
                    if (melhor == null || melhorQue(candidato, melhor)) {
                        melhor = candidato;
                    }
                }
            }
        }

        if (melhor == null) {
            throw new IllegalStateException("Não existe caminho entre as entradas e o alvo.");
        }
        return melhor;
    }

    /**
     * Caminho de menor dano entre duas divisões.
     *
     * @param edificio edifício
     * @param origem divisão de partida
     * @param destino divisão de chegada
     * @return as divisões do caminho (vazio se não houver caminho)
     */
    public static ArrayUnorderedADT<Divisao> caminho(Edificio edificio, Divisao origem, Divisao destino) {
        ArrayUnorderedADT<Divisao> caminho = new ArrayUnordered<>();
        Iterator<Divisao> passos = edificio.shortesPathIt(origem, destino);
        while (passos.hasNext()) {
            caminho.addToRear(passos.next());
        }
        return caminho;
    }

    /**
     * Cópia do edifício em que as divisões de um caminho ficaram sem inimigos.
     */
    private static Edificio semInimigos(Edificio edificio, ArrayUnorderedADT<Divisao> caminho) {
        Edificio copia = new Edificio(edificio.getId(), edificio.getName(), edificio.getPlantaEdificio());
        for (Divisao passo : caminho) {
            Divisao divisao = copia.getDivisao(passo.getName());
            while (divisao.haveInimigo()) {
                divisao.removeInimigo(divisao.getInimigos().first());
            }
            copia.atualizarCustoEntrada(divisao);
        }
        return copia;
    }

    private static boolean melhorQue(Plano a, Plano b) {
        if (a.isViavel() != b.isViavel()) {
            return a.isViavel();
        }
        if (a.getVidaFinal() != b.getVidaFinal()) {
            return a.getVidaFinal() > b.getVidaFinal();
        }
        if (a.getVidaNoAlvo() != b.getVidaNoAlvo()) {
            return a.getVidaNoAlvo() > b.getVidaNoAlvo();
        }
        // Em caso de empate, o percurso mais curto.
        return a.getCaminhoIda().size() + a.getCaminhoVolta().size()
                < b.getCaminhoIda().size() + b.getCaminhoVolta().size();
    }

    /**
     * Percorre o caminho completo e calcula a vida em cada ponto: cada divisão com inimigos tira o
     * dano do combate ({@link Edificio#custoEntrada}) e fica limpa; os coletes somam logo; os kits
     * são usados se a vida não estiver cheia, ou guardados na mochila (pilha) e usados quando a
     * próxima divisão seria fatal.
     */
    private Plano simular(Edificio edificio, ArrayUnorderedADT<Divisao> ida, ArrayUnorderedADT<Divisao> volta) {
        long vida = VIDA_INICIAL;
        long danoIda = 0;
        long danoVolta = 0;
        long vidaNoAlvo = 0;
        StackADT<Long> mochila = new LinkedStack<>();
        UnorderedListADT<Divisao> visitadas = new LinearLinkedUnorderedList<>();

        int total = ida.size() + volta.size() - 1;
        for (int i = 0; i < total && vida > 0; i++) {
            boolean naIda = i < ida.size();
            Divisao divisao = naIda ? ida.find(i) : volta.find(i - ida.size() + 1);
            boolean jaVisitada = visitadas.contains(divisao);
            long custo = jaVisitada ? 0 : edificio.custoEntrada(divisao);

            while (vida - custo <= 0 && !mochila.isEmpty()) {
                vida = Math.min(VIDA_INICIAL, vida + mochila.pop());
            }
            vida -= custo;
            if (naIda) {
                danoIda += custo;
            } else {
                danoVolta += custo;
            }

            if (vida > 0 && !jaVisitada && divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected()) {
                ItemCura item = (ItemCura) divisao.getItem();
                if (item.getType() == TypeItemCura.COLETE) {
                    vida += item.getVida_recuperada();
                } else if (vida < VIDA_INICIAL) {
                    vida = Math.min(VIDA_INICIAL, vida + item.getVida_recuperada());
                } else if (mochila.size() < CAPACIDADE_MOCHILA) {
                    mochila.push(item.getVida_recuperada());
                }
            }
            if (!jaVisitada) {
                visitadas.addToRear(divisao);
            }

            if (i == ida.size() - 1) {
                vidaNoAlvo = Math.max(vida, 0);
            }
        }

        return new Plano(ida, volta, danoIda, danoVolta, vidaNoAlvo, Math.max(vida, 0));
    }
}
