package tocruz.missoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ed.interfaces.OrderedListADT;
import ed.linkedlist.LinearLinkedOrderedList;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import tocruz.mapa.Edificio;

/**
 * Ordenação dos resultados de várias versões de uma missão.
 */
class TestResultadoSimulacao {

    private static Simulacoes simulacao(long numero, long vida) throws Exception {
        Simulacoes s = new Simulacoes(numero, new Edificio());
        Field campo = Simulacoes.class.getDeclaredField("vida_to");
        campo.setAccessible(true);
        campo.setLong(s, vida);
        return s;
    }

    @Test
    void ordenaPorVidaRestanteEntreVersoes() throws Exception {
        OrderedListADT<ResultadoSimulacao> lista = new LinearLinkedOrderedList<>();
        lista.add(new ResultadoSimulacao(1, simulacao(1, 40)));
        lista.add(new ResultadoSimulacao(2, simulacao(1, 90)));
        lista.add(new ResultadoSimulacao(1, simulacao(2, 70)));

        Iterator<ResultadoSimulacao> it = lista.iterator();
        ResultadoSimulacao primeiro = it.next();
        assertEquals(2, primeiro.getVersaoMissao());
        assertEquals(90, (long) primeiro.getSimulacao().getVida_to());
        assertEquals(70, (long) it.next().getSimulacao().getVida_to());
        assertEquals(40, (long) it.next().getSimulacao().getVida_to());
    }

    @Test
    void emCasoDeEmpateAVersaoMaisAntigaVemPrimeiro() throws Exception {
        ResultadoSimulacao v1 = new ResultadoSimulacao(1, simulacao(1, 50));
        ResultadoSimulacao v3 = new ResultadoSimulacao(3, simulacao(1, 50));
        assertTrue(v1.compareTo(v3) < 0);
    }

    @Test
    void descricaoMostraAVersaoESemTrajetoNaoFalha() throws Exception {
        String texto = new ResultadoSimulacao(4, simulacao(2, 30)).toString();
        assertTrue(texto.startsWith("Versão 4 | simulação 2 | vida restante: 30"));
        assertTrue(texto.endsWith("trajeto: (vazio)"));
    }
}
