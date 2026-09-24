package tocruz.motor;

/**
 * Algo que aconteceu durante um turno, com os dados necessários para o mostrar (por exemplo, para
 * a interface gráfica animar um disparo ou um movimento).
 *
 * <p>O motor produz uma frase para o relato e um evento para cada acontecimento; a consola usa as
 * frases e a interface gráfica usa os eventos.</p>
 *
 * @author Francisco Oliveira
 */
public final class Evento {

    /** Tipos de acontecimento. */
    public enum Tipo {
        /** O Tó Cruz entrou no edifício ({@code destino}). */
        ENTRADA,
        /** O Tó Cruz passou de {@code origem} para {@code destino}. */
        MOVIMENTO_TO,
        /** Um inimigo patrulhou ao longo de {@code caminho}. */
        MOVIMENTO_INIMIGO,
        /** Um inimigo ouviu o Tó Cruz e foi ao encontro dele ao longo de {@code caminho}. */
        PERSEGUICAO,
        /** O Tó Cruz acertou num inimigo: {@code valor} de dano, {@code restante} de vida do inimigo. */
        ATAQUE_TO,
        /** Um inimigo morreu. */
        INIMIGO_ELIMINADO,
        /** Um inimigo acertou no Tó Cruz: {@code valor} de dano, {@code restante} de vida do Tó Cruz. */
        ATAQUE_INIMIGO,
        /** O Tó Cruz usou um kit: {@code valor} pontos, {@code restante} de vida. */
        KIT_USADO,
        /** O Tó Cruz guardou um kit de {@code valor} pontos. */
        KIT_GUARDADO,
        /** O Tó Cruz vestiu um colete: {@code valor} pontos, {@code restante} de vida. */
        COLETE,
        /** O Tó Cruz apanhou o alvo ({@code personagem} é o nome do alvo). */
        ALVO,
        /** Disparou o alarme: {@code valor} turnos para sair. */
        ALARME,
        /** O Tó Cruz saiu do edifício por {@code origem}. */
        SAIDA,
        /** O Tó Cruz morreu. */
        MORTE,
        /** O tempo do alarme acabou antes de o Tó Cruz sair. */
        TEMPO_ESGOTADO
    }

    private final Tipo tipo;
    private final String personagem;
    private final String origem;
    private final String destino;
    private final String[] caminho;
    private final long valor;
    private final long restante;

    Evento(Tipo tipo, String personagem, String origem, String destino, String[] caminho, long valor, long restante) {
        this.tipo = tipo;
        this.personagem = personagem;
        this.origem = origem;
        this.destino = destino;
        this.caminho = caminho == null ? new String[0] : caminho.clone();
        this.valor = valor;
        this.restante = restante;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** @return nome do inimigo envolvido, ou do alvo; {@code null} quando não se aplica */
    public String getPersonagem() {
        return personagem;
    }

    /** @return divisão onde o acontecimento começa */
    public String getOrigem() {
        return origem;
    }

    /** @return divisão onde o acontecimento acaba */
    public String getDestino() {
        return destino;
    }

    /** @return divisões percorridas, da origem ao destino (vazio se não houve movimento) */
    public String[] getCaminho() {
        return caminho.clone();
    }

    public long getValor() {
        return valor;
    }

    public long getRestante() {
        return restante;
    }

    @Override
    public String toString() {
        return tipo + (personagem == null ? "" : " " + personagem) + (origem == null ? "" : " " + origem)
                + (destino == null ? "" : " -> " + destino) + " (" + valor + ", " + restante + ")";
    }
}
