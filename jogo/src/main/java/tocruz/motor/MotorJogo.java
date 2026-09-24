package tocruz.motor;

import ed.arraylist.ArrayUnordered;
import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.QueueADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import ed.queue.LinkedQueue;
import java.util.Iterator;
import java.util.Random;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.personagens.Inimigo;
import tocruz.personagens.ToCruz;

/**
 * Regras de uma simulação manual, sem qualquer leitura ou escrita na consola.
 *
 * <p>O motor recebe as ações do jogador (entrar, mover, atacar, usar kit, aguardar, sair) e aplica
 * os cenários do enunciado. Tanto o menu de consola como a interface gráfica usam esta classe, o
 * que separa as regras do jogo da forma de as mostrar.</p>
 *
 * <p>Cada turno tem duas fases:</p>
 * <ol>
 *   <li><b>Fase do jogador</b> – o Tó Cruz move-se, ataca, usa um kit ou fica onde está. Ao entrar
 *   numa divisão com inimigos ataca primeiro (cenário 1). Numa divisão sem inimigos apanha o alvo
 *   (cenário 6), consome os coletes e decide o que fazer aos kits.</li>
 *   <li><b>Fase dos inimigos</b> – os inimigos em combate com o Tó Cruz atacam-no; os restantes
 *   movem-se ({@link MovimentoInimigos}); quem entrar na divisão do Tó Cruz ataca de imediato
 *   (cenário 3). Se o Tó Cruz eliminar todos os inimigos da divisão, o turno termina sem esta fase.</li>
 * </ol>
 *
 * <p>No nível {@link Dificuldade#ELITE}, um inimigo que vá ao encontro do Tó Cruz cai numa
 * emboscada: o Tó Cruz ouviu-o chegar e dispara primeiro. Apanhar o alvo dispara um alarme: o Tó
 * Cruz tem o número de turnos do caminho mais curto até uma saída mais
 * {@link Dificuldade#FOLGA_ALARME} para sair.</p>
 *
 * <p>Cada acontecimento fica registado duas vezes: como frase ({@link #consumirRelato()}) e como
 * {@link Evento} ({@link #consumirEventos()}), que a interface gráfica usa para as animações.</p>
 *
 * @author Francisco Oliveira
 */
public class MotorJogo {

    /** Fases em que o jogo pode estar. */
    public enum Estado {
        /** O jogador ainda tem de escolher por onde entrar. */
        ESCOLHER_ENTRADA,
        /** O jogador pode agir. */
        A_JOGAR,
        /** O Tó Cruz está numa divisão com um kit e tem de decidir o que fazer com ele. */
        DECIDIR_KIT,
        /** A simulação terminou. */
        TERMINADO
    }

    /** Opções perante um kit de vida encontrado numa divisão. */
    public enum DecisaoKit {
        /** Usar o kit já. */
        USAR,
        /** Guardar o kit na mochila. */
        GUARDAR,
        /** Deixar o kit na divisão. */
        DEIXAR
    }

    private final Edificio edificio;
    private final Dificuldade dificuldade;
    private final ToCruz toCruz;
    private final MovimentoInimigos movimento;
    private final QueueADT<Divisao> trajeto;
    private final UnorderedListADT<Inimigo> inimigosMortos;

    private UnorderedListADT<String> relato;
    private UnorderedListADT<Evento> eventos;
    private Estado estado;
    private Divisao atual;
    private ItemCura kitPorDecidir;
    private boolean faseInimigosPendente;
    private boolean sucesso;
    private int turno;
    /** Turno até ao qual o Tó Cruz tem de sair (-1 enquanto não houver alarme). */
    private int turnoLimite = -1;

    /**
     * Cria um motor com as regras do enunciado (nível Agente).
     *
     * @param edificio edifício da simulação (deve ser uma cópia do edifício da missão)
     * @param aleatorio gerador usado no movimento dos inimigos
     */
    public MotorJogo(Edificio edificio, Random aleatorio) {
        this(edificio, aleatorio, Dificuldade.AGENTE);
    }

