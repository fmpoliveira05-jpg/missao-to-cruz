package tocruz.personagens;

/**
 * Esta classe representa um inimigo no jogo.
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 1.0
 */
public class Inimigo extends Personagem {

    /**
     * Nome da divisão que o inimigo protege. O enunciado limita os movimentos de cada inimigo às
     * divisões a uma distância máxima de duas ligações desta. Pode ser {@code null} (inimigos criados
     * nos testes), caso em que o inimigo se pode mover livremente.
     */
    private String divisaoGuardada;

    /**
     * Construtor da classe Inimigo.
     *
     * @param nome  Nome do inimigo.
     * @param poder Poder do inimigo.
     */
    public Inimigo(String nome, long poder) {
        super(nome, poder);
    }

    /**
     * Construtor usado na importação: o inimigo fica associado à divisão que protege.
     *
     * @param nome            Nome do inimigo.
     * @param poder           Poder do inimigo.
     * @param divisaoGuardada Nome da divisão que protege.
     */
    public Inimigo(String nome, long poder, String divisaoGuardada) {
        super(nome, poder);
        this.divisaoGuardada = divisaoGuardada;
    }

    /**
     * Construtor da classe Inimigo.
     * Inicializa um novo objeto Inimigo com as propriedades fornecidas.
     *
     * @param id_personagem O ID do personagem inimigo.
     * @param nome          O nome do inimigo.
     * @param vida          A quantidade de vida do inimigo.
     * @param poder         O poder de ataque do inimigo.
     */
    public Inimigo(int id_personagem, String nome, long vida, long poder) {
        super(id_personagem, nome, vida, poder);
    }

    /**
     * Cria uma cópia independente de outro inimigo (mesmo id, vida, poder e divisão guardada).
     *
     * @param outro inimigo a copiar
     */
    public Inimigo(Inimigo outro) {
        super(outro.getId_personagem(), outro.getNome(), outro.getVida(), outro.getPoder());
        this.divisaoGuardada = outro.divisaoGuardada;
    }

    /**
     * @return nome da divisão que o inimigo protege, ou {@code null} se não tiver uma
     */
    public String getDivisaoGuardada() {
        return divisaoGuardada;
    }

    /**
     * Retorna a representação em String com os dados do inimigo.
     *
     * @return String com os detalhes do inimigo.
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Compara este alvo com outro objeto para verificar se são iguais.
     * A comparação é baseada no identificador único e no nome do alvo.
     *
     * @param o o objeto a ser comparado.
     * @return true se os alvos forem iguais (mesmo identificador ou mesmo nome),
     * caso contrário false.
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Gera um código hash para o alvo baseado no nome.
     *
     * @return o código hash do nome do alvo.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Ajusta o poder do inimigo ao nível de dificuldade (nunca fica abaixo de 1).
     *
     * @param fator fator a aplicar ao poder atual
     */
    public void ajustarPoder(double fator) {
        setPoder(Math.max(1, Math.round(getPoder() * fator)));
    }
}
