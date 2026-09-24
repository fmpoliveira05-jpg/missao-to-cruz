package tocruz.gui;

/**
 * Uma personagem tal como aparece no ecrã: posição, orientação, vida mostrada e efeitos.
 *
 * <p>É separada da personagem do jogo porque, durante uma animação, o ecrã ainda mostra o
 * estado anterior (por exemplo, a vida antes de a bala chegar).</p>
 *
 * @author Francisco Oliveira
 */
final class Figura {

    final String nome;
    final boolean toCruz;

    double x;
    double y;
    double angulo;
    double alfa = 1;
    /** Transparência para onde a figura tende (0 quando está escondida pelo nevoeiro). */
    double alfaAlvo = 1;
    long vida;
    long vidaMaxima;
    long poder;
    /** Divisão onde a figura está (ou para onde vai). */
    String sala;
    boolean visivel = true;
    boolean emMovimento;
    boolean morto;
    /** Rotação extra quando cai (0 a 90 graus, em radianos). */
    double queda;
    double flash;
    /** Tempo restante do "!" por cima de um inimigo que ouviu o Tó Cruz. */
    double alerta;
    /** O Tó Cruz leva o objetivo às costas. */
    boolean comObjetivo;

    Figura(String nome, boolean toCruz) {
        this.nome = nome;
        this.toCruz = toCruz;
    }

    /** Ponto de onde sai a bala (ponta da arma). */
    double bocaX() {
        double e = PainelPlanta.ESCALA_FIGURA;
        return x + Math.cos(angulo) * 26 * e - Math.sin(angulo) * 4.5 * e;
    }

    double bocaY() {
        double e = PainelPlanta.ESCALA_FIGURA;
        return y + Math.sin(angulo) * 26 * e + Math.cos(angulo) * 4.5 * e;
    }

    void virarPara(double tx, double ty) {
        if (Math.hypot(tx - x, ty - y) > 1) {
            angulo = Math.atan2(ty - y, tx - x);
        }
    }
}