    /**
     * Cria um motor para um edifício. O edifício é alterado durante o jogo, por isso deve ser uma
     * cópia do edifício da missão.
     *
     * @param edificio edifício da simulação
     * @param aleatorio gerador usado no movimento dos inimigos
     * @param dificuldade nível de dificuldade
     */
    public MotorJogo(Edificio edificio, Random aleatorio, Dificuldade dificuldade) {
        this.edificio = edificio;
        this.dificuldade = dificuldade;
        this.toCruz = new ToCruz();
        this.movimento = new MovimentoInimigos(aleatorio, dificuldade);
        this.trajeto = new LinkedQueue<>();
        this.inimigosMortos = new LinearLinkedUnorderedList<>();
        this.relato = new LinearLinkedUnorderedList<>();
        this.eventos = new LinearLinkedUnorderedList<>();
        this.estado = Estado.ESCOLHER_ENTRADA;

        if (dificuldade.getFatorPoder() != 1.0) {
            Iterator<Divisao> divisoes = edificio.IteratorMapa();
            while (divisoes.hasNext()) {
                for (Inimigo inimigo : divisoes.next().getInimigos()) {
                    inimigo.ajustarPoder(dificuldade.getFatorPoder());
                }
            }
            edificio.atualizarCustos();
        }
    }

    // ------------------------------------------------------------------ consultas

    /** @return edifício da simulação */
    public Edificio getEdificio() {
        return edificio;
    }

    /** @return nível de dificuldade */
    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    /** @return o Tó Cruz */
    public ToCruz getToCruz() {
        return toCruz;
    }

    /** @return fase atual do jogo */
    public Estado getEstado() {
        return estado;
    }

    /** @return divisão onde o Tó Cruz está ({@code null} antes de entrar) */
    public Divisao getDivisaoAtual() {
        return atual;
    }

    /** @return número do turno atual (0 antes de entrar) */
    public int getTurno() {
        return turno;
    }

    /** @return se a missão terminou com sucesso (saiu vivo com o alvo) */
    public boolean isSucesso() {
        return sucesso;
    }

    /** @return kit à espera de decisão, ou {@code null} */
    public ItemCura getKitPorDecidir() {
        return kitPorDecidir;
    }

    /** @return se o alarme está a tocar */
    public boolean isAlarmeAtivo() {
        return turnoLimite >= 0;
    }

    /** @return turnos que faltam para o alarme acabar, ou -1 se não houver alarme */
    public int getTurnosParaFugir() {
        return turnoLimite < 0 ? -1 : Math.max(0, turnoLimite - turno);
    }

    /** @return cópia do percurso feito pelo Tó Cruz */
    public QueueADT<Divisao> getTrajeto() {
        QueueADT<Divisao> copia = new LinkedQueue<>();
        int total = trajeto.size();
        for (int i = 0; i < total; i++) {
            Divisao divisao = trajeto.dequeue();
            copia.enqueue(divisao);
            trajeto.enqueue(divisao);
        }
        return copia;
    }

    /** @return inimigos eliminados pelo Tó Cruz */
    public UnorderedListADT<Inimigo> getInimigosMortos() {
        return inimigosMortos;
    }

    /**
     * Devolve as mensagens acumuladas desde a última chamada e começa um relato novo.
     *
     * @return frases a descrever o que aconteceu
     */
    public UnorderedListADT<String> consumirRelato() {
        UnorderedListADT<String> atualRelato = this.relato;
        this.relato = new LinearLinkedUnorderedList<>();
        return atualRelato;
    }

    /**
     * Devolve os eventos acumulados desde a última chamada, pela ordem em que aconteceram.
     *
     * @return eventos para animar
     */
    public UnorderedListADT<Evento> consumirEventos() {
        UnorderedListADT<Evento> atuais = this.eventos;
        this.eventos = new LinearLinkedUnorderedList<>();
        return atuais;
    }

