package tocruz.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Edificio;
import tocruz.missoes.Missao;
import tocruz.motor.JogadorAutomatico;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;

/**
 * Disposição do mapa e desenho dos painéis (sem abrir janelas).
 */
class TestInterface {

    private static boolean[][] ligacoes(int n, int[][] pares) {
        boolean[][] ligado = new boolean[n][n];
        for (int[] par : pares) {
            ligado[par[0]][par[1]] = true;
            ligado[par[1]][par[0]] = true;
        }
        return ligado;
    }

    private static double[] iguais(int n, double valor) {
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = valor;
        }
        return v;
    }

    @Test
    void cicloFicaSemCruzamentosNemSobreposicoes() {
        int n = 8;
        int[][] pares = new int[n][];
        for (int i = 0; i < n; i++) {
            pares[i] = new int[]{i, (i + 1) % n};
        }
        boolean[][] ligado = ligacoes(n, pares);
        DisposicaoGrafo d = DisposicaoGrafo.calcular(ligado, iguais(n, 120), iguais(n, 58), 1.4);

        assertEquals(0, d.cruzamentos());
        assertEquals(0, d.ligacoesPorCimaDeCaixas());
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                assertFalse(d.sobrepoem(i, j));
            }
        }
    }

    @Test
    void mesmaMissaoDaSempreOMesmoDesenho() {
        boolean[][] ligado = ligacoes(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {1, 3}});
        DisposicaoGrafo a = DisposicaoGrafo.calcular(ligado, iguais(5, 130), iguais(5, 58), 1.4);
        DisposicaoGrafo b = DisposicaoGrafo.calcular(ligado, iguais(5, 130), iguais(5, 58), 1.4);
        for (int i = 0; i < 5; i++) {
            assertEquals(a.x[i], b.x[i]);
            assertEquals(a.y[i], b.y[i]);
        }
    }

    @Test
    void missaoDeExemploDesenhaSeSemDefeitosEmTodasAsFases() throws Exception {
        Missao missao = JanelaMissao.carregar(null);
        Edificio base = missao.getEdificio();
        PainelMapa mapa = new PainelMapa();
        mapa.setEdificio(base);
        PainelLateral lateral = new PainelLateral();

        MotorJogo motor = new MotorJogo(new Edificio(base.getId(), base.getName(), base.getPlantaEdificio()), new Random(1));
        mapa.setMotor(motor);
        desenhar(mapa, lateral, motor);

        motor.entrar(new PlaneadorMissao().planear(motor.getEdificio()).getEntrada());
        mapa.setPlano(new PlaneadorMissao().planear(motor.getEdificio()));
        JogadorAutomatico jogador = new JogadorAutomatico();
        int desenhos = 0;
        while (motor.getEstado() != MotorJogo.Estado.TERMINADO && motor.getTurno() < JogadorAutomatico.LIMITE_TURNOS) {
            jogador.jogarTurno(motor);
            desenhar(mapa, lateral, motor);
            desenhos++;
        }
        desenhar(mapa, lateral, motor);
        assertTrue(desenhos > 5);
    }

    private static void desenhar(PainelMapa mapa, PainelLateral lateral, MotorJogo motor) {
        lateral.atualizar(motor, false);
        BufferedImage imagem = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        mapa.setSize(1000, 700);
        mapa.paint(g);
        lateral.setSize(372, 700);
        lateral.doLayout();
        lateral.paint(g);
        g.dispose();
    }
}
