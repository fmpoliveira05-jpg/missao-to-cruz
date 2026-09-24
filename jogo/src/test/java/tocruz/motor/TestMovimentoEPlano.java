package tocruz.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ed.interfaces.ArrayUnorderedADT;
import java.util.Iterator;
import java.util.Random;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.personagens.Inimigo;
import tocruz.personagens.ToCruz;

/**
 * Custos de entrada, cópia do edifício, movimento dos inimigos e modo automático.
 */
class TestMovimentoEPlano {

    /** R0 ── R1 ── R2 ── R3 ── R4 ── R5 ── R6 */
    private static Edificio corredor() {
        Edificio e = Mapas.edificio("R0", "R1", "R2", "R3", "R4", "R5", "R6");
        for (int i = 0; i < 6; i++) {
            Mapas.ligar(e, "R" + i, "R" + (i + 1));
        }
        return e;
    }

    private static Divisao ondeEsta(Edificio e, Inimigo inimigo) {
        Iterator<Divisao> it = e.IteratorMapa();
        while (it.hasNext()) {
            Divisao d = it.next();
            if (d.getInimigos().contains(inimigo)) {
                return d;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ custos e cópia

    @Test
    void custoDeEntradaEODanoDoCombate() {
        Edificio e = corredor();
        assertEquals(0, e.custoEntrada(e.getDivisao("R0")));

        // Inimigos com 100 de vida: 3 ataques do Tó Cruz (40), 2 respostas.
        Mapas.inimigo(e, "R1", "a", 5);
        Mapas.inimigo(e, "R1", "b", 15);
        assertEquals(40, e.custoEntrada(e.getDivisao("R1")));

        // Um inimigo já ferido morre ao primeiro ataque: não chega a responder.
        Inimigo ferido = new Inimigo("c", 50, "R2");
        ferido.setVida(ToCruz.PODER);
        e.getDivisao("R2").addInimigo(ferido);
        assertEquals(0, e.custoEntrada(e.getDivisao("R2")));
    }

    @Test
    void custoFicaNoGrafoSoParaQuemEntra() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R3", "a", 10);
        assertEquals(20.0, e.getShortestPath(e.getDivisao("R0"), e.getDivisao("R3")));
        assertEquals(0.0, e.getShortestPath(e.getDivisao("R3"), e.getDivisao("R0")));
    }

    @Test
    void copiaDoEdificioEIndependenteETemSoAsLigacoesReais() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R2", "a", 10);
        Edificio copia = new Edificio(e.getId(), e.getName(), e.getPlantaEdificio());

        assertEquals(1, copia.divisoesAte(copia.getDivisao("R0"), 1).size() - 1, "R0 só tem um vizinho.");
        assertEquals(20, copia.custoEntrada(copia.getDivisao("R2")));

        copia.getDivisao("R2").getInimigos().first().setVida(0);
        copia.getDivisao("R2").removeInimigo(copia.getDivisao("R2").getInimigos().first());
        assertEquals(1, e.getDivisao("R2").getInimigos().size(), "O original não pode mudar.");
        assertEquals(100, e.getDivisao("R2").getInimigos().first().getVida());
    }

    @Test
    void divisoesAteRespeitaADistancia() {
        Edificio e = corredor();
        ArrayUnorderedADT<Divisao> perto = e.divisoesAte(e.getDivisao("R3"), 2);
        assertEquals(5, perto.size());
        assertEquals("R3", perto.first().getName(), "A própria divisão vem primeiro.");
        assertFalse(perto.contains(e.getDivisao("R0")));
        assertFalse(perto.contains(e.getDivisao("R6")));
        assertNull(e.getDivisao("não existe"));
    }

    // ------------------------------------------------------------ movimento

    @Test
    void inimigoNuncaSeAfastaMaisDeDuasLigacoesDaDivisaoQueGuarda() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R3", "sentinela", 10);
        Inimigo sentinela = e.getDivisao("R3").getInimigos().first();
        ArrayUnorderedADT<Divisao> zona = e.divisoesAte(e.getDivisao("R3"), 2);
        MovimentoInimigos movimento = new MovimentoInimigos(new Random(7));

        Divisao antes = e.getDivisao("R3");
        int divisoesVisitadas = 0;
        boolean[] visitada = new boolean[7];
        for (int turno = 0; turno < 300; turno++) {
            movimento.moverInimigos(e);
            Divisao agora = ondeEsta(e, sentinela);
            assertNotNull(agora);
            assertTrue(zona.contains(agora), "Saiu da zona: " + agora.getName());
            assertTrue(e.divisoesAte(antes, 2).contains(agora), "Andou mais de duas ligações.");
            int indice = agora.getName().charAt(1) - '0';
            if (!visitada[indice]) {
                visitada[indice] = true;
                divisoesVisitadas++;
            }
            antes = agora;
        }
        assertEquals(5, divisoesVisitadas, "Deve patrulhar toda a zona.");
    }

