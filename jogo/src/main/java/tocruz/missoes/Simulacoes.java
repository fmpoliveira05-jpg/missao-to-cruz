package tocruz.missoes;

import ed.arraylist.ArrayUnordered;
import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.QueueADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import ed.queue.LinkedQueue;
import java.util.Objects;
import java.util.Random;
import tocruz.interfaces.SimulacoesInt;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.motor.JogadorAutomatico;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;
import tocruz.motor.Plano;
import tocruz.personagens.Inimigo;
import tocruz.personagens.ToCruz;
import tocruz.util.Consola;

/**
 * Uma simulação de uma missão, jogada na consola.
 *
 * <p>As regras do jogo estão em {@link MotorJogo}; esta classe só pergunta ao jogador o que quer
 * fazer, mostra o que aconteceu e guarda o resultado (percurso, inimigos eliminados e vida final).
 * Antes, as regras e as perguntas na consola estavam misturadas num único ficheiro de 1200 linhas,
 * o que impedia testar as regras e reaproveitá-las na interface gráfica.</p>
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 2.0
 */
public class Simulacoes implements SimulacoesInt, Comparable<Simulacoes> {

    /** Número da simulação dentro da missão. */
    private long versao_simulacao;

    /** Divisões por onde o Tó Cruz passou, pela ordem (fila: a primeira a entrar é a primeira a sair). */
    private QueueADT<Divisao> trajeto_to;

    /** Inimigos eliminados durante a simulação. */
    private UnorderedListADT<Inimigo> inimigos_dead;

    /** Vida do Tó Cruz no fim da simulação. */
    private long vida_to;

    /** Se o Tó Cruz saiu vivo e com o alvo. */
    private boolean sucesso;

    /** Edifício da simulação (uma cópia do edifício da missão). */
    private Edificio edificio;

    /** Gerador usado no movimento dos inimigos. */
    private final Random aleatorio;

    /**
     * @param versao_simulacao número da simulação
     * @param edificio cópia do edifício da missão
     */
    public Simulacoes(long versao_simulacao, Edificio edificio) {
        this(versao_simulacao, edificio, new Random());
    }

    /**
     * @param versao_simulacao número da simulação
     * @param edificio cópia do edifício da missão
     * @param aleatorio gerador para o movimento dos inimigos (com semente, a simulação é reproduzível)
     */
    public Simulacoes(long versao_simulacao, Edificio edificio, Random aleatorio) {
        this.edificio = edificio;
        this.versao_simulacao = versao_simulacao;
        this.vida_to = 0;
        this.trajeto_to = new LinkedQueue<>();
        this.inimigos_dead = new LinearLinkedUnorderedList<>();
        this.aleatorio = aleatorio;
    }

    /** @return número da simulação */
    public long getVersao_simulacao() {
        return versao_simulacao;
    }

    /** @return percurso do Tó Cruz */
    public QueueADT<Divisao> getTrajeto_to() {
        return trajeto_to;
    }

    /** @return inimigos eliminados */
    public UnorderedListADT<Inimigo> getInimigos_dead() {
        return inimigos_dead;
    }

    /** @return vida do Tó Cruz no fim da simulação */
    public double getVida_to() {
        return vida_to;
    }

    /** @return se a missão foi cumprida */
    public boolean isSucesso() {
        return sucesso;
    }

    /**
     * Acrescenta uma divisão ao percurso.
     *
     * @param divisao divisão visitada
     */
    public void addDivisaoTrajetoToCruz(Divisao divisao) {
        this.trajeto_to.enqueue(divisao);
    }

    // ------------------------------------------------------------------ modos de jogo

    /**
     * Modo manual: o jogador escolhe a entrada e depois, em cada turno, a ação do Tó Cruz. Em cada
     * turno são mostrados o melhor caminho para o objetivo e para o kit mais próximo.
     *
     * @return esta simulação, já com o resultado
     */
    @Override
    public Simulacoes modojogoManual() {
        MotorJogo motor = new MotorJogo(edificio, aleatorio);

        ArrayUnorderedADT<Divisao> entradas = motor.getEntradas();
        System.out.println("Por onde entra o Tó Cruz?");
        for (int i = 0; i < entradas.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + entradas.find(i).getName());
        }
        int escolha = Consola.lerInteiro("Entrada --> ", 1, entradas.size());
        motor.entrar(entradas.find(escolha - 1));
        mostrarRelato(motor);

        while (motor.getEstado() != MotorJogo.Estado.TERMINADO) {
            if (motor.getEstado() == MotorJogo.Estado.DECIDIR_KIT) {
                perguntarKit(motor);
            } else {
                mostrarEstado(motor);
                perguntarAcao(motor);
            }
            mostrarRelato(motor);
        }

