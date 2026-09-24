package tocruz.gui;

import ed.arraylist.ArrayUnordered;
import ed.interfaces.ArrayUnorderedADT;
import ed.interfaces.UnorderedListADT;
import ed.queue.LinkedQueue;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Random;
import tocruz.items.ItemCura;
import tocruz.mapa.Divisao;
import tocruz.motor.Evento;
import tocruz.motor.MotorJogo;
import tocruz.personagens.Inimigo;

/**
 * O que se vê no mapa e as animações que levam do estado anterior ao atual.
 *
 * <p>Depois de cada ação, o motor já está no estado final; os eventos que produziu são
 * transformados numa fila de passos (andar, disparar, cair, curar...) que são animados um de cada
 * vez. Quando a fila acaba, a cena volta a ser sincronizada com o motor, para garantir que o
 * ecrã mostra exatamente o estado do jogo.</p>
 *
 * @author Francisco Oliveira
 */
final class Cena {

    private static final double VELOCIDADE = 0.40;

    final Planta planta;
    private final ArrayUnorderedADT<Figura> figuras = new ArrayUnordered<>();
    private final ArrayUnorderedADT<Efeito> efeitos = new ArrayUnordered<>();
    private final LinkedQueue<Passo> fila = new LinkedQueue<>();
    private final Random aleatorio = new Random(11);
    private Passo atual;
    private double tempoPasso;

    /** Tempo total em milissegundos (para as pulsações). */
    double relogio;
    /** Intensidade do tremor do ecrã e do vermelho nas margens quando o Tó Cruz é atingido. */
    double tremor;
    double vinheta;
    /** Faixa com uma mensagem grande no centro do ecrã. */
    String faixa;
    Color corFaixa = Tema.VERMELHO;
    private double faixaRestante;
    private double faixaDuracao;

    final boolean[] itemVisivel;
    final ItemCura[] item;
    int salaObjetivo = -1;
    String nomeObjetivo = "";
    boolean objetivoVisivel;
    boolean alarme;
    /** No nível Elite só se veem os inimigos a duas divisões ou menos do Tó Cruz. */
    boolean nevoeiro;

    Cena(Planta planta) {
        this.planta = planta;
        this.itemVisivel = new boolean[planta.nomes.length];
        this.item = new ItemCura[planta.nomes.length];
    }

    // ------------------------------------------------------------------ estado

    ArrayUnorderedADT<Figura> figuras() {
        return figuras;
    }

    ArrayUnorderedADT<Efeito> efeitos() {
        return efeitos;
    }

    Figura toCruz() {
        return figura("\u0000ToCruz");
    }

    Figura figura(String nome) {
        for (Figura f : figuras) {
            if (f.nome.equals(nome)) {
                return f;
            }
        }
        return null;
    }

    boolean ocupada() {
        return atual != null || !fila.isEmpty();
    }

    /** Estado inicial de uma simulação nova. */
    void carregar(MotorJogo motor) {
        while (!figuras.isEmpty()) {
            figuras.removeFirst();
        }
        while (!efeitos.isEmpty()) {
            efeitos.removeFirst();
        }
        while (!fila.isEmpty()) {
            fila.dequeue();
        }
        atual = null;
        faixa = null;
        tremor = 0;
        vinheta = 0;
        Figura to = new Figura("\u0000ToCruz", true);
        to.visivel = false;
        to.vidaMaxima = 100;
        figuras.addToRear(to);
        nevoeiro = motor.getDificuldade().temPerseguicao();
        sincronizar(motor);
        for (Figura f : figuras) {
            if (!f.toCruz) {
                Point2D p = posicaoLivre(f);
                f.x = p.getX();
                f.y = p.getY();
                f.angulo = aleatorio.nextDouble() * Math.PI * 2;
                f.alfa = f.alfaAlvo;
            }
        }
    }