    /** @return divisões por onde o Tó Cruz pode entrar ou sair */
    public ArrayUnorderedADT<Divisao> getEntradas() {
        ArrayUnorderedADT<Divisao> entradas = new ArrayUnordered<>();
        Iterator<Divisao> divisoes = edificio.IteratorMapa();
        while (divisoes.hasNext()) {
            Divisao divisao = divisoes.next();
            if (divisao.isEntrada_saida()) {
                entradas.addToRear(divisao);
            }
        }
        return entradas;
    }

    /** @return se o Tó Cruz está numa divisão com inimigos */
    public boolean emCombate() {
        return atual != null && atual.haveConfronto();
    }

    /** @return divisões para onde o Tó Cruz se pode mover (nenhuma durante um combate) */
    public ArrayUnorderedADT<Divisao> getDestinosPossiveis() {
        ArrayUnorderedADT<Divisao> destinos = new ArrayUnordered<>();
        if (estado == Estado.A_JOGAR && !emCombate()) {
            Iterator<Divisao> vizinhos = edificio.getNextDivisoes(atual);
            while (vizinhos.hasNext()) {
                destinos.addToRear(vizinhos.next());
            }
        }
        return destinos;
    }

    /** @return se é possível usar um kit da mochila */
    public boolean podeUsarKit() {
        return estado == Estado.A_JOGAR && toCruz.mochilaTemKit() && toCruz.getVida() < PlaneadorMissao.VIDA_INICIAL;
    }

    /** @return se o Tó Cruz pode sair do edifício neste turno */
    public boolean podeSair() {
        return estado == Estado.A_JOGAR && atual.isEntrada_saida() && !emCombate();
    }

    /** @return se a opção dada é possível perante o kit por decidir */
    public boolean podeDecidir(DecisaoKit decisao) {
        if (estado != Estado.DECIDIR_KIT) {
            return false;
        }
        switch (decisao) {
            case USAR:
                return toCruz.getVida() < PlaneadorMissao.VIDA_INICIAL;
            case GUARDAR:
                return !toCruz.mochilaIsFull();
            default:
                return true;
        }
    }

    // ------------------------------------------------------------------ ações do jogador

    /**
     * Primeira jogada: o Tó Cruz entra no edifício.
     *
     * @param entrada divisão de entrada
     * @throws IllegalStateException se o jogo já começou
     * @throws IllegalArgumentException se a divisão não for uma entrada
     */
    public void entrar(Divisao entrada) {
        exigirEstado(Estado.ESCOLHER_ENTRADA);
        if (entrada == null || !entrada.isEntrada_saida()) {
            throw new IllegalArgumentException("O Tó Cruz só pode entrar por uma entrada/saída.");
        }
        turno = 1;
        estado = Estado.A_JOGAR;
        entrada.addToCruz(toCruz);
        atual = entrada;
        eventos.addToRear(new Evento(Evento.Tipo.ENTRADA, null, null, entrada.getName(), null, 0, toCruz.getVida()));
        chegarA(entrada);
        verificarAlarme();
    }

    /**
     * Move o Tó Cruz para uma divisão vizinha.
     *
     * @param destino divisão vizinha
     * @throws IllegalStateException se não for possível mover (combate, fim de jogo, ...)
     * @throws IllegalArgumentException se o destino não for vizinho
     */
    public void mover(Divisao destino) {
        exigirEstado(Estado.A_JOGAR);
        if (emCombate()) {
            throw new IllegalStateException("Durante um combate o Tó Cruz só pode atacar ou usar um kit.");
        }
        if (!getDestinosPossiveis().contains(destino)) {
            throw new IllegalArgumentException("A divisão " + destino.getName() + " não é vizinha de " + atual.getName() + ".");
        }
        turno++;
        Divisao origem = atual;
        atual.removeToCruz();
        destino.addToCruz(toCruz);
        atual = destino;
        eventos.addToRear(new Evento(Evento.Tipo.MOVIMENTO_TO, null, origem.getName(), destino.getName(),
                new String[]{origem.getName(), destino.getName()}, 0, toCruz.getVida()));
        chegarA(destino);
        verificarAlarme();
    }

