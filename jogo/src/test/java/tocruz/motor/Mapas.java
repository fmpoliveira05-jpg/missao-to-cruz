package tocruz.motor;

import java.util.Random;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Alvo;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.personagens.Inimigo;

/**
 * Edifícios pequenos, montados à mão, para os testes do motor.
 */
final class Mapas {

    private Mapas() {
    }

    /** Gerador que devolve sempre 0: os inimigos escolhem a primeira candidata, que é ficar onde estão. */
    static final class InimigosParados extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int limite) {
            return 0;
        }
    }

    static Edificio edificio(String... nomes) {
        Edificio edificio = new Edificio();
        for (String nome : nomes) {
            edificio.addDivisao(new Divisao(nome));
        }
        return edificio;
    }

    static void ligar(Edificio edificio, String a, String b) {
        edificio.addLigacao(edificio.getDivisao(a), edificio.getDivisao(b), 0);
    }

    static void inimigo(Edificio edificio, String divisao, String nome, long poder) {
        edificio.getDivisao(divisao).addInimigo(new Inimigo(nome, poder, divisao));
        edificio.atualizarCustoEntrada(edificio.getDivisao(divisao));
    }

    static void kit(Edificio edificio, String divisao, long pontos) {
        edificio.getDivisao(divisao).setItem(new ItemCura(TypeItemCura.KIT_VIDA, pontos));
    }

    static void colete(Edificio edificio, String divisao, long pontos) {
        edificio.getDivisao(divisao).setItem(new ItemCura(TypeItemCura.COLETE, pontos));
    }

    /**
     * Dois caminhos da entrada E ao alvo X:
     * <pre>
     *   E ── A ── X ── S
     *   └─── B ───┘
     * </pre>
     * A tem um guarda de poder 30; B está livre e tem um kit. E e S são entradas/saídas.
     */
    static Edificio doisCaminhos() {
        Edificio e = edificio("E", "A", "B", "X", "S");
        ligar(e, "E", "A");
        ligar(e, "A", "X");
        ligar(e, "E", "B");
        ligar(e, "B", "X");
        ligar(e, "X", "S");
        e.getDivisao("E").setEntrada_saida(true);
        e.getDivisao("S").setEntrada_saida(true);
        e.getDivisao("X").setAlvo(new Alvo("documentos"));
        inimigo(e, "A", "guarda", 30);
        kit(e, "B", 20);
        return e;
    }
}