    /** Acerta a cena com o motor (no fim de cada animação). */
    void sincronizar(MotorJogo motor) {
        for (int i = 0; i < planta.nomes.length; i++) {
            Divisao divisao = motor.getEdificio().getDivisao(planta.nomes[i]);
            item[i] = divisao.getItem() instanceof ItemCura ? (ItemCura) divisao.getItem() : null;
            itemVisivel[i] = item[i] != null && !item[i].isCollected();
            if (divisao.getAlvo() != null) {
                salaObjetivo = i;
                nomeObjetivo = divisao.getAlvo().getNome();
                objetivoVisivel = !divisao.getAlvo().isAtinigido();
            }
            for (Inimigo inimigo : divisao.getInimigos()) {
                Figura f = figura(inimigo.getNome());
                if (f == null) {
                    f = new Figura(inimigo.getNome(), false);
                    f.vidaMaxima = 100;
                    figuras.addToRear(f);
                }
                f.sala = divisao.getName();
                f.vida = inimigo.getVida();
                f.poder = inimigo.getPoder();
                f.morto = false;
                f.queda = 0;
            }
        }
        // Inimigos que já não estão no edifício foram eliminados.
        for (Figura f : figuras) {
            if (!f.toCruz && !f.morto && !existe(motor, f.nome)) {
                f.morto = true;
                f.queda = Math.PI / 2;
            }
        }
        Figura to = toCruz();
        to.vida = motor.getToCruz().getVida();
        to.vidaMaxima = Math.max(100, to.vida);
        to.comObjetivo = motor.getToCruz().isColectedAlvo();
        if (motor.getDivisaoAtual() != null) {
            to.sala = motor.getDivisaoAtual().getName();
        }
        boolean saiu = motor.getEstado() == MotorJogo.Estado.TERMINADO && motor.getToCruz().getVida() > 0
                && motor.getDivisaoAtual() != null && !motor.getDivisaoAtual().haveInimigo() && to.alfa < 0.05;
        to.visivel = motor.getDivisaoAtual() != null && !saiu;
        to.morto = motor.getToCruz().getVida() <= 0 && motor.getDivisaoAtual() != null;
        if (to.morto) {
            to.queda = Math.PI / 2;
        }
        alarme = motor.isAlarmeAtivo() && motor.getEstado() != MotorJogo.Estado.TERMINADO;
    }