    /**
     * O Tó Cruz ataca os inimigos da divisão onde está.
     *
     * @throws IllegalStateException se não houver combate
     */
    public void atacar() {
        exigirEstado(Estado.A_JOGAR);
        if (!emCombate()) {
            throw new IllegalStateException("Não há inimigos nesta divisão.");
        }
        turno++;
        relato.addToRear("Turno " + turno + ": o Tó Cruz continua o combate em " + atual.getName() + ".");
        ataqueDoToCruz();
        if (emCombate()) {
            faseInimigos();
        } else {
            // Inimigos eliminados: o turno termina sem movimento dos inimigos.
            tratarAlvoEItem(false);
        }
        verificarAlarme();
    }

    /**
     * Usa o kit do topo da mochila. Gasta a fase do jogador (cenário 4).
     *
     * @throws IllegalStateException se não houver kit ou a vida estiver cheia
     */
    public void usarKit() {
        if (!podeUsarKit()) {
            throw new IllegalStateException("Não há kits na mochila ou a vida já está cheia.");
        }
        turno++;
        long antes = toCruz.getVida();
        ItemCura kit = toCruz.usarKit();
        relato.addToRear("Turno " + turno + ": o Tó Cruz usou um kit da mochila: " + antes + " → " + toCruz.getVida() + " de vida.");
        eventos.addToRear(new Evento(Evento.Tipo.KIT_USADO, null, atual.getName(), atual.getName(), null,
                kit.getVida_recuperada(), toCruz.getVida()));
        faseInimigos();
        verificarAlarme();
    }

    /**
     * O Tó Cruz fica onde está e os inimigos jogam.
     */
    public void aguardar() {
        exigirEstado(Estado.A_JOGAR);
        if (emCombate()) {
            throw new IllegalStateException("Durante um combate o Tó Cruz só pode atacar ou usar um kit.");
        }
        turno++;
        relato.addToRear("Turno " + turno + ": o Tó Cruz ficou em " + atual.getName() + ".");
        faseInimigos();
        verificarAlarme();
    }

    /**
     * Decide o que fazer com o kit encontrado na divisão.
     *
     * @param decisao usar, guardar ou deixar
     * @throws IllegalStateException se não houver kit por decidir ou a opção não for possível
     */
    public void decidirKit(DecisaoKit decisao) {
        if (!podeDecidir(decisao)) {
            throw new IllegalStateException("Essa opção não é possível agora.");
        }
        ItemCura kit = kitPorDecidir;
        switch (decisao) {
            case USAR: {
                long antes = toCruz.getVida();
                toCruz.usarItem(kit);
                relato.addToRear("Usou o kit: " + antes + " → " + toCruz.getVida() + " de vida.");
                eventos.addToRear(new Evento(Evento.Tipo.KIT_USADO, null, atual.getName(), atual.getName(), null,
                        kit.getVida_recuperada(), toCruz.getVida()));
                break;
            }
            case GUARDAR:
                toCruz.guardarKit(kit);
                relato.addToRear("Guardou o kit de " + kit.getVida_recuperada() + " na mochila ("
                        + toCruz.getMochila().size() + "/" + PlaneadorMissao.CAPACIDADE_MOCHILA + ").");
                eventos.addToRear(new Evento(Evento.Tipo.KIT_GUARDADO, null, atual.getName(), atual.getName(), null,
                        kit.getVida_recuperada(), toCruz.getVida()));
                break;
            default:
                relato.addToRear("Deixou o kit na divisão.");
                break;
        }
        kitPorDecidir = null;
        estado = Estado.A_JOGAR;
        if (faseInimigosPendente) {
            faseInimigos();
        }
        verificarAlarme();
    }

    /**
     * O Tó Cruz sai do edifício. A missão só tem sucesso se ele levar o alvo.
     *
     * @throws IllegalStateException se não estiver numa saída livre de inimigos
     */
    public void sair() {
        if (!podeSair()) {
            throw new IllegalStateException("O Tó Cruz só pode sair por uma entrada/saída sem inimigos.");
        }
        if (toCruz.isColectedAlvo()) {
            relato.addToRear("O Tó Cruz saiu do edifício com o alvo. Missão cumprida!");
        } else {
            relato.addToRear("O Tó Cruz saiu do edifício sem o alvo. Missão falhada.");
        }
        eventos.addToRear(new Evento(Evento.Tipo.SAIDA, null, atual.getName(), null, null, 0, toCruz.getVida()));
        terminar(toCruz.isColectedAlvo());
    }

