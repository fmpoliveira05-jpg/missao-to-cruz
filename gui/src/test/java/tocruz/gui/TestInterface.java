package tocruz.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Edificio;
import tocruz.missoes.Missao;
import tocruz.motor.Dificuldade;
import tocruz.motor.JogadorAutomatico;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;

/**
 * Disposição da planta, animações e desenho dos painéis (sem abrir janelas).
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
        DisposicaoGrafo d = DisposicaoGrafo.calcular(ligacoes(n, pares), iguais(n, 120), iguais(n, 58), 1.4);
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
    void plantaTemUmCorredorPorLigacaoEPortasNasSaidas() throws Exception {
        Missao missao = JanelaMissao.carregar(null);
        Planta p = Planta.criar(missao.getEdificio());
        for (int a = 0; a < p.nomes.length; a++) {
            for (int b = 0; b < p.nomes.length; b++) {
                assertEquals(p.ligado[a][b], p.corredor(a, b) != null);
                if (a != b) {
                    assertFalse(p.salas[a].intersects(p.salas[b]), "Salas sobrepostas.");
                }
            }
            if (p.saida[a]) {
                assertNotNull(p.portaExterior[a], p.nomes[a] + " devia ter porta para o exterior.");
            }
        }
    }

    @Test
    void jogoInteiroAnimaDoPrincipioAoFimEmTodosOsNiveis() throws Exception {
        Missao missao = JanelaMissao.carregar(null);
        Edificio base = missao.getEdificio();
        Planta planta = Planta.criar(base);
        PainelPlanta painel = new PainelPlanta();
        painel.setPlanta(planta);
        painel.setEscolherDificuldade(false);
        PainelLateral lateral = new PainelLateral();

        for (Dificuldade nivel : Dificuldade.values()) {
            MotorJogo motor = new MotorJogo(new Edificio(base.getId(), base.getName(), base.getPlantaEdificio()), new Random(3), nivel);
            Cena cena = new Cena(planta);
            cena.carregar(motor);
            painel.setJogo(motor, cena);
            desenhar(painel, lateral, motor, cena);

            motor.entrar(new PlaneadorMissao().planear(motor.getEdificio()).getEntrada());
            JogadorAutomatico jogador = new JogadorAutomatico();
            int acoes = 0;
            do {
                cena.acrescentar(motor.consumirEventos());
                int passos = 0;
                while (cena.ocupada()) {
                    cena.avancar(33);
                    if (passos++ % 10 == 0) {
                        desenhar(painel, lateral, motor, cena);
                    }
                    assertTrue(passos < 2000, "A animação não termina.");
                }
                cena.sincronizar(motor);
                if (motor.getEstado() != MotorJogo.Estado.TERMINADO) {
                    jogador.jogarTurno(motor);
                }
                acoes++;
            } while (motor.getEstado() != MotorJogo.Estado.TERMINADO && acoes < JogadorAutomatico.LIMITE_TURNOS);
            cena.acrescentar(motor.consumirEventos());
            while (cena.ocupada()) {
                cena.avancar(33);
            }
            cena.sincronizar(motor);
            desenhar(painel, lateral, motor, cena);

            // No fim, cada inimigo vivo está na mesma sala no ecrã e no motor.
            for (Figura f : cena.figuras()) {
                if (!f.toCruz && !f.morto) {
                    assertTrue(motor.getEdificio().getDivisao(f.sala).getInimigos().size() > 0, f.nome + " fora do sítio");
                }
            }
        }
    }

    private static void desenhar(PainelPlanta painel, PainelLateral lateral, MotorJogo motor, Cena cena) {
        lateral.atualizar(motor, false, cena.ocupada());
        BufferedImage imagem = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        painel.setSize(1000, 700);
        painel.paint(g);
        lateral.setSize(372, 700);
        lateral.doLayout();
        lateral.paint(g);
        g.dispose();
    }
}
