package tocruz.motor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ed.interfaces.ArrayUnorderedADT;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Alvo;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;

/**
 * Níveis de dificuldade, perseguição, alarme e eventos para a interface gráfica.
 */
class TestDificuldades {

    /** R0 (entrada) ── R1 ── R2 ── R3 ── R4 ── R5 ── R6 */
    private static Edificio corredor() {
        Edificio e = Mapas.edificio("R0", "R1", "R2", "R3", "R4", "R5", "R6");
        for (int i = 0; i < 6; i++) {
            Mapas.ligar(e, "R" + i, "R" + (i + 1));
        }
        e.getDivisao("R0").setEntrada_saida(true);
        return e;
    }

    private static int contar(MotorJogo m, Evento.Tipo tipo) {
        int total = 0;
        for (Evento evento : m.consumirEventos()) {
            if (evento.getTipo() == tipo) {
                total++;
            }
        }
        return total;
    }

    @Test
    void caminhoEmLigacoesDaOPercursoCompleto() {
        Edificio e = corredor();
        ArrayUnorderedADT<Divisao> caminho = e.caminhoEmLigacoes(e.getDivisao("R1"), e.getDivisao("R4"));
        assertEquals(4, caminho.size());
        assertEquals("R1", caminho.first().getName());
        assertEquals("R4", caminho.last().getName());
    }

    @Test
    void recrutaEnfrentaInimigosComMetadeDoPoder() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R1", "guarda", 30);
        new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.RECRUTA);
        assertEquals(15, e.getDivisao("R1").getInimigos().first().getPoder());
        assertEquals(30, e.custoEntrada(e.getDivisao("R1")));
    }

    @Test
    void eliteInimigoOuveOTóCruzEAvancaUmaDivisao() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R2", "caçador", 10);
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.ELITE);
        m.entrar(e.getDivisao("R0"));

        assertEquals(1, e.getDivisao("R1").getInimigos().size(), "Ouviu-o a duas divisões e avançou uma.");
        boolean perseguiu = false;
        for (Evento evento : m.consumirEventos()) {
            if (evento.getTipo() == Evento.Tipo.PERSEGUICAO) {
                perseguiu = true;
                assertArrayEquals(new String[]{"R2", "R1"}, evento.getCaminho());
            }
        }
        assertTrue(perseguiu);
    }

    @Test
    void eliteQuemChegaAoTóCruzCaiNumaEmboscada() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R1", "caçador", 10);
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.ELITE);
        assertEquals(13, e.getDivisao("R1").getInimigos().first().getPoder(), "No nível Elite os inimigos são 30% mais fortes.");
        m.entrar(e.getDivisao("R0"));

        assertTrue(m.emCombate());
        assertEquals(100, m.getToCruz().getVida(), "O Tó Cruz estava à espera: dispara primeiro.");
        assertEquals(60, e.getDivisao("R0").getInimigos().first().getVida());
    }

    @Test
    void agenteNaoPersegue() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R2", "guarda", 10);
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.AGENTE);
        m.entrar(e.getDivisao("R0"));
        assertFalse(m.emCombate());
        assertEquals(1, e.getDivisao("R2").getInimigos().size());
    }

    @Test
    void eliteNaoOuveQuemEstaLonge() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R4", "guarda", 10);
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.ELITE);
        m.entrar(e.getDivisao("R0"));
        assertEquals(1, e.getDivisao("R4").getInimigos().size());
    }

    @Test
    void eliteInimigoVoltaAoPostoDepoisDePerseguir() {
        Edificio e = corredor();
        Mapas.inimigo(e, "R6", "sentinela", 10);
        // Tirado à força da zona que guarda (R4 a R6).
        e.getDivisao("R6").removeInimigo(e.getDivisao("R6").getInimigos().first());
        e.getDivisao("R1").addInimigo(new tocruz.personagens.Inimigo("sentinela", 10, "R6"));
        MovimentoInimigos movimento = new MovimentoInimigos(new Mapas.InimigosParados(), Dificuldade.ELITE);

        movimento.moverInimigos(e, null, false);
        assertEquals(1, e.getDivisao("R3").getInimigos().size(), "Sem ouvir ninguém, regressa duas divisões.");
        movimento.moverInimigos(e, null, false);
        assertEquals(1, e.getDivisao("R5").getInimigos().size(), "Continua o regresso até à zona que guarda.");
        movimento.moverInimigos(e, null, false);
        assertEquals(1, e.getDivisao("R5").getInimigos().size(), "Já na zona, volta às regras da patrulha.");
    }

    @Test
    void alarmeDaTurnosParaSairEFalhaSeAcabarem() {
        Edificio e = corredor();
        e.getDivisao("R2").setAlvo(new Alvo("documentos"));
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.ELITE);
        m.entrar(e.getDivisao("R0"));
        m.mover(e.getDivisao("R1"));
        m.mover(e.getDivisao("R2"));

        assertTrue(m.isAlarmeAtivo());
        assertEquals(2 + Dificuldade.FOLGA_ALARME, m.getTurnosParaFugir());
        for (int i = 0; i < 4; i++) {
            m.aguardar();
        }
        assertEquals(1, m.getTurnosParaFugir());
        m.aguardar();
        assertEquals(MotorJogo.Estado.TERMINADO, m.getEstado());
        assertFalse(m.isSucesso());
    }

    @Test
    void alarmeCumpridoAtempoDaSucesso() {
        Edificio e = corredor();
        e.getDivisao("R2").setAlvo(new Alvo("documentos"));
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.ELITE);
        m.entrar(e.getDivisao("R0"));
        m.mover(e.getDivisao("R1"));
        m.mover(e.getDivisao("R2"));
        m.mover(e.getDivisao("R1"));
        m.mover(e.getDivisao("R0"));
        m.sair();
        assertTrue(m.isSucesso());
    }

    @Test
    void semAlarmeNosOutrosNiveis() {
        Edificio e = corredor();
        e.getDivisao("R1").setAlvo(new Alvo("documentos"));
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados(), Dificuldade.AGENTE);
        m.entrar(e.getDivisao("R0"));
        m.mover(e.getDivisao("R1"));
        assertFalse(m.isAlarmeAtivo());
        assertEquals(-1, m.getTurnosParaFugir());
    }

    @Test
    void combateGeraEventosPorOrdem() {
        Edificio e = Mapas.doisCaminhos();
        MotorJogo m = new MotorJogo(e, new Mapas.InimigosParados());
        m.entrar(e.getDivisao("E"));
        m.consumirEventos();
        m.mover(e.getDivisao("A"));

        Evento.Tipo[] esperado = {Evento.Tipo.MOVIMENTO_TO, Evento.Tipo.ATAQUE_TO, Evento.Tipo.ATAQUE_INIMIGO};
        int i = 0;
        for (Evento evento : m.consumirEventos()) {
            assertEquals(esperado[i++], evento.getTipo());
        }
        assertEquals(3, i);

        m.atacar();
        m.atacar();
        assertEquals(1, contar(m, Evento.Tipo.INIMIGO_ELIMINADO));
    }
}