    // ------------------------------------------------------------------ sugestões

    /**
     * Melhor caminho (menor dano) para o objetivo seguinte: o alvo, ou a saída mais segura depois
     * de o apanhar.
     *
     * @return as divisões do caminho, a começar na atual (vazio se o jogo não estiver a decorrer)
     */
    public ArrayUnorderedADT<Divisao> caminhoSugerido() {
        if (atual == null || estado == Estado.TERMINADO) {
            return new ArrayUnordered<>();
        }
        if (!toCruz.isColectedAlvo()) {
            return PlaneadorMissao.caminho(edificio, atual, divisaoDoAlvo());
        }
        return caminhoMaisBaratoPara(getEntradas());
    }

    /**
     * Melhor caminho para o kit de vida ainda disponível mais próximo (em dano).
     *
     * @return as divisões do caminho, ou vazio se já não houver kits
     */
    public ArrayUnorderedADT<Divisao> caminhoParaKit() {
        ArrayUnorderedADT<Divisao> comKit = new ArrayUnordered<>();
        if (atual != null && estado != Estado.TERMINADO) {
            Iterator<Divisao> divisoes = edificio.IteratorMapa();
            while (divisoes.hasNext()) {
                Divisao divisao = divisoes.next();
                if (divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected()
                        && ((ItemCura) divisao.getItem()).getType() == TypeItemCura.KIT_VIDA
                        && !divisao.equals(atual)) {
                    comKit.addToRear(divisao);
                }
            }
        }
        return caminhoMaisBaratoPara(comKit);
    }

    /** @return divisão onde está o alvo */
    public Divisao divisaoDoAlvo() {
        Iterator<Divisao> divisoes = edificio.IteratorMapa();
        while (divisoes.hasNext()) {
            Divisao divisao = divisoes.next();
            if (divisao.getAlvo() != null) {
                return divisao;
            }
        }
        throw new IllegalStateException("O edifício não tem alvo.");
    }

    // ------------------------------------------------------------------ regras internas

    private ArrayUnorderedADT<Divisao> caminhoMaisBaratoPara(ArrayUnorderedADT<Divisao> destinos) {
        ArrayUnorderedADT<Divisao> melhor = new ArrayUnordered<>();
        double melhorCusto = Double.MAX_VALUE;
        for (Divisao destino : destinos) {
            double custo = edificio.getShortestPath(atual, destino);
            ArrayUnorderedADT<Divisao> caminho = PlaneadorMissao.caminho(edificio, atual, destino);
            boolean maisCurto = custo == melhorCusto && caminho.size() < melhor.size();
            if (!caminho.isEmpty() && (custo < melhorCusto || maisCurto)) {
                melhor = caminho;
                melhorCusto = custo;
            }
        }
        return melhor;
    }

    /**
     * O Tó Cruz acabou de entrar numa divisão (fase do jogador).
     */
    private void chegarA(Divisao divisao) {
        trajeto.enqueue(divisao);
        relato.addToRear("Turno " + turno + ": o Tó Cruz entrou em " + divisao.getName() + ".");

        if (divisao.haveInimigo()) {
            // Cenário 1 e 5: o Tó Cruz tem a prioridade de ataque.
            ataqueDoToCruz();
            if (emCombate()) {
                faseInimigos();
            } else {
                tratarAlvoEItem(false);
            }
        } else {
            tratarAlvoEItem(true);
        }
    }

