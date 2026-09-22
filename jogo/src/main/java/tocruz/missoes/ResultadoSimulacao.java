package tocruz.missoes;

import ed.exceptions.EmptyCollectionException;
import ed.interfaces.QueueADT;
import tocruz.mapa.Divisao;

/**
 * Resultado de uma simulação manual, associado à versão da missão em que foi jogada.
 *
 * <p>Serve para listar, numa só lista ordenada, as simulações de todas as versões de uma missão:
 * primeiro as que terminaram com mais pontos de vida; em caso de empate, a versão mais antiga
 * e depois a simulação mais antiga.</p>
 *
 * @author Francisco Oliveira
 */
public class ResultadoSimulacao implements Comparable<ResultadoSimulacao> {

    private final long versaoMissao;
    private final Simulacoes simulacao;

    /**
     * @param versaoMissao versão da missão onde a simulação foi feita
     * @param simulacao a simulação
     */
    public ResultadoSimulacao(long versaoMissao, Simulacoes simulacao) {
        this.versaoMissao = versaoMissao;
        this.simulacao = simulacao;
    }

    /** @return versão da missão onde a simulação foi feita */
    public long getVersaoMissao() {
        return this.versaoMissao;
    }

    /** @return a simulação */
    public Simulacoes getSimulacao() {
        return this.simulacao;
    }

    @Override
    public int compareTo(ResultadoSimulacao o) {
        int porVida = Double.compare(o.simulacao.getVida_to(), this.simulacao.getVida_to());
        if (porVida != 0) {
            return porVida;
        }
        int porVersao = Long.compare(this.versaoMissao, o.versaoMissao);
        if (porVersao != 0) {
            return porVersao;
        }
        return Long.compare(this.simulacao.getVersao_simulacao(), o.simulacao.getVersao_simulacao());
    }

    /**
     * @return uma linha legível: versão, número da simulação, vida restante, inimigos eliminados
     *         e o trajeto percorrido
     */
    @Override
    public String toString() {
        return "Versão " + this.versaoMissao
                + " | simulação " + this.simulacao.getVersao_simulacao()
                + " | vida restante: " + (long) this.simulacao.getVida_to()
                + " | inimigos eliminados: " + this.simulacao.getInimigos_dead().size()
                + " | trajeto: " + descreverTrajeto(this.simulacao.getTrajeto_to());
    }

    /**
     * Lista os nomes das divisões da fila sem a alterar (cada elemento é retirado e volta a
     * entrar no fim, pela mesma ordem).
     */
    private static String descreverTrajeto(QueueADT<Divisao> trajeto) {
        if (trajeto == null || trajeto.isEmpty()) {
            return "(vazio)";
        }
        StringBuilder texto = new StringBuilder();
        int total = trajeto.size();
        try {
            for (int i = 0; i < total; i++) {
                Divisao divisao = trajeto.dequeue();
                if (i > 0) {
                    texto.append(" → ");
                }
                texto.append(divisao.getName());
                trajeto.enqueue(divisao);
            }
        } catch (EmptyCollectionException ex) {
            throw new IllegalStateException("O trajeto mudou enquanto era lido.", ex);
        }
        return texto.toString();
    }
}