        guardarResultado(motor);
        System.out.println(relatorio());
        return this;
    }

    /**
     * Modo automático: mostra a melhor entrada, o caminho até ao alvo e o caminho de regresso,
     * com a vida prevista em cada parte.
     */
    @Override
    public void modojogoAutomatico() {
        try {
            System.out.println(new PlaneadorMissao().planear(edificio).descrever());
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Jogo automático: o Tó Cruz entra pela entrada do plano e joga sozinho, turno a turno, com os
     * inimigos a moverem-se como no modo manual.
     *
     * @return esta simulação, já com o resultado
     */
    @Override
    public Simulacoes jogoAutomatico() {
        Plano plano = new PlaneadorMissao().planear(edificio);
        MotorJogo motor = new MotorJogo(edificio, aleatorio);
        JogadorAutomatico jogador = new JogadorAutomatico();

        motor.entrar(plano.getEntrada());
        mostrarRelato(motor);
        while (motor.getEstado() != MotorJogo.Estado.TERMINADO && motor.getTurno() < JogadorAutomatico.LIMITE_TURNOS) {
            jogador.jogarTurno(motor);
            mostrarRelato(motor);
        }

        guardarResultado(motor);
        System.out.println(relatorio());
        return this;
    }

    // ------------------------------------------------------------------ consola

    private void mostrarEstado(MotorJogo motor) {
        ToCruz to = motor.getToCruz();
        System.out.println();
        System.out.println("Turno " + motor.getTurno() + " | " + motor.getDivisaoAtual().getName()
                + " | vida " + to.getVida() + " | kits " + to.getMochila().size() + "/" + PlaneadorMissao.CAPACIDADE_MOCHILA
                + " | alvo " + (to.isColectedAlvo() ? "apanhado" : "por apanhar"));

        if (motor.emCombate()) {
            StringBuilder texto = new StringBuilder("Em combate com:");
            for (Inimigo inimigo : motor.getDivisaoAtual().getInimigos()) {
                texto.append(' ').append(inimigo.getNome()).append(" (vida ").append(inimigo.getVida())
                        .append(", poder ").append(inimigo.getPoder()).append(')');
            }
            System.out.println(texto);
            return;
        }

        String objetivo = to.isColectedAlvo() ? "a saída" : "o alvo";
        System.out.println("Melhor caminho para " + objetivo + ": " + juntar(motor.caminhoSugerido()));
        ArrayUnorderedADT<Divisao> kit = motor.caminhoParaKit();
        if (!kit.isEmpty()) {
            System.out.println("Kit de vida mais próximo: " + juntar(kit));
        }
    }

    private void perguntarAcao(MotorJogo motor) {
        ArrayUnorderedADT<String> rotulos = new ArrayUnordered<>();
        ArrayUnorderedADT<Runnable> acoes = new ArrayUnordered<>();

        if (motor.emCombate()) {
            rotulos.addToRear("Atacar");
            acoes.addToRear(motor::atacar);
        } else {
            for (Divisao destino : motor.getDestinosPossiveis()) {
                rotulos.addToRear("Ir para " + destino.getName() + descreverDivisao(destino));
                acoes.addToRear(() -> motor.mover(destino));
            }
            rotulos.addToRear("Ficar onde está");
            acoes.addToRear(motor::aguardar);
        }
        if (motor.podeUsarKit()) {
            rotulos.addToRear("Usar kit da mochila");
            acoes.addToRear(motor::usarKit);
        }
        if (motor.podeSair()) {
            rotulos.addToRear(motor.getToCruz().isColectedAlvo() ? "Sair do edifício com o alvo" : "Sair do edifício (sem o alvo)");
            acoes.addToRear(motor::sair);
        }

        for (int i = 0; i < rotulos.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + rotulos.find(i));
        }
        int escolha = Consola.lerInteiro("Ação --> ", 1, rotulos.size());
        acoes.find(escolha - 1).run();
    }

    private void perguntarKit(MotorJogo motor) {
        ItemCura kit = motor.getKitPorDecidir();
        System.out.println("Kit de " + kit.getVida_recuperada() + " pontos. Vida atual: " + motor.getToCruz().getVida() + ".");

        ArrayUnorderedADT<MotorJogo.DecisaoKit> opcoes = new ArrayUnordered<>();
        for (MotorJogo.DecisaoKit decisao : MotorJogo.DecisaoKit.values()) {
            if (motor.podeDecidir(decisao)) {
                opcoes.addToRear(decisao);
            }
        }
        for (int i = 0; i < opcoes.size(); i++) {
            String texto;
            switch (opcoes.find(i)) {
                case USAR:
                    texto = "Usar já";
                    break;
                case GUARDAR:
                    texto = "Guardar na mochila";
                    break;
                default:
                    texto = "Deixar na divisão";
                    break;
            }
            System.out.println("  " + (i + 1) + " - " + texto);
        }
        int escolha = Consola.lerInteiro("Kit --> ", 1, opcoes.size());
        motor.decidirKit(opcoes.find(escolha - 1));
    }

    private static String descreverDivisao(Divisao divisao) {
        StringBuilder texto = new StringBuilder();
        long poder = 0;
        for (Inimigo inimigo : divisao.getInimigos()) {
            poder += inimigo.getPoder();
        }
        if (poder > 0) {
            texto.append(" [inimigos, poder ").append(poder).append(']');
        }
        if (divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected()) {
            ItemCura item = (ItemCura) divisao.getItem();
            texto.append(" [").append(item.getType() == TypeItemCura.COLETE ? "colete" : "kit")
                    .append(" +").append(item.getVida_recuperada()).append(']');
        }
        if (divisao.getAlvo() != null && !divisao.getAlvo().isAtinigido()) {
            texto.append(" [alvo]");
        }
        if (divisao.isEntrada_saida()) {
            texto.append(" [saída]");
        }
        return texto.toString();
    }

    private static void mostrarRelato(MotorJogo motor) {
        for (String frase : motor.consumirRelato()) {
            System.out.println("  · " + frase);
        }
    }

    private static String juntar(ArrayUnorderedADT<Divisao> caminho) {
        if (caminho.isEmpty()) {
            return "(sem caminho)";
        }
        StringBuilder texto = new StringBuilder();
        for (Divisao divisao : caminho) {
            if (texto.length() > 0) {
                texto.append(" → ");
            }
            texto.append(divisao.getName());
        }
        return texto.toString();
    }

    // ------------------------------------------------------------------ resultado

    private void guardarResultado(MotorJogo motor) {
        this.trajeto_to = motor.getTrajeto();
        this.inimigos_dead = motor.getInimigosMortos();
        this.vida_to = Math.max(0, motor.getToCruz().getVida());
        this.sucesso = motor.isSucesso();
    }

    /**
     * @return resumo da simulação: resultado, vida, inimigos eliminados e percurso
     */
    public String relatorio() {
        StringBuilder texto = new StringBuilder();
        texto.append("\n=== Fim da simulação ").append(versao_simulacao).append(" ===\n");
        texto.append(sucesso ? "Missão cumprida" : "Missão falhada").append(" | vida final: ").append(vida_to).append('\n');
        texto.append("Inimigos eliminados: ").append(inimigos_dead.size());
        if (!inimigos_dead.isEmpty()) {
            StringBuilder nomes = new StringBuilder();
            for (Inimigo inimigo : inimigos_dead) {
                nomes.append(nomes.length() == 0 ? "" : ", ").append(inimigo.getNome());
            }
            texto.append(" (").append(nomes).append(')');
        }
        texto.append('\n').append("Percurso: ").append(percurso());
        return texto.toString();
    }

    /** Lê a fila do percurso sem a alterar (cada divisão sai e volta a entrar no fim). */
    private String percurso() {
        StringBuilder texto = new StringBuilder();
        int total = trajeto_to.size();
        for (int i = 0; i < total; i++) {
            Divisao divisao = trajeto_to.dequeue();
            texto.append(i == 0 ? "" : " → ").append(divisao.getName());
            trajeto_to.enqueue(divisao);
        }
        return total == 0 ? "(vazio)" : texto.toString();
    }

    @Override
    public String toString() {
        return "Simulação " + versao_simulacao + " | " + (sucesso ? "cumprida" : "falhada")
                + " | vida " + vida_to + " | inimigos eliminados " + inimigos_dead.size()
                + " | percurso: " + percurso();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Simulacoes that = (Simulacoes) o;
        return versao_simulacao == that.versao_simulacao && vida_to == that.vida_to
                && Objects.equals(edificio, that.edificio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versao_simulacao, vida_to, edificio);
    }

    /**
     * Ordena da maior para a menor vida final; em caso de empate, a simulação mais recente primeiro.
     *
     * @param o outra simulação
     * @return negativo se esta vier antes
     */
    @Override
    public int compareTo(Simulacoes o) {
        if (this.vida_to != o.vida_to) {
            return this.vida_to < o.vida_to ? 1 : -1;
        }
        return Long.compare(o.versao_simulacao, this.versao_simulacao);
    }
}