    @Test
    void custosAcompanhamOMovimento() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R3", "sentinela", 10);
        Inimigo sentinela = e.getDivisao("R3").getInimigos().first();
        MovimentoInimigos movimento = new MovimentoInimigos(new Random(3));
        for (int turno = 0; turno < 20; turno++) {
            movimento.moverInimigos(e);
            Divisao agora = ondeEsta(e, sentinela);
            Iterator<Divisao> it = e.IteratorMapa();
            while (it.hasNext()) {
                Divisao d = it.next();
                double esperado = d.equals(agora) ? 20.0 : 0.0;
                if (!d.getName().equals("R0")) {
                    Divisao anterior = e.getDivisao("R" + (d.getName().charAt(1) - '0' - 1));
                    assertEquals(esperado, e.getShortestPath(anterior, d) - e.getShortestPath(anterior, anterior));
                }
            }
        }
    }

    @Test
    void inimigosEmCombateNaoSeMovem() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R3", "sentinela", 10);
        e.getDivisao("R3").addToCruz(new ToCruz());
        MovimentoInimigos movimento = new MovimentoInimigos(new Random(1));
        for (int turno = 0; turno < 30; turno++) {
            movimento.moverInimigos(e);
            assertEquals(1, e.getDivisao("R3").getInimigos().size());
        }
    }

    // ------------------------------------------------------------ modo automático

    @Test
    void planoEvitaOGuardaEGuardaOKit() {
        Edificio e = Mapas.doisCaminhos();
        Plano plano = new PlaneadorMissao().planear(e);
        assertEquals("S", plano.getEntrada().getName(), "Pela saída S o alvo fica logo ao lado.");
        assertEquals(2, plano.getCaminhoIda().size());

        e.getDivisao("S").setEntrada_saida(false);
        plano = new PlaneadorMissao().planear(e);
        assertEquals("E", plano.getEntrada().getName());
        assertEquals("B", plano.getCaminhoIda().find(1).getName(), "Deve evitar o guarda em A.");
        assertEquals("E", plano.getSaida().getName());
        assertEquals(0, plano.getDanoIda() + plano.getDanoVolta());
        assertEquals(100, plano.getVidaFinal());
        assertTrue(plano.isViavel());
    }

    @Test
    void regressoNaoVoltaAContarInimigosJaEliminados() {
        Edificio e = Mapas.edificio("E", "A", "X");
        Mapas.ligar(e, "E", "A");
        Mapas.ligar(e, "A", "X");
        e.getDivisao("E").setEntrada_saida(true);
        e.getDivisao("X").setAlvo(new tocruz.mapa.Alvo("alvo"));
        Mapas.inimigo(e, "A", "guarda", 10);

        Plano plano = new PlaneadorMissao().planear(e);
        assertEquals(20, plano.getDanoIda());
        assertEquals(0, plano.getDanoVolta());
        assertEquals(80, plano.getVidaFinal());
        assertEquals(1, e.getDivisao("A").getInimigos().size(), "Planear não pode alterar o edifício.");
    }

    @Test
    void planoMarcaMissaoInviavel() {
        Edificio e = Mapas.edificio("E", "A", "X");
        Mapas.ligar(e, "E", "A");
        Mapas.ligar(e, "A", "X");
        e.getDivisao("E").setEntrada_saida(true);
        e.getDivisao("X").setAlvo(new tocruz.mapa.Alvo("alvo"));
        Mapas.inimigo(e, "A", "chefe", 60);

        Plano plano = new PlaneadorMissao().planear(e);
        assertFalse(plano.chegaAoAlvo());
        assertFalse(plano.isViavel());
        assertTrue(plano.descrever().contains("não chega vivo"));
    }

    @Test
    void previsaoDoPlanoBateCertoComOJogoQuandoOsInimigosNaoSeMexem() {
        Edificio e = Mapas.edificio("E", "A", "B", "X", "S");
        Mapas.ligar(e, "E", "A");
        Mapas.ligar(e, "A", "B");
        Mapas.ligar(e, "B", "X");
        Mapas.ligar(e, "X", "S");
        e.getDivisao("E").setEntrada_saida(true);
        e.getDivisao("S").setEntrada_saida(true);
        e.getDivisao("X").setAlvo(new tocruz.mapa.Alvo("alvo"));
        Mapas.inimigo(e, "A", "a", 15);
        Mapas.kit(e, "B", 20);
        Mapas.inimigo(e, "X", "b", 10);

        Plano plano = new PlaneadorMissao().planear(e);
        Edificio copia = new Edificio(e.getId(), e.getName(), e.getPlantaEdificio());
        MotorJogo motor = new MotorJogo(copia, new Mapas.InimigosParados());
        assertTrue(new JogadorAutomatico().jogar(motor, copia.getDivisao(plano.getEntrada().getName())));
        assertEquals(plano.getVidaFinal(), motor.getToCruz().getVida());
    }
}
