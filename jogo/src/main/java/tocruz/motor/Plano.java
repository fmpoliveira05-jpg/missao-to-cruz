package tocruz.motor;

import ed.arraylist.ArrayUnordered;
import ed.interfaces.ArrayUnorderedADT;
import tocruz.mapa.Divisao;

/**
 * Resultado do modo automático: por onde entrar, o caminho até ao alvo, o caminho de volta e a
 * vida com que o Tó Cruz deverá terminar.
 *
 * @author Francisco Oliveira
 */
public class Plano {

    private final ArrayUnorderedADT<Divisao> caminhoIda;
    private final ArrayUnorderedADT<Divisao> caminhoVolta;
    private final long danoIda;
    private final long danoVolta;
    private final long vidaNoAlvo;
    private final long vidaFinal;

    Plano(ArrayUnorderedADT<Divisao> caminhoIda, ArrayUnorderedADT<Divisao> caminhoVolta,
          long danoIda, long danoVolta, long vidaNoAlvo, long vidaFinal) {
        this.caminhoIda = caminhoIda;
        this.caminhoVolta = caminhoVolta;
        this.danoIda = danoIda;
        this.danoVolta = danoVolta;
        this.vidaNoAlvo = vidaNoAlvo;
        this.vidaFinal = vidaFinal;
    }

    /** @return divisão por onde o Tó Cruz deve entrar */
    public Divisao getEntrada() {
        return caminhoIda.find(0);
    }

    /** @return divisão por onde o Tó Cruz deve sair */
    public Divisao getSaida() {
        return caminhoVolta.find(caminhoVolta.size() - 1);
    }

    /** @return divisões da entrada até ao alvo (inclusive) */
    public ArrayUnorderedADT<Divisao> getCaminhoIda() {
        return copia(caminhoIda);
    }

    /** @return divisões do alvo (inclusive) até à saída */
    public ArrayUnorderedADT<Divisao> getCaminhoVolta() {
        return copia(caminhoVolta);
    }

    /** @return dano previsto até ao alvo (poder dos inimigos nas divisões atravessadas) */
    public long getDanoIda() {
        return danoIda;
    }

    /** @return dano previsto no regresso */
    public long getDanoVolta() {
        return danoVolta;
    }

    /** @return vida prevista ao chegar ao alvo, já contando com os itens do caminho */
    public long getVidaNoAlvo() {
        return vidaNoAlvo;
    }

    /** @return vida prevista à saída do edifício */
    public long getVidaFinal() {
        return vidaFinal;
    }

    /** @return se o Tó Cruz chega vivo ao alvo */
    public boolean chegaAoAlvo() {
        return vidaNoAlvo > 0;
    }

    /** @return se a missão pode ser cumprida: chegar ao alvo e sair com vida */
    public boolean isViavel() {
        return vidaFinal > 0;
    }

    /**
     * @return texto com o plano, para a consola e para o painel da interface gráfica
     */
    public String descrever() {
        StringBuilder texto = new StringBuilder();
        texto.append("Entrar por: ").append(getEntrada().getName()).append('\n');
        texto.append("Até ao alvo: ").append(juntar(caminhoIda)).append(" (dano previsto ").append(danoIda).append(")\n");
        texto.append("Regresso: ").append(juntar(caminhoVolta)).append(" (dano previsto ").append(danoVolta).append(")\n");
        if (!chegaAoAlvo()) {
            texto.append("O Tó Cruz não chega vivo ao alvo por nenhuma das entradas.");
        } else if (!isViavel()) {
            texto.append("Chega ao alvo com ").append(vidaNoAlvo).append(" de vida, mas não consegue sair com vida.");
        } else {
            texto.append("Missão viável: sai do edifício com ").append(vidaFinal).append(" pontos de vida.");
        }
        return texto.toString();
    }

    private static String juntar(ArrayUnorderedADT<Divisao> caminho) {
        StringBuilder texto = new StringBuilder();
        for (Divisao divisao : caminho) {
            if (texto.length() > 0) {
                texto.append(" → ");
            }
            texto.append(divisao.getName());
        }
        return texto.toString();
    }

    private static ArrayUnorderedADT<Divisao> copia(ArrayUnorderedADT<Divisao> original) {
        ArrayUnorderedADT<Divisao> copia = new ArrayUnordered<>();
        for (Divisao divisao : original) {
            copia.addToRear(divisao);
        }
        return copia;
    }
}
