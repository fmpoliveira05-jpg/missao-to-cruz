package tocruz.motor;

/**
 * Níveis de dificuldade da simulação.
 *
 * <ul>
 *   <li><b>Recruta</b> – os inimigos têm metade do poder e só andam uma divisão por turno.</li>
 *   <li><b>Agente</b> – as regras exatas do enunciado: os inimigos andam até duas divisões, ao
 *   acaso, sem se afastarem mais de duas divisões da sala que guardam.</li>
 *   <li><b>Elite</b> – os inimigos são 30% mais fortes, ouvem o Tó Cruz quando ele está a duas
 *   divisões ou menos e avançam uma divisão por turno ao encontro dele. Quem chega à divisão do
 *   Tó Cruz cai numa emboscada (ele ouviu-o chegar e dispara primeiro). Ao apanhar o alvo dispara
 *   um alarme: há um número limitado de turnos para sair e os inimigos ouvem-no a três divisões,
 *   por todo o edifício.</li>
 * </ul>
 *
 * @author Francisco Oliveira
 */
public enum Dificuldade {

    RECRUTA("Recruta", "Inimigos com metade do poder e mais lentos.", 0.5, 1, false, false),
    AGENTE("Agente", "As regras do enunciado: patrulhas ao acaso perto da sala que guardam.", 1.0, 2, false, false),
    ELITE("Elite", "Inimigos mais fortes que o ouvem e vêm ao seu encontro. Apanhar o alvo dispara o alarme.", 1.3, 2, true, true);

    /** Distância (em ligações) a que um inimigo ouve o Tó Cruz no nível Elite. */
    public static final int RAIO_DETECAO = 2;

    /** Distância a que os inimigos o ouvem enquanto o alarme toca. */
    public static final int RAIO_DETECAO_ALARME = 3;

    /** Turnos a mais, além do caminho mínimo até à saída, que o alarme dá para fugir. */
    public static final int FOLGA_ALARME = 3;

    private final String nome;
    private final String descricao;
    private final double fatorPoder;
    private final int passosInimigos;
    private final boolean perseguicao;
    private final boolean alarme;

    Dificuldade(String nome, String descricao, double fatorPoder, int passosInimigos, boolean perseguicao, boolean alarme) {
        this.nome = nome;
        this.descricao = descricao;
        this.fatorPoder = fatorPoder;
        this.passosInimigos = passosInimigos;
        this.perseguicao = perseguicao;
        this.alarme = alarme;
    }

    /** @return nome para mostrar */
    public String getNome() {
        return nome;
    }

    /** @return frase curta que explica o nível */
    public String getDescricao() {
        return descricao;
    }

    /** @return fator aplicado ao poder de cada inimigo */
    public double getFatorPoder() {
        return fatorPoder;
    }

    /** @return número máximo de divisões que um inimigo anda num turno */
    public int getPassosInimigos() {
        return passosInimigos;
    }

    /** @return se os inimigos perseguem o Tó Cruz quando o ouvem */
    public boolean temPerseguicao() {
        return perseguicao;
    }

    /** @return se apanhar o alvo dispara um alarme com tempo para fugir */
    public boolean temAlarme() {
        return alarme;
    }
}