    /**
     * Alvo (cenário 6), coletes e kits de uma divisão sem inimigos.
     *
     * @param comFaseInimigos se o turno continua com a fase dos inimigos
     */
    private void tratarAlvoEItem(boolean comFaseInimigos) {
        if (atual.getAlvo() != null && !toCruz.isColectedAlvo()) {
            atual.ToCruzGetAlvo();
            relato.addToRear("O Tó Cruz tem o alvo (" + atual.getAlvo().getNome() + "). Agora tem de sair com vida.");
            eventos.addToRear(new Evento(Evento.Tipo.ALVO, atual.getAlvo().getNome(), atual.getName(), atual.getName(), null, 0, toCruz.getVida()));
            if (dificuldade.temAlarme()) {
                dispararAlarme();
            }
        }

        boolean aDecidir = false;
        if (atual.getItem() instanceof ItemCura && !atual.getItem().isCollected()) {
            ItemCura item = (ItemCura) atual.getItem();
            if (item.getType() == TypeItemCura.COLETE) {
                long antes = toCruz.getVida();
                toCruz.usarItem(item);
                relato.addToRear("Vestiu um colete: " + antes + " → " + toCruz.getVida() + " de vida.");
                eventos.addToRear(new Evento(Evento.Tipo.COLETE, null, atual.getName(), atual.getName(), null,
                        item.getVida_recuperada(), toCruz.getVida()));
            } else if (toCruz.getVida() < PlaneadorMissao.VIDA_INICIAL || !toCruz.mochilaIsFull()) {
                kitPorDecidir = item;
                estado = Estado.DECIDIR_KIT;
                aDecidir = true;
                relato.addToRear("Encontrou um kit de " + item.getVida_recuperada() + " pontos.");
            }
        }

        if (aDecidir) {
            faseInimigosPendente = comFaseInimigos;
        } else if (comFaseInimigos) {
            faseInimigos();
        }
    }

    /**
     * O alarme dá ao Tó Cruz os turnos do caminho mais curto até uma saída, mais uma folga.
     */
    private void dispararAlarme() {
        int maisPerto = Integer.MAX_VALUE;
        for (Divisao saida : getEntradas()) {
            int ligacoes = edificio.caminhoEmLigacoes(atual, saida).size() - 1;
            if (ligacoes >= 0) {
                maisPerto = Math.min(maisPerto, ligacoes);
            }
        }
        int turnos = (maisPerto == Integer.MAX_VALUE ? 0 : maisPerto) + Dificuldade.FOLGA_ALARME;
        turnoLimite = turno + turnos;
        relato.addToRear("ALARME! O Tó Cruz tem " + turnos + " turnos para sair do edifício.");
        eventos.addToRear(new Evento(Evento.Tipo.ALARME, null, atual.getName(), null, null, turnos, toCruz.getVida()));
    }

    /**
     * Com o alarme a tocar, a missão falha se os turnos acabarem e o Tó Cruz não puder sair já.
     */
    private void verificarAlarme() {
        if (estado == Estado.TERMINADO || turnoLimite < 0) {
            return;
        }
        boolean passou = turno > turnoLimite;
        boolean semTempo = turno == turnoLimite && estado == Estado.A_JOGAR && !podeSair();
        if (passou || semTempo) {
            relato.addToRear("O tempo acabou: chegaram reforços e o Tó Cruz ficou encurralado. Missão falhada.");
            eventos.addToRear(new Evento(Evento.Tipo.TEMPO_ESGOTADO, null, atual.getName(), null, null, 0, toCruz.getVida()));
            terminar(false);
        }
    }

    private void ataqueDoToCruz() {
        int total = atual.getInimigos().size();
        Inimigo[] alvos = new Inimigo[total];
        long[] vidaAntes = new long[total];
        int i = 0;
        for (Inimigo inimigo : atual.getInimigos()) {
            alvos[i] = inimigo;
            vidaAntes[i] = inimigo.getVida();
            i++;
        }

        int mortosAntes = inimigosMortos.size();
        atual.attackToCruz(inimigosMortos);
        edificio.atualizarCustoEntrada(atual);

        for (int j = 0; j < total; j++) {
            long depois = Math.max(0, alvos[j].getVida());
            eventos.addToRear(new Evento(Evento.Tipo.ATAQUE_TO, alvos[j].getNome(), atual.getName(), atual.getName(), null,
                    vidaAntes[j] - depois, depois));
            if (depois == 0) {
                eventos.addToRear(new Evento(Evento.Tipo.INIMIGO_ELIMINADO, alvos[j].getNome(), atual.getName(), atual.getName(), null, 0, 0));
            }
        }

        int mortos = inimigosMortos.size() - mortosAntes;
        StringBuilder texto = new StringBuilder("O Tó Cruz atacou");
        if (mortos > 0) {
            texto.append(" e eliminou ").append(mortos).append(mortos == 1 ? " inimigo" : " inimigos");
        }
        for (Inimigo inimigo : atual.getInimigos()) {
            texto.append("; ").append(inimigo.getNome()).append(" ficou com ").append(inimigo.getVida());
        }
        relato.addToRear(texto.append('.').toString());
    }

