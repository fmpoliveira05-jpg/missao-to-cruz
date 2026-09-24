package tocruz.motor;

import ed.interfaces.ArrayUnorderedADT;
import tocruz.mapa.Divisao;
import tocruz.personagens.Inimigo;
import tocruz.personagens.ToCruz;

/**
 * Joga uma simulação sozinho, turno a turno, com as mesmas regras do modo manual.
 *
 * <p>Em cada turno segue o caminho sugerido pelo motor (menor dano até ao alvo e, depois, até à
 * saída). Como o caminho é recalculado todos os turnos, adapta-se aos inimigos que se movem.
 * Usa um kit quando o próximo ataque seria fatal e usa ou guarda os kits que encontra.</p>
 *
 * @author Francisco Oliveira
 */
public class JogadorAutomatico {

    /** Limite de turnos, para uma simulação sem saída possível não ficar a correr para sempre. */
    public static final int LIMITE_TURNOS = 500;

    /**
     * Joga uma simulação completa.
     *
     * @param motor motor ainda por começar
     * @param entrada divisão por onde o Tó Cruz entra
     * @return se a missão terminou com sucesso
     */
    public boolean jogar(MotorJogo motor, Divisao entrada) {
        motor.entrar(entrada);
        while (motor.getEstado() != MotorJogo.Estado.TERMINADO && motor.getTurno() < LIMITE_TURNOS) {
            jogarTurno(motor);
        }
        return motor.isSucesso();
    }

    /**
     * Escolhe e executa a próxima ação.
     *
     * @param motor motor a meio de um jogo
     */
    public void jogarTurno(MotorJogo motor) {
        ToCruz toCruz = motor.getToCruz();

        if (motor.getEstado() == MotorJogo.Estado.DECIDIR_KIT) {
            if (motor.podeDecidir(MotorJogo.DecisaoKit.USAR)) {
                motor.decidirKit(MotorJogo.DecisaoKit.USAR);
            } else if (motor.podeDecidir(MotorJogo.DecisaoKit.GUARDAR)) {
                motor.decidirKit(MotorJogo.DecisaoKit.GUARDAR);
            } else {
                motor.decidirKit(MotorJogo.DecisaoKit.DEIXAR);
            }
            return;
        }

        if (motor.emCombate()) {
            if (toCruz.getVida() <= respostaAoProximoAtaque(motor.getDivisaoAtual()) && motor.podeUsarKit()) {
                motor.usarKit();
            } else {
                motor.atacar();
            }
            return;
        }

        if (toCruz.isColectedAlvo() && motor.podeSair()) {
            motor.sair();
            return;
        }

        ArrayUnorderedADT<Divisao> caminho = motor.caminhoSugerido();
        if (caminho.size() >= 2) {
            Divisao seguinte = caminho.find(1);
            if (toCruz.getVida() <= motor.getEdificio().custoEntrada(seguinte) && motor.podeUsarKit()) {
                motor.usarKit();
            } else {
                motor.mover(seguinte);
            }
        } else if (motor.podeUsarKit()) {
            motor.usarKit();
        } else {
            motor.aguardar();
        }
    }

    /** Dano que os inimigos que sobrevivem ao próximo ataque do Tó Cruz lhe vão causar. */
    private static long respostaAoProximoAtaque(Divisao divisao) {
        long dano = 0;
        for (Inimigo inimigo : divisao.getInimigos()) {
            if (inimigo.getVida() > ToCruz.PODER) {
                dano += inimigo.getPoder();
            }
        }
        return dano;
    }
}
