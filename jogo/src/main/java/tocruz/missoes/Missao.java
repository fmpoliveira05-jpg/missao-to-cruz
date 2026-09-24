package tocruz.missoes;

import tocruz.util.Consola;
import tocruz.interfaces.MissaoInt;
import ed.interfaces.OrderedListADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedOrderedList;
import ed.linkedlist.LinearLinkedUnorderedList;
import tocruz.mapa.Edificio;

import java.util.Objects;

/**
 * Representa uma missão no contexto do jogo, com informações como código, versão,
 * edifício associado e simulações realizadas. Permite executar modos de jogo manual, automático
 * e "jogo automático".
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 1.0
 */
public class Missao implements MissaoInt, Comparable<Missao> {
    /**
     * Código da missão.
     */
    private String cod_missao;

    /**
     * Versão da missão.
     */
    private long versao;

    /**
     * Edifício associado à missão.
     */
    private Edificio edificio;

    /**
     * Total de simulações realizadas nesta missão.
     */
    private int tot_simulacoes;

    /**
     * Lista ordenada de simulações realizadas.
     */
    private OrderedListADT<Simulacoes> simulacoes;

    /**
     * Construtor que inicializa a missão com os parâmetros fornecidos.
     *
     * @param cod_missao o código da missão
     * @param versao a versão da missão
     * @param edificio o edifício associado
     */
    public Missao(String cod_missao, long versao, Edificio edificio) {
        this.cod_missao = cod_missao;
        this.versao = versao;
        this.edificio = edificio;
        this.tot_simulacoes = 0;
        this.simulacoes = new LinearLinkedOrderedList<>();
    }

    /**
     * Construtor default que inicializa a missão com valores default.
     */
    public Missao() {
        this.cod_missao = "";
        this.versao = 0;
        this.edificio = new Edificio();
        this.simulacoes = new LinearLinkedOrderedList<>();
    }

    /**
     * Retorna o código da missão.
     *
     * @return o código da missão
     */
    public String getcod_missao() {
        return cod_missao;
    }

    /**
     * Retorna a versão da missão.
     *
     * @return a versão da missão
     */
    public long getVersao() {
        return versao;
    }

    /**
     * Retorna a lista de simulações realizadas nesta missão.
     *
     * @return a lista ordenada de simulações
     */
    public OrderedListADT<Simulacoes> getSimulacoes() {
        return simulacoes;
    }

    /**
     * Retorna o número total de simulações realizadas.
     *
     * @return o número total de simulações
     */
    public int getTot_simulacoes() {
        return tot_simulacoes;
    }

    /**
     * Retorna o edifício original da missão (as simulações trabalham sobre cópias).
     *
     * @return o edifício da missão.
     */
    public Edificio getEdificio() {
        return edificio;
    }

    /**
     * Exibe todas as simulações realizadas nesta missão.
     */
    @Override
    public void viewSimulacoes() {
        for (Simulacoes simulacao : simulacoes) {
            System.out.println(simulacao);
        }
    }

    /**
     * Modo manual: o utilizador joga simulações seguidas desta missão. Cada simulação começa
     * numa cópia do edifício, com os inimigos e itens na posição original.
     */
    @Override
    public void modoManual() {
        int op;
        do {
            System.out.println();
            System.out.println("Missão " + cod_missao + " (versão " + versao + ")");
            System.out.println("  1 - Jogar uma simulação");
            System.out.println("  0 - Voltar");
            op = Consola.lerInteiro("Selecione uma opção --> ", 0, 1);

            if (op == 1) {
                tot_simulacoes++;
                Simulacoes simula = new Simulacoes(tot_simulacoes, copiaDoEdificio());
                simulacoes.add(simula.modojogoManual());
            }
        } while (op != 0);

        if (!simulacoes.isEmpty()) {
            System.out.println("Simulações desta missão (da maior para a menor vida final):");
            for (Simulacoes simulacao : simulacoes) {
                System.out.println("  " + simulacao);
            }
        }
    }

    /** Cópia do edifício, para cada simulação começar do estado original da missão. */
    private Edificio copiaDoEdificio() {
        return new Edificio(this.edificio.getId(), this.edificio.getName(), this.edificio.getPlantaEdificio());
    }

    /**
     * Executa o modo automático da missão, mostrando o caminho mais curto para o ToCruz entrar no
     * edifício e coletar o alvo, e o melhor caminho que ele deve fazer, se possível,
     * para sair do edifício com o alvo.
     */
    @Override
    public void modoAutomatico() {
        new Simulacoes(0, copiaDoEdificio()).modojogoAutomatico();
    }

    /**
     * Executa um jogo automático nesta missão, ou seja, o jogador não interage para fazer
     * uma determinada ação.
     */
    @Override
    public void jogoAutomatico() {
        new Simulacoes(0, copiaDoEdificio()).jogoAutomatico();
    }

    /**
     * Compara esta missão com outra com base no código e na versão.
     *
     * @param o a outra missão a ser comparada
     * @return 1 se esta missão for menor, -1 se for maior, ou 0 se forem iguais
     */
    @Override
    public int compareTo(Missao o) {
        int value = 0;
        if (this.cod_missao.compareTo(o.cod_missao) < 0) {
            value = 1;
        } else if (this.cod_missao.compareTo(o.cod_missao) > 0) {
            value = -1;
        } else {
            if (this.versao < o.versao) {
                value = 1;
            } else {
                value = -1;
            }
        }

        return value;
    }

    /**
     * Verifica a igualdade entre dois objetos Missao.
     * A comparação é feita pelo código da missão e a versão.
     *
     * @param o o objeto a ser comparado com esta Missao
     * @return true se os objetos forem iguais, false caso contrário
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Missao missao = (Missao) o;

        return this.cod_missao.equals(((Missao) o).cod_missao) && this.versao == ((Missao) o).versao;
    }

    /**
     * Calcula o código de hash para a Missao com base no código e na versão.
     *
     * @return o código de hash calculado
     */
    @Override
    public int hashCode() {
        return Objects.hash(cod_missao, versao);
    }
}
