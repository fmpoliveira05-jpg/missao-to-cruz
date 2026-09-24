package tocruz.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Edificio;

/**
 * Regras de um turno: combate, alvo, kits, saída e morte.
 */
class TestMotorJogo {

    private static MotorJogo motor(Edificio edificio) {
        return new MotorJogo(edificio, new Mapas.InimigosParados());
    }

    @Test
    void soEntraPorUmaEntrada() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        assertThrows(IllegalArgumentException.class, () -> m.entrar(e.getDivisao("A")));
        m.entrar(e.getDivisao("E"));
        assertEquals(MotorJogo.Estado.A_JOGAR, m.getEstado());
        assertEquals(1, m.getTurno());
        assertEquals(2, m.getDestinosPossiveis().size());
    }

    @Test
    void combateSegueOsCenariosDoEnunciado() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));

        // Cenário 1: ao entrar, o Tó Cruz ataca primeiro (guarda fica com 60) e o guarda responde.
        m.mover(e.getDivisao("A"));
        assertTrue(m.emCombate());
        assertEquals(70, m.getToCruz().getVida());
        assertEquals(0, m.getDestinosPossiveis().size(), "Em combate não se pode fugir.");
        assertThrows(IllegalStateException.class, () -> m.mover(e.getDivisao("X")));

        m.atacar();
        assertEquals(40, m.getToCruz().getVida());
        m.atacar();
        // Guarda eliminado: o turno acaba sem resposta.
        assertEquals(40, m.getToCruz().getVida());
        assertFalse(m.emCombate());
        assertEquals(1, m.getInimigosMortos().size());
        assertEquals(0, e.custoEntrada(e.getDivisao("A")), "Divisão limpa deixa de custar vida.");
    }

    @Test
    void danoRealIgualAoCustoPrevistoNoGrafo() {
        Edificio e = Mapas.doisCaminhos();
        long previsto = e.custoEntrada(e.getDivisao("A"));
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.mover(e.getDivisao("A"));
        while (m.emCombate()) {
            m.atacar();
        }
        assertEquals(100 - previsto, m.getToCruz().getVida());
    }

    @Test
    void missaoCumpridaComAlvoESaida() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.mover(e.getDivisao("B"));
        m.decidirKit(MotorJogo.DecisaoKit.GUARDAR);
        m.mover(e.getDivisao("X"));
        assertTrue(m.getToCruz().isColectedAlvo());
        m.mover(e.getDivisao("S"));
        assertTrue(m.podeSair());
        m.sair();
        assertEquals(MotorJogo.Estado.TERMINADO, m.getEstado());
        assertTrue(m.isSucesso());
        assertEquals(4, m.getTrajeto().size());
    }

    @Test
    void sairSemAlvoFalhaAMissao() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.sair();
        assertEquals(MotorJogo.Estado.TERMINADO, m.getEstado());
        assertFalse(m.isSucesso());
    }

    @Test
    void kitComVidaCheiaSoPodeSerGuardadoOuDeixado() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.mover(e.getDivisao("B"));
        assertEquals(MotorJogo.Estado.DECIDIR_KIT, m.getEstado());
        assertFalse(m.podeDecidir(MotorJogo.DecisaoKit.USAR));
        assertThrows(IllegalStateException.class, () -> m.decidirKit(MotorJogo.DecisaoKit.USAR));
        m.decidirKit(MotorJogo.DecisaoKit.GUARDAR);
        assertEquals(1, m.getToCruz().getMochila().size());
        assertEquals(MotorJogo.Estado.A_JOGAR, m.getEstado());
        assertFalse(m.podeUsarKit(), "Com a vida cheia o kit fica na mochila.");
    }

    @Test
    void kitDaMochilaCuraDuranteOCombate() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.mover(e.getDivisao("B"));
        m.decidirKit(MotorJogo.DecisaoKit.GUARDAR);
        m.mover(e.getDivisao("X"));
        m.mover(e.getDivisao("A"));
        assertEquals(70, m.getToCruz().getVida());
        assertTrue(m.podeUsarKit());
        m.usarKit();
        // +20 do kit e o guarda volta a atacar (-30).
        assertEquals(60, m.getToCruz().getVida());
        assertEquals(0, m.getToCruz().getMochila().size());
    }

    @Test
    void coleteSomaVidaAlemDos100() {
        Edificio e = Mapas.doisCaminhos();
        Mapas.colete(e, "S", 25);
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("S"));
        assertEquals(125, m.getToCruz().getVida());
    }

    @Test
    void oTóCruzMorreETerminaSemSucesso() {
        Edificio e = Mapas.doisCaminhos();
        Mapas.inimigo(e, "A", "chefe", 200);
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        m.mover(e.getDivisao("A"));
        assertEquals(0, m.getToCruz().getVida());
        assertEquals(MotorJogo.Estado.TERMINADO, m.getEstado());
        assertFalse(m.isSucesso());
    }

    @Test
    void inimigoQueEntraNaDivisaoDoTóCruzAtacaPrimeiro() {
        Edificio e = Mapas.edificio("E", "R1", "R2");
        Mapas.ligar(e, "E", "R1");
        Mapas.ligar(e, "R1", "R2");
        e.getDivisao("E").setEntrada_saida(true);
        Mapas.inimigo(e, "R2", "patrulha", 10);

        // Candidatas do inimigo em R2 (BFS): R2, R1, E. O índice 2 leva-o até à entrada.
        MotorJogo m = new MotorJogo(e, new Random() {
            private static final long serialVersionUID = 1L;

            @Override
            public int nextInt(int limite) {
                return limite - 1;
            }
        });
        m.entrar(e.getDivisao("E"));

        assertTrue(m.emCombate());
        assertEquals(90, m.getToCruz().getVida());
        boolean relatado = false;
        for (String frase : m.consumirRelato()) {
            relatado |= frase.contains("atacou primeiro");
        }
        assertTrue(relatado);
    }

    @Test
    void caminhoSugeridoEvitaOGuardaEDepoisApontaParaASaida() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        m.entrar(e.getDivisao("E"));
        assertEquals("B", m.caminhoSugerido().find(1).getName());

        m.mover(e.getDivisao("B"));
        m.decidirKit(MotorJogo.DecisaoKit.DEIXAR);
        m.mover(e.getDivisao("X"));
        assertEquals("S", m.caminhoSugerido().find(1).getName());
    }

    @Test
    void jogadorAutomaticoCumpreAMissao() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = motor(e);
        assertTrue(new JogadorAutomatico().jogar(m, e.getDivisao("E")));
        assertEquals(100, m.getToCruz().getVida());
    }
}