    private static boolean existe(MotorJogo motor, String nome) {
        Iterator<Divisao> it = motor.getEdificio().IteratorMapa();
        while (it.hasNext()) {
            for (Inimigo inimigo : it.next().getInimigos()) {
                if (inimigo.getNome().equals(nome)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Põe cada figura parada no seu lugar, sem animação. */
    void assentar() {
        for (Figura f : figuras) {
            if (f.visivel && !f.morto && !f.emMovimento) {
                Point2D p = posicaoLivre(f);
                f.x = p.getX();
                f.y = p.getY();
            }
            if (!f.toCruz) {
                f.alfaAlvo = escondida(f.sala) ? 0 : (f.morto ? 0.55 : 1);
            }
            f.alfa = f.alfaAlvo;
        }
    }

    // ------------------------------------------------------------------ posições

    /** Onde uma figura fica dentro da sala onde está, conforme quem mais lá está. */
    Point2D posicaoLivre(Figura f) {
        int i = planta.indice(f.sala);
        if (i < 0) {
            return new Point2D.Double(f.x, f.y);
        }
        Rectangle2D r = Desenho.interior(planta.salas[i]);
        Figura to = toCruz();
        boolean comToCruz = to.visivel && !to.morto && f.sala.equals(to.sala);
        int inimigos = 0;
        int ordem = 0;
        for (Figura o : figuras) {
            if (!o.toCruz && !o.morto && f.sala.equals(o.sala)) {
                if (o != f && o.nome.compareTo(f.nome) < 0) {
                    ordem++;
                }
                inimigos++;
            }
        }
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        double w = r.getWidth();
        double h = r.getHeight();
        if (f.toCruz) {
            return inimigos > 0 ? new Point2D.Double(cx - 0.26 * w, cy + 0.1 * h) : new Point2D.Double(cx - 0.04 * w, cy + 0.12 * h);
        }
        double[][] lugares = comToCruz
                ? new double[][]{{0.18, -0.08}, {0.32, 0.26}, {0.02, -0.3}, {0.06, 0.32}, {0.36, -0.3}, {-0.04, 0.02}}
                : new double[][]{{-0.12, 0.06}, {0.22, -0.12}, {0.08, 0.3}, {0.34, 0.24}, {-0.3, -0.14}, {-0.3, 0.3}};
        double[] l = lugares[ordem % lugares.length];
        return new Point2D.Double(cx + l[0] * w, cy + l[1] * h);
    }

    Point2D posicaoItem(int sala) {
        Rectangle2D r = Desenho.interior(planta.salas[sala]);
        return new Point2D.Double(r.getX() + 0.17 * r.getWidth(), r.getY() + 0.8 * r.getHeight());
    }

    Point2D posicaoObjetivo(int sala) {
        Rectangle2D r = Desenho.interior(planta.salas[sala]);
        return new Point2D.Double(r.getX() + 0.82 * r.getWidth(), r.getY() + 0.74 * r.getHeight());
    }

    /** Se uma sala está às escuras para o Tó Cruz (só no nível Elite). */
    boolean escondida(String sala) {
        Figura to = toCruz();
        if (!nevoeiro || !to.visivel || to.sala == null || sala == null) {
            return false;
        }
        int a = planta.indice(to.sala);
        int b = planta.indice(sala);
        return a >= 0 && b >= 0 && planta.distancia[a][b] > tocruz.motor.Dificuldade.RAIO_DETECAO;
    }

    // ------------------------------------------------------------------ tempo

    /** Avança as animações {@code dt} milissegundos. */
    void avancar(double dt) {
        relogio += dt;
        tremor = Math.max(0, tremor - dt / 260);
        vinheta = Math.max(0, vinheta - dt / 600);
        if (faixa != null) {
            faixaRestante -= dt;
            if (faixaRestante <= 0) {
                faixa = null;
            }
        }

        for (int k = efeitos.size() - 1; k >= 0; k--) {
            Efeito e = efeitos.find(k);
            if (e.avancar(dt)) {
                efeitos.remove(e);
            }
        }

        double restante = dt;
        while (restante > 0) {
            if (atual == null) {
                if (fila.isEmpty()) {
                    break;
                }
                atual = fila.dequeue();
                tempoPasso = 0;
                atual.iniciar();
            }
            double falta = atual.duracao - tempoPasso;
            double usado = Math.min(falta, restante);
            tempoPasso += usado;
            restante -= usado;
            atual.atualizar(tempoPasso);
            if (tempoPasso >= atual.duracao) {
                atual.terminar();
                atual = null;
            }
        }

        // Quem está parado encaminha-se para o seu lugar e vira-se para o adversário.
        Figura to = toCruz();
        double fator = Math.min(1, dt / 90);
        for (Figura f : figuras) {
            f.flash = Math.max(0, f.flash - dt / 160);
            f.alerta = Math.max(0, f.alerta - dt);
            if (!f.toCruz) {
                f.alfaAlvo = escondida(f.sala) ? 0 : (f.morto ? 0.55 : 1);
            }
            f.alfa += (f.alfaAlvo - f.alfa) * Math.min(1, dt / 160);
            if (f.emMovimento || f.morto || !f.visivel) {
                continue;
            }
            Point2D p = posicaoLivre(f);
            f.x += (p.getX() - f.x) * fator;
            f.y += (p.getY() - f.y) * fator;
            if (!f.toCruz && to.visivel && !to.morto && f.sala.equals(to.sala)) {
                f.virarPara(to.x, to.y);
            }
        }
        if (to.visivel && !to.morto && !to.emMovimento) {
            Figura alvo = inimigoMaisPerto(to);
            if (alvo != null) {
                to.virarPara(alvo.x, alvo.y);
            }
        }
    }

    private Figura inimigoMaisPerto(Figura to) {
        Figura melhor = null;
        for (Figura f : figuras) {
            if (!f.toCruz && !f.morto && f.sala != null && f.sala.equals(to.sala)
                    && (melhor == null || Math.hypot(f.x - to.x, f.y - to.y) < Math.hypot(melhor.x - to.x, melhor.y - to.y))) {
                melhor = f;
            }
        }
        return melhor;
    }

    // ------------------------------------------------------------------ eventos → passos

    /** Transforma os eventos de uma ação em passos animados. */
    void acrescentar(UnorderedListADT<Evento> eventos) {
        Mover grupo = null;
        for (Evento e : eventos) {
            boolean movimentoInimigo = e.getTipo() == Evento.Tipo.MOVIMENTO_INIMIGO || e.getTipo() == Evento.Tipo.PERSEGUICAO;
            if (movimentoInimigo) {
                Figura f = figura(e.getPersonagem());
                if (f != null) {
                    if (grupo == null) {
                        grupo = new Mover();
                    }
                    grupo.juntar(f, e.getCaminho(), e.getTipo() == Evento.Tipo.PERSEGUICAO);
                }
                continue;
            }
            if (grupo != null) {
                fila.enqueue(grupo);
                grupo = null;
            }
            Figura to = toCruz();
            switch (e.getTipo()) {
                case ENTRADA:
                    fila.enqueue(new Entrada(e.getDestino()));
                    break;
                case MOVIMENTO_TO: {
                    Mover m = new Mover();
                    m.juntar(to, e.getCaminho(), false);
                    fila.enqueue(m);
                    break;
                }
                case ATAQUE_TO: {
                    Figura alvo = figura(e.getPersonagem());
                    if (alvo != null) {
                        fila.enqueue(new Disparo(to, alvo, e.getValor(), e.getRestante(), 3, Tema.CIANO));
                    }
                    break;
                }
                case INIMIGO_ELIMINADO: {
                    Figura alvo = figura(e.getPersonagem());
                    if (alvo != null) {
                        fila.enqueue(new Morte(alvo));
                    }
                    break;
                }
                case ATAQUE_INIMIGO: {
                    Figura atirador = figura(e.getPersonagem());
                    if (atirador != null) {
                        fila.enqueue(new Disparo(atirador, to, e.getValor(), e.getRestante(), 2, Tema.VERMELHO));
                    }
                    break;
                }
                case KIT_USADO:
                    fila.enqueue(new Cura(e.getOrigem(), "+" + e.getValor(), e.getRestante(), Tema.VERDE));
                    break;
                case KIT_GUARDADO:
                    fila.enqueue(new Cura(e.getOrigem(), "KIT GUARDADO", e.getRestante(), Tema.VERDE));
                    break;
                case COLETE:
                    fila.enqueue(new Cura(e.getOrigem(), "+" + e.getValor() + " COLETE", e.getRestante(), Tema.AZUL));
                    break;
                case ALVO:
                    fila.enqueue(new ApanharObjetivo());
                    break;
                case ALARME:
                    fila.enqueue(new Faixa("ALARME! " + e.getValor() + " TURNOS PARA SAIR", Tema.VERMELHO, 1300, true));
                    break;
                case SAIDA:
                    fila.enqueue(new Saida(e.getOrigem()));
                    break;
                case MORTE:
                    fila.enqueue(new Morte(to));
                    break;
                case TEMPO_ESGOTADO:
                    fila.enqueue(new Faixa("TEMPO ESGOTADO", Tema.VERMELHO, 1300, false));
                    break;
                default:
                    break;
            }
        }
        if (grupo != null) {
            fila.enqueue(grupo);
        }
    }

    private double semente() {
        return aleatorio.nextDouble();
    }

    private static double suave(double p) {
        return p * p * (3 - 2 * p);
    }

    // ------------------------------------------------------------------ passos

    /** Um passo da animação. */
    private abstract class Passo {
        double duracao;

        Passo(double duracao) {
            this.duracao = duracao;
        }

        void iniciar() {
        }

        /** @param t milissegundos desde o início do passo */
        abstract void atualizar(double t);

        void terminar() {
        }
    }

    /** Uma ou mais figuras a andar pelos corredores (em simultâneo). */
    private final class Mover extends Passo {
        private final ArrayUnorderedADT<Figura> quem = new ArrayUnordered<>();
        private final ArrayUnorderedADT<String[]> salas = new ArrayUnordered<>();
        private final ArrayUnorderedADT<Boolean> alertas = new ArrayUnordered<>();
        private double[][] trajetos;
        private double[] comprimentos;

        Mover() {
            super(0);
        }

        void juntar(Figura f, String[] caminho, boolean alerta) {
            quem.addToRear(f);
            salas.addToRear(caminho);
            alertas.addToRear(alerta);
        }

        @Override
        void iniciar() {
            int n = quem.size();
            trajetos = new double[n][];
            comprimentos = new double[n];
            // Primeiro muda-se a sala de todos, para os lugares finais já contarem com todos.
            for (int k = 0; k < n; k++) {
                String[] c = salas.find(k);
                quem.find(k).sala = c[c.length - 1];
            }
            double maior = 0;
            for (int k = 0; k < n; k++) {
                Figura f = quem.find(k);
                f.emMovimento = true;
                if (alertas.find(k)) {
                    f.alerta = 1100;
                }
                trajetos[k] = trajeto(f, salas.find(k));
                comprimentos[k] = comprimento(trajetos[k]);
                maior = Math.max(maior, comprimentos[k]);
            }
            duracao = Math.max(380, Math.min(1500, maior / VELOCIDADE));
        }

        @Override
        void atualizar(double t) {
            double p = suave(Math.min(1, t / duracao));
            for (int k = 0; k < quem.size(); k++) {
                colocar(quem.find(k), trajetos[k], comprimentos[k] * p);
            }
        }

        @Override
        void terminar() {
            for (Figura f : quem) {
                f.emMovimento = false;
            }
        }

        private double[] trajeto(Figura f, String[] caminho) {
            double[] pontos = {f.x, f.y};
            for (int k = 0; k + 1 < caminho.length; k++) {
                int a = planta.indice(caminho[k]);
                int b = planta.indice(caminho[k + 1]);
                double[] c = a >= 0 && b >= 0 ? planta.corredor(a, b) : null;
                if (c != null) {
                    pontos = juntarPontos(pontos, c);
                }
                if (k + 2 < caminho.length && b >= 0) {
                    Point2D centro = planta.centro(b);
                    pontos = juntarPontos(pontos, new double[]{centro.getX(), centro.getY()});
                }
            }
            Point2D fim = posicaoLivre(f);
            return juntarPontos(pontos, new double[]{fim.getX(), fim.getY()});
        }
    }

    /** O Tó Cruz entra pela porta exterior. */
    private final class Entrada extends Passo {
        private final String sala;
        private double[] trajeto;
        private double total;

        Entrada(String sala) {
            super(900);
            this.sala = sala;
        }

        @Override
        void iniciar() {
            Figura to = toCruz();
            int i = planta.indice(sala);
            double[] porta = planta.portaExterior[i];
            to.sala = sala;
            to.visivel = true;
            to.emMovimento = true;
            to.alfa = 0;
            to.alfaAlvo = 1;
            Point2D fim = posicaoLivre(to);
            trajeto = porta == null
                    ? new double[]{fim.getX(), fim.getY(), fim.getX(), fim.getY()}
                    : new double[]{porta[2], porta[3], porta[0], porta[1], fim.getX(), fim.getY()};
            total = comprimento(trajeto);
            to.x = trajeto[0];
            to.y = trajeto[1];
        }

        @Override
        void atualizar(double t) {
            colocar(toCruz(), trajeto, total * suave(Math.min(1, t / duracao)));
        }

        @Override
        void terminar() {
            toCruz().emMovimento = false;
        }
    }

    /** O Tó Cruz sai pela porta exterior. */
    private final class Saida extends Passo {
        private final String sala;
        private double[] trajeto;
        private double total;

        Saida(String sala) {
            super(900);
            this.sala = sala;
        }

        @Override
        void iniciar() {
            Figura to = toCruz();
            double[] porta = planta.portaExterior[planta.indice(sala)];
            to.emMovimento = true;
            trajeto = porta == null
                    ? new double[]{to.x, to.y, to.x, to.y}
                    : new double[]{to.x, to.y, porta[0], porta[1], porta[2], porta[3]};
            total = comprimento(trajeto);
        }

        @Override
        void atualizar(double t) {
            double p = Math.min(1, t / duracao);
            colocar(toCruz(), trajeto, total * suave(p));
            toCruz().alfaAlvo = p > 0.65 ? 0 : 1;
        }

        @Override
        void terminar() {
            Figura to = toCruz();
            to.emMovimento = false;
            to.alfa = 0;
        }
    }

    /** Rajada de tiros de uma figura contra outra. */
    private final class Disparo extends Passo {
        private final Figura atirador;
        private final Figura alvo;
        private final long dano;
        private final long restante;
        private final int balas;
        private final Color cor;
        private final boolean[] disparou;
        private final boolean[] acertou;

        Disparo(Figura atirador, Figura alvo, long dano, long restante, int balas, Color cor) {
            super(200 + balas * 85 + 180);
            this.atirador = atirador;
            this.alvo = alvo;
            this.dano = dano;
            this.restante = restante;
            this.balas = balas;
            this.cor = cor;
            this.disparou = new boolean[balas];
            this.acertou = new boolean[balas];
        }

        @Override
        void iniciar() {
            atirador.virarPara(alvo.x, alvo.y);
        }

        @Override
        void atualizar(double t) {
            atirador.virarPara(alvo.x, alvo.y);
            for (int k = 0; k < balas; k++) {
                double momento = 130 + k * 85;
                if (!disparou[k] && t >= momento) {
                    disparou[k] = true;
                    double jx = (semente() - 0.5) * 10;
                    double jy = (semente() - 0.5) * 10;
                    efeitos.addToRear(new Efeito.Rasto(atirador.bocaX(), atirador.bocaY(), alvo.x + jx, alvo.y + jy, cor));
                    efeitos.addToRear(new Efeito.Clarao(atirador.bocaX(), atirador.bocaY(), atirador.angulo));
                }
                if (!acertou[k] && t >= momento + 70) {
                    acertou[k] = true;
                    efeitos.addToRear(new Efeito.Impacto(alvo.x, alvo.y, alvo.toCruz ? Tema.VERMELHO : new Color(0xFFB347),
                            (long) (semente() * 1000)));
                    alvo.flash = 1;
                    if (alvo.toCruz) {
                        tremor = 1;
                        vinheta = 1;
                    }
                    if (k == balas - 1) {
                        alvo.vida = restante;
                        efeitos.addToRear(new Efeito.Texto("-" + dano, alvo.x + 18, alvo.y - 50,
                                alvo.toCruz ? new Color(0xFF6B6B) : new Color(0xFFE08A), 20f, 950));
                    }
                }
            }
        }
    }

    /** Uma figura cai (eliminada). */
    private final class Morte extends Passo {
        private final Figura f;

        Morte(Figura f) {
            super(560);
            this.f = f;
        }

        @Override
        void iniciar() {
            f.morto = true;
            f.vida = 0;
            if (!f.toCruz) {
                efeitos.addToRear(new Efeito.Texto("ELIMINADO", f.x, f.y - 50, new Color(0xE6EEF7), 13f, 900));
            }
        }

        @Override
        void atualizar(double t) {
            double p = suave(Math.min(1, t / duracao));
            f.queda = p * Math.PI / 2;
            f.alfa = 1 - 0.45 * p;
        }
    }

    /** Kit, colete ou kit guardado. */
    private final class Cura extends Passo {
        private final String sala;
        private final String texto;
        private final long vida;
        private final Color cor;

        Cura(String sala, String texto, long vida, Color cor) {
            super(620);
            this.sala = sala;
            this.texto = texto;
            this.vida = vida;
            this.cor = cor;
        }

        @Override
        void iniciar() {
            Figura to = toCruz();
            to.vida = vida;
            to.vidaMaxima = Math.max(100, vida);
            int i = planta.indice(sala);
            if (i >= 0) {
                itemVisivel[i] = false;
            }
            efeitos.addToRear(new Efeito.Anel(to.x, to.y, cor, 46));
            efeitos.addToRear(new Efeito.Texto(texto, to.x, to.y - 56, cor, 17f, 1000));
        }

        @Override
        void atualizar(double t) {
        }
    }

    /** O Tó Cruz apanha o objetivo. */
    private final class ApanharObjetivo extends Passo {
        ApanharObjetivo() {
            super(900);
        }

        @Override
        void iniciar() {
            Figura to = toCruz();
            objetivoVisivel = false;
            to.comObjetivo = true;
            efeitos.addToRear(new Efeito.Anel(to.x, to.y, Tema.DOURADO, 90));
            efeitos.addToRear(new Efeito.Texto("OBJETIVO RECUPERADO", to.x, to.y - 62, Tema.DOURADO, 18f, 1600));
        }

        @Override
        void atualizar(double t) {
        }
    }

    /** Mensagem grande a meio do ecrã. */
    private final class Faixa extends Passo {
        private final String texto;
        private final Color cor;
        private final boolean ligaAlarme;

        Faixa(String texto, Color cor, double duracao, boolean ligaAlarme) {
            super(duracao);
            this.texto = texto;
            this.cor = cor;
            this.ligaAlarme = ligaAlarme;
        }

        @Override
        void iniciar() {
            faixa = texto;
            corFaixa = cor;
            faixaDuracao = duracao + 600;
            faixaRestante = faixaDuracao;
            if (ligaAlarme) {
                alarme = true;
            }
        }

        @Override
        void atualizar(double t) {
        }
    }

    /** @return progresso da faixa (0 a 1) para a animação de entrada e saída */
    double progressoFaixa() {
        return faixaDuracao <= 0 ? 1 : 1 - faixaRestante / faixaDuracao;
    }

    // ------------------------------------------------------------------ geometria dos trajetos

    private static double[] juntarPontos(double[] a, double[] b) {
        double[] r = new double[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static double comprimento(double[] p) {
        double total = 0;
        for (int k = 2; k < p.length; k += 2) {
            total += Math.hypot(p[k] - p[k - 2], p[k + 1] - p[k - 1]);
        }
        return total;
    }

    /** Põe a figura a uma distância {@code s} do início do trajeto, virada para onde anda. */
    private static void colocar(Figura f, double[] p, double s) {
        double resto = s;
        for (int k = 2; k < p.length; k += 2) {
            double troco = Math.hypot(p[k] - p[k - 2], p[k + 1] - p[k - 1]);
            if (resto <= troco || k == p.length - 2) {
                double fr = troco == 0 ? 1 : Math.min(1, resto / troco);
                f.x = p[k - 2] + (p[k] - p[k - 2]) * fr;
                f.y = p[k - 1] + (p[k + 1] - p[k - 1]) * fr;
                if (troco > 0.5) {
                    f.angulo = Math.atan2(p[k + 1] - p[k - 1], p[k] - p[k - 2]);
                }
                return;
            }
            resto -= troco;
        }
    }
}