    /**
     * Fase dos inimigos: quem está em combate ataca, os outros movem-se e quem chegar à divisão
     * do Tó Cruz ataca logo (cenário 3).
     */
    private void faseInimigos() {
        faseInimigosPendente = false;

        for (Inimigo inimigo : atual.getInimigos()) {
            if (estado != Estado.TERMINADO) {
                sofrerAtaque(inimigo);
            }
        }
        if (estado == Estado.TERMINADO) {
            return;
        }

        UnorderedListADT<Inimigo> jaNaDivisao = new LinearLinkedUnorderedList<>();
        for (Inimigo inimigo : atual.getInimigos()) {
            jaNaDivisao.addToRear(inimigo);
        }

        UnorderedListADT<String> perseguidores = new LinearLinkedUnorderedList<>();
        for (Evento evento : movimento.moverInimigos(edificio, atual, isAlarmeAtivo())) {
            eventos.addToRear(evento);
            if (evento.getTipo() == Evento.Tipo.PERSEGUICAO) {
                perseguidores.addToRear(evento.getPersonagem());
                relato.addToRear(evento.getPersonagem() + " ouviu o Tó Cruz e avançou de " + evento.getOrigem()
                        + " para " + evento.getDestino() + ".");
            } else {
                relato.addToRear(evento.getPersonagem() + " passou de " + evento.getOrigem() + " para " + evento.getDestino() + ".");
            }
        }

        boolean emboscada = false;
        for (Inimigo inimigo : atual.getInimigos()) {
            if (!jaNaDivisao.contains(inimigo) && estado != Estado.TERMINADO) {
                if (perseguidores.contains(inimigo.getNome())) {
                    // Nível Elite: o Tó Cruz ouviu-o aproximar-se e estava à espera.
                    relato.addToRear(inimigo.getNome() + " entrou em " + atual.getName() + ", mas o Tó Cruz estava à espera.");
                    emboscada = true;
                } else {
                    relato.addToRear(inimigo.getNome() + " entrou em " + atual.getName() + " e atacou primeiro.");
                    sofrerAtaque(inimigo);
                }
            }
        }
        if (emboscada && estado != Estado.TERMINADO && atual.haveInimigo()) {
            ataqueDoToCruz();
            if (!emCombate()) {
                tratarAlvoEItem(false);
            }
        }
    }

    private void sofrerAtaque(Inimigo inimigo) {
        long vida = Math.max(0, toCruz.getVida() - inimigo.getPoder());
        long dano = toCruz.getVida() - vida;
        toCruz.setVida(vida);
        relato.addToRear(inimigo.getNome() + " atacou: o Tó Cruz ficou com " + vida + " de vida.");
        eventos.addToRear(new Evento(Evento.Tipo.ATAQUE_INIMIGO, inimigo.getNome(), atual.getName(), atual.getName(), null, dano, vida));
        if (toCruz.isDead()) {
            relato.addToRear("O Tó Cruz morreu. Missão falhada.");
            eventos.addToRear(new Evento(Evento.Tipo.MORTE, null, atual.getName(), null, null, 0, 0));
            terminar(false);
        }
    }

    private void terminar(boolean comSucesso) {
        this.sucesso = comSucesso && !toCruz.isDead();
        this.estado = Estado.TERMINADO;
    }

    private void exigirEstado(Estado esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("Ação impossível neste momento do jogo (" + estado + ").");
        }
    }
}
