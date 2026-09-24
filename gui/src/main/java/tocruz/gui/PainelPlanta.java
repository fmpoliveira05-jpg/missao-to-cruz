package tocruz.gui;

import ed.interfaces.ArrayUnorderedADT;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Divisao;
import tocruz.motor.Dificuldade;
import tocruz.motor.MotorJogo;
import tocruz.motor.Plano;
import tocruz.personagens.Inimigo;

/**
 * O mapa: a planta vista de cima, as personagens, as animações e a informação por cima (objetivo,
 * alarme, escolha da dificuldade e resultado final).
 *
 * @author Francisco Oliveira
 */
class PainelPlanta extends JComponent {

    private static final long serialVersionUID = 1L;
    /** Tamanho das personagens e objetos em relação à planta. */
    static final double ESCALA_FIGURA = 1.7;

    /** Quem recebe os cliques. */
    interface Ouvinte {
        void clicouSala(Divisao divisao);

        void escolheuDificuldade(Dificuldade dificuldade);
    }

    private transient Planta planta;
    private transient Cena cena;
    private transient MotorJogo motor;
    private transient Plano plano;
    private transient Ouvinte ouvinte;
    /** A planta desenhada uma só vez, com esta resolução (pixels por unidade da planta). */
    private static final double RESOLUCAO_FUNDO = 1.5;
    /** Zoom da câmara quando segue o Tó Cruz. */
    private static final double ZOOM_PERTO = 1.45;

    private transient BufferedImage fundo;
    private double camX = Double.NaN;
    private double camY;
    private double camZoom;
    private boolean seguir = true;

    private String nomeMissao = "";
    private boolean escolherDificuldade = true;
    private boolean mostrarSugestao = true;
    private boolean mostrarKit = true;
    private int salaSobre = -1;
    private int cartaoSobre = -1;

    PainelPlanta() {
        setPreferredSize(new Dimension(1000, 720));
        setOpaque(true);
        setFocusable(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter rato = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int sala = escolherDificuldade ? -1 : salaEm(e.getX(), e.getY());
                int cartao = escolherDificuldade ? cartaoEm(e.getX(), e.getY()) : -1;
                if (sala != salaSobre || cartao != cartaoSobre) {
                    salaSobre = sala;
                    cartaoSobre = cartao;
                    setCursor(Cursor.getPredefinedCursor(sala >= 0 || cartao >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                salaSobre = -1;
                cartaoSobre = -1;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                if (ouvinte == null) {
                    return;
                }
                if (escolherDificuldade) {
                    int cartao = cartaoEm(e.getX(), e.getY());
                    if (cartao >= 0) {
                        ouvinte.escolheuDificuldade(Dificuldade.values()[cartao]);
                    }
                    return;
                }
                int sala = salaEm(e.getX(), e.getY());
                if (sala >= 0 && motor != null) {
                    ouvinte.clicouSala(motor.getEdificio().getDivisao(planta.nomes[sala]));
                }
            }
        };
        addMouseListener(rato);
        addMouseMotionListener(rato);
    }

    void setOuvinte(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
    }

    void setPlanta(Planta planta) {
        this.planta = planta;
        this.fundo = null;
        this.camX = Double.NaN;
    }

    void setNomeMissao(String nome) {
        this.nomeMissao = nome;
    }

    void setJogo(MotorJogo motor, Cena cena) {
        this.motor = motor;
        this.cena = cena;
        this.plano = null;
    }

    void setEscolherDificuldade(boolean escolher) {
        this.escolherDificuldade = escolher;
    }

    boolean isEscolherDificuldade() {
        return escolherDificuldade;
    }

    void setPlano(Plano plano) {
        this.plano = plano;
    }

    Plano getPlano() {
        return plano;
    }

    void setMostrarSugestao(boolean mostrar) {
        this.mostrarSugestao = mostrar;
    }

    void setMostrarKit(boolean mostrar) {
        this.mostrarKit = mostrar;
    }

    void setSeguir(boolean seguir) {
        this.seguir = seguir;
    }

    // ------------------------------------------------------------------ câmara

    /** Na vista geral reserva-se uma faixa em cima (objetivo) e em baixo (ajuda); de perto usa-se tudo. */
    private double topo() {
        return vistaDePerto() ? 0 : 96;
    }

    private double alturaUtil() {
        return vistaDePerto() ? getHeight() : getHeight() - 96 - 48;
    }

    private double zoomGeral() {
        return Math.min((getWidth() - 20) / planta.largura, (getHeight() - 96 - 48) / planta.altura);
    }

    /** A câmara segue o Tó Cruz de perto; antes de entrar e no fim mostra o edifício inteiro. */
    private boolean vistaDePerto() {
        if (!seguir || escolherDificuldade || motor == null || cena == null) {
            return false;
        }
        Figura to = cena.toCruz();
        boolean terminou = motor.getEstado() == MotorJogo.Estado.TERMINADO && !cena.ocupada();
        return to.visivel && !terminou && zoomGeral() < ZOOM_PERTO * 0.9;
    }

    /** Aproxima a câmara do sítio onde deve estar (chamado a cada imagem). */
    void avancarCamera(double dt) {
        if (planta == null || getWidth() < 50) {
            return;
        }
        double alvoX;
        double alvoY;
        double alvoZoom;
        if (vistaDePerto()) {
            Figura to = cena.toCruz();
            alvoZoom = ZOOM_PERTO;
            alvoX = limitarCamera(to.x, getWidth() / 2.0 / alvoZoom, planta.largura);
            alvoY = limitarCamera(to.y, alturaUtil() / 2.0 / alvoZoom, planta.altura);
        } else {
            alvoZoom = zoomGeral();
            alvoX = planta.largura / 2;
            alvoY = planta.altura / 2;
        }
        if (Double.isNaN(camX)) {
            camX = alvoX;
            camY = alvoY;
            camZoom = alvoZoom;
            return;
        }
        double f = Math.min(1, dt / 220);
        camX += (alvoX - camX) * f;
        camY += (alvoY - camY) * f;
        camZoom += (alvoZoom - camZoom) * f;
    }

    /** Não deixa a câmara mostrar demasiado espaço vazio à volta do edifício. */
    private static double limitarCamera(double centro, double meiaVista, double tamanho) {
        if (tamanho <= 2 * meiaVista) {
            return tamanho / 2;
        }
        double folga = Planta.MARGEM - 24;
        return Math.max(meiaVista + folga, Math.min(tamanho - meiaVista - folga, centro));
    }

    /** Coloca a câmara já no sítio final (usado nas capturas). */
    void assentarCamera() {
        camX = Double.NaN;
        avancarCamera(16);
        camX = Double.NaN;
        for (int k = 0; k < 2; k++) {
            avancarCamera(1000);
        }
    }

    // ------------------------------------------------------------------ geometria do ecrã

    private AffineTransform transformacao() {
        if (Double.isNaN(camX)) {
            avancarCamera(16);
        }
        AffineTransform t = new AffineTransform();
        t.translate(getWidth() / 2.0, topo() + alturaUtil() / 2);
        t.scale(camZoom, camZoom);
        t.translate(-camX, -camY);
        return t;
    }

    private Point2D paraMundo(int x, int y) {
        try {
            return transformacao().inverseTransform(new Point2D.Double(x, y), null);
        } catch (NoninvertibleTransformException e) {
            return new Point2D.Double(-1, -1);
        }
    }

    private int salaEm(int x, int y) {
        if (planta == null) {
            return -1;
        }
        Point2D p = paraMundo(x, y);
        return planta.salaEm(p.getX(), p.getY());
    }

    private Rectangle2D cartao(int k) {
        double w = Math.min(270, (getWidth() - 120) / 3.0);
        double h = 230;
        double total = 3 * w + 2 * 24;
        double x = (getWidth() - total) / 2 + k * (w + 24);
        double y = getHeight() / 2.0 - h / 2 + 30;
        return new Rectangle2D.Double(x, y, w, h);
    }

    private int cartaoEm(int x, int y) {
        for (int k = 0; k < 3; k++) {
            if (cartao(k).contains(x, y)) {
                return k;
            }
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (escolherDificuldade || motor == null) {
            return null;
        }
        int i = salaEm(e.getX(), e.getY());
        if (i < 0) {
            return null;
        }
        Divisao divisao = motor.getEdificio().getDivisao(planta.nomes[i]);
        StringBuilder texto = new StringBuilder("<html><b>").append(divisao.getName()).append("</b>");
        if (cena != null && cena.escondida(divisao.getName())) {
            texto.append("<br>Fora de alcance: não se sabe quem lá está.");
        } else {
            for (Inimigo inimigo : divisao.getInimigos()) {
                texto.append("<br>").append(inimigo.getNome()).append(": vida ").append(inimigo.getVida())
                        .append(", poder ").append(inimigo.getPoder());
            }
            if (divisao.haveInimigo()) {
                texto.append("<br>Dano previsto ao entrar: ").append(motor.getEdificio().custoEntrada(divisao));
            }
        }
        if (divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected()) {
            ItemCura item = (ItemCura) divisao.getItem();
            texto.append("<br>").append(item.getType() == TypeItemCura.COLETE ? "Colete" : "Kit de vida")
                    .append(" +").append(item.getVida_recuperada());
        }
        if (divisao.getAlvo() != null && !divisao.getAlvo().isAtinigido()) {
            texto.append("<br>Objetivo: ").append(divisao.getAlvo().getNome());
        }
        if (divisao.isEntrada_saida()) {
            texto.append("<br>Entrada / saída");
        }
        return texto.append("</html>").toString();
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Desenho.EXTERIOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (planta == null || getWidth() < 50 || getHeight() < 50) {
            g.dispose();
            return;
        }

        AffineTransform t = transformacao();
        prepararFundo();
        double tremor = cena == null ? 0 : cena.tremor;
        double dx = tremor > 0 ? Math.sin((cena.relogio) * 0.09) * 6 * tremor : 0;
        double dy = tremor > 0 ? Math.cos((cena.relogio) * 0.113) * 5 * tremor : 0;
        Graphics2D b = (Graphics2D) g.create();
        b.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        b.translate(dx, dy);
        b.transform(t);
        b.setPaint(Desenho.texturaExterior());
        b.fill(new Rectangle2D.Double(-3000, -3000, planta.largura + 6000, planta.altura + 6000));
        b.translate(-MARGEM_FUNDO, -MARGEM_FUNDO);
        b.scale(1 / RESOLUCAO_FUNDO, 1 / RESOLUCAO_FUNDO);
        b.drawImage(fundo, 0, 0, null);
        b.dispose();

        if (cena != null && motor != null) {
            Graphics2D m = (Graphics2D) g.create();
            m.translate(dx, dy);
            m.transform(t);
            desenharMundo(m);
            m.dispose();
        }

        desenharEcra(g);
        g.dispose();
    }

    private static final double MARGEM_FUNDO = 60;

    private void prepararFundo() {
        if (fundo != null) {
            return;
        }
        int w = (int) Math.ceil((planta.largura + 2 * MARGEM_FUNDO) * RESOLUCAO_FUNDO);
        int h = (int) Math.ceil((planta.altura + 2 * MARGEM_FUNDO) * RESOLUCAO_FUNDO);
        fundo = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D f = fundo.createGraphics();
        f.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        f.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        f.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        f.scale(RESOLUCAO_FUNDO, RESOLUCAO_FUNDO);
        f.translate(MARGEM_FUNDO, MARGEM_FUNDO);
        Desenho.desenharEdificio(f, planta);
        f.dispose();
    }

    private void desenharMundo(Graphics2D g) {
        boolean livre = !cena.ocupada();
        boolean aJogar = motor.getEstado() == MotorJogo.Estado.A_JOGAR || motor.getEstado() == MotorJogo.Estado.DECIDIR_KIT;
        double pulso = 0.5 + 0.5 * Math.sin(cena.relogio / 260);

        // Itens e objetivo.
        for (int i = 0; i < planta.nomes.length; i++) {
            if (cena.itemVisivel[i] && cena.item[i] != null) {
                Point2D p = cena.posicaoItem(i);
                Graphics2D gi = escalado(g, p);
                if (cena.item[i].getType() == TypeItemCura.COLETE) {
                    Desenho.colete(gi, 0, 0, 0.6 + 0.4 * pulso);
                } else {
                    Desenho.kit(gi, 0, 0, 0.6 + 0.4 * pulso);
                }
                gi.dispose();
            }
        }
        if (cena.objetivoVisivel && cena.salaObjetivo >= 0) {
            Point2D p = cena.posicaoObjetivo(cena.salaObjetivo);
            Graphics2D gi = escalado(g, p);
            Desenho.objetivo(gi, cena.nomeObjetivo, 0, 0, (cena.relogio % 1400) / 1400.0);
            gi.dispose();
        }

        // Personagens: primeiro os caídos, depois os de pé, o Tó Cruz por cima.
        for (Figura f : cena.figuras()) {
            if (f.morto && f.visivel) {
                desenharFigura(g, f);
            }
        }
        for (Figura f : cena.figuras()) {
            if (!f.morto && !f.toCruz && f.visivel) {
                desenharFigura(g, f);
            }
        }
        Figura to = cena.toCruz();
        if (to.visivel && !to.morto) {
            desenharFigura(g, to);
        }

        desenharLuz(g, to);
        if (to.visivel && !to.morto && to.alfa > 0.3) {
            desenharLanterna(g, to);
        }

        // Caminhos sugeridos, salas onde se pode clicar e plano automático.
        if (plano != null) {
            desenharRota(g, plano.getCaminhoIda(), Tema.DOURADO, 5f, false);
            desenharRota(g, plano.getCaminhoVolta(), Tema.LARANJA, 4f, true);
        }
        if (livre && aJogar && motor.getEstado() == MotorJogo.Estado.A_JOGAR) {
            if (mostrarKit) {
                desenharRota(g, motor.caminhoParaKit(), Tema.VERDE, 3f, true);
            }
            if (mostrarSugestao) {
                desenharRota(g, motor.caminhoSugerido(), Tema.CIANO, 4f, false);
            }
        }
        for (int i = 0; i < planta.nomes.length; i++) {
            boolean clicavel = livre && clicavel(i);
            if (clicavel || i == salaSobre) {
                Rectangle2D r = planta.salas[i];
                Color cor = motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA ? Tema.VERDE : Tema.CIANO;
                g.setColor(Tema.comAlfa(cor, clicavel ? (int) (90 + 120 * pulso) : 70));
                g.setStroke(new BasicStroke(i == salaSobre ? 3.5f : 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{12f, 8f}, fase(cena.relogio / 40)));
                g.draw(new RoundRectangle2D.Double(r.getX() - 4, r.getY() - 4, r.getWidth() + 8, r.getHeight() + 8, 14, 14));
            }
        }

        // Barras de vida e avisos por cima das personagens.
        for (Figura f : cena.figuras()) {
            if (!f.morto && f.visivel && f.alfa > 0.2) {
                if (f.toCruz) {
                    double frac = f.vida / 100.0;
                    Color cor = f.vida > 60 ? Tema.VERDE : f.vida > 30 ? Tema.DOURADO : Tema.VERMELHO;
                    Desenho.barraVida(g, f.x, f.y - 40, frac, cor, 52);
                    if (f.vida > 100) {
                        Desenho.barraVida(g, f.x, f.y - 47, (f.vida - 100) / 100.0, Tema.AZUL, 52);
                    }
                } else {
                    Desenho.barraVida(g, f.x, f.y - 38, f.vida / (double) f.vidaMaxima, Tema.VERMELHO, 44);
                    g.setFont(Tema.texto(Font.BOLD, 12.5f));
                    String poder = String.valueOf(f.poder);
                    FontMetrics fm = g.getFontMetrics();
                    g.setColor(new Color(0, 0, 0, 180));
                    g.fill(new RoundRectangle2D.Double(f.x + 25, f.y - 45, fm.stringWidth(poder) + 10, 16, 8, 8));
                    g.setColor(new Color(0xFF8A80));
                    g.drawString(poder, (float) (f.x + 30), (float) (f.y - 32.5));
                }
                if (f.alerta > 0) {
                    double a = Math.min(1, f.alerta / 300);
                    g.setFont(Tema.titulo(22f));
                    g.setColor(Tema.comAlfa(Tema.DOURADO, (int) (255 * a)));
                    g.drawString("!", (float) (f.x - 4), (float) (f.y - 50 - 6 * Math.sin(cena.relogio / 90)));
                }
            }
        }

        for (Efeito e : cena.efeitos()) {
            e.desenhar(g);
        }
    }

    private static Graphics2D escalado(Graphics2D g, Point2D centro) {
        Graphics2D gi = (Graphics2D) g.create();
        gi.translate(centro.getX(), centro.getY());
        gi.scale(ESCALA_FIGURA * 0.85, ESCALA_FIGURA * 0.85);
        return gi;
    }

    private boolean clicavel(int i) {
        if (motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA) {
            return planta.saida[i];
        }
        if (motor.getEstado() != MotorJogo.Estado.A_JOGAR) {
            return false;
        }
        Divisao d = motor.getEdificio().getDivisao(planta.nomes[i]);
        return motor.getDestinosPossiveis().contains(d);
    }

    private void desenharFigura(Graphics2D g, Figura f) {
        if (f.alfa < 0.02) {
            return;
        }
        if (f.toCruz && f.comObjetivo && !f.morto) {
            double ax = f.x - Math.cos(f.angulo) * 18;
            double ay = f.y - Math.sin(f.angulo) * 18;
            Desenho.brilhoRedondo(g, ax, ay, 34, Tema.DOURADO, 0.8);
        }
        if (f.toCruz && !f.morto) {
            Desenho.brilhoRedondo(g, f.x, f.y, 52, Tema.CIANO, 0.45);
        }
        if (f.morto) {
            g.setColor(new Color(20, 8, 8, (int) (140 * Math.min(1, f.queda / (Math.PI / 2)))));
            g.fill(new Ellipse2D.Double(f.x - 22, f.y - 14, 44, 30));
        }
        Desenho.soldado(g, f.x, f.y, f.angulo + f.queda, !f.toCruz, f.alfa, f.flash, ESCALA_FIGURA);
    }

    /** Escurece tudo o que está longe do Tó Cruz; no nível Elite, as salas fora de alcance ficam às escuras. */
    private void desenharLuz(Graphics2D g, Figura to) {
        Rectangle2D tudo = new Rectangle2D.Double(-500, -500, planta.largura + 1000, planta.altura + 1000);
        if (to.visivel && !to.morto) {
            float[] fracoes = {0f, 0.45f, 1f};
            Color[] cores = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(4, 8, 14, 115)};
            g.setPaint(new RadialGradientPaint(new Point2D.Double(to.x, to.y), 640f, fracoes, cores));
        } else {
            g.setColor(new Color(4, 8, 14, 40));
        }
        g.fill(tudo);
        for (int i = 0; i < planta.nomes.length; i++) {
            if (cena.escondida(planta.nomes[i])) {
                g.setColor(new Color(4, 8, 14, 130));
                g.fill(planta.salas[i]);
            }
        }
        // Luzes verdes das saídas.
        for (int i = 0; i < planta.nomes.length; i++) {
            double[] porta = planta.portaExterior[i];
            if (porta != null) {
                Desenho.brilhoRedondo(g, porta[0], porta[1], 36, Tema.VERDE, 0.7);
            }
        }
        if (cena.alarme) {
            double a = 0.5 + 0.5 * Math.sin(cena.relogio / 140);
            g.setColor(new Color(255, 40, 40, (int) (28 * a)));
            g.fill(tudo);
        }
    }

    /** Feixe da lanterna do Tó Cruz, na direção para onde está virado. */
    private void desenharLanterna(Graphics2D g, Figura to) {
        double alcance = 300;
        double abertura = Math.toRadians(52);
        java.awt.geom.Arc2D feixe = new java.awt.geom.Arc2D.Double(to.x - alcance, to.y - alcance, 2 * alcance, 2 * alcance,
                -Math.toDegrees(to.angulo) - Math.toDegrees(abertura) / 2, Math.toDegrees(abertura), java.awt.geom.Arc2D.PIE);
        float[] fracoes = {0f, 0.15f, 1f};
        Color[] cores = {new Color(255, 244, 214, 0), new Color(255, 244, 214, 60), new Color(255, 244, 214, 0)};
        Graphics2D l = (Graphics2D) g.create();
        int sala = planta.salaEm(to.x, to.y);
        if (sala >= 0) {
            // A luz não atravessa paredes: fica dentro da sala onde o Tó Cruz está.
            l.clip(Desenho.interior(planta.salas[sala]));
        }
        l.setPaint(new RadialGradientPaint(new Point2D.Double(to.x, to.y), (float) alcance, fracoes, cores));
        l.fill(feixe);
        l.dispose();
    }

    /** Desenha um caminho pelos corredores, com um tracejado que se move no sentido do caminho. */
    private void desenharRota(Graphics2D g, ArrayUnorderedADT<Divisao> caminho, Color cor, float grossura, boolean fino) {
        if (caminho == null || caminho.size() < 2) {
            return;
        }
        Path2D linha = new Path2D.Double();
        Point2D inicio = planta.centro(planta.indice(caminho.find(0).getName()));
        linha.moveTo(inicio.getX(), inicio.getY());
        for (int k = 0; k + 1 < caminho.size(); k++) {
            int a = planta.indice(caminho.find(k).getName());
            int b = planta.indice(caminho.find(k + 1).getName());
            double[] c = planta.corredor(a, b);
            if (c != null) {
                for (int j = 0; j < c.length; j += 2) {
                    linha.lineTo(c[j], c[j + 1]);
                }
            }
            Point2D centro = planta.centro(b);
            linha.lineTo(centro.getX(), centro.getY());
        }
        if (!fino) {
            g.setColor(Tema.comAlfa(cor, 40));
            g.setStroke(new BasicStroke(grossura * 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linha);
        }
        Stroke traco = new BasicStroke(grossura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                fino ? new float[]{4f, 9f} : new float[]{14f, 10f}, fase(cena.relogio / 25));
        g.setColor(Tema.comAlfa(cor, 220));
        g.setStroke(traco);
        g.draw(linha);
        Point2D fim = planta.centro(planta.indice(caminho.last().getName()));
        g.setColor(cor);
        g.fill(new Ellipse2D.Double(fim.getX() - 6, fim.getY() - 6, 12, 12));
    }

    // ------------------------------------------------------------------ camada do ecrã

    private void desenharEcra(Graphics2D g) {
        if (cena != null && cena.vinheta > 0) {
            float[] fracoes = {0f, 0.7f, 1f};
            Color[] cores = {new Color(255, 0, 0, 0), new Color(255, 0, 0, 0), new Color(200, 20, 20, (int) (150 * cena.vinheta))};
            g.setPaint(new RadialGradientPaint(new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0),
                    (float) (Math.max(getWidth(), getHeight()) * 0.75), fracoes, cores));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (cena != null && cena.alarme) {
            double a = 0.5 + 0.5 * Math.sin(cena.relogio / 140);
            g.setColor(new Color(255, 50, 50, (int) (160 * a)));
            g.setStroke(new BasicStroke(6f));
            g.drawRect(3, 3, getWidth() - 6, getHeight() - 6);
        }

        if (motor != null && !escolherDificuldade) {
            desenharPainelObjetivo(g);
            desenharAtalhos(g);
            if (vistaDePerto()) {
                desenharMiniMapa(g);
            }
        }
        if (cena != null && cena.faixa != null) {
            desenharFaixa(g, cena.faixa, cena.corFaixa, null, cena.progressoFaixa());
        } else if (motor != null && cena != null && motor.getEstado() == MotorJogo.Estado.TERMINADO && !cena.ocupada()) {
            String titulo = motor.isSucesso() ? "MISSÃO CUMPRIDA" : "MISSÃO FALHADA";
            String detalhe = "Vida final " + motor.getToCruz().getVida() + "  ·  inimigos eliminados "
                    + motor.getInimigosMortos().size() + "  ·  " + motor.getTurno() + " turnos  ·  nível "
                    + motor.getDificuldade().getNome();
            desenharFaixa(g, titulo, motor.isSucesso() ? Tema.VERDE : Tema.VERMELHO, detalhe, 0.5);
        }
        if (escolherDificuldade) {
            desenharEscolhaDificuldade(g);
        }
    }

    private void desenharPainelObjetivo(Graphics2D g) {
        Dificuldade d = motor.getDificuldade();
        boolean alarme = cena != null && cena.alarme && motor.isAlarmeAtivo()
                && !(motor.getEstado() == MotorJogo.Estado.TERMINADO && !cena.ocupada());
        double w = 330;
        double h = alarme ? 118 : 86;
        double x = 16;
        double y = 16;
        g.setColor(new Color(8, 16, 28, 215));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        g.setColor(Tema.comAlfa(Tema.CARTAO_BORDA, 255));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        Color corNivel = corDe(d);
        g.setFont(Tema.texto(Font.BOLD, 11f));
        FontMetrics fm = g.getFontMetrics();
        String nivel = "NÍVEL " + d.getNome().toUpperCase();
        g.setColor(Tema.comAlfa(corNivel, 50));
        g.fill(new RoundRectangle2D.Double(x + 14, y + 12, fm.stringWidth(nivel) + 14, 19, 10, 10));
        g.setColor(corNivel);
        g.drawString(nivel, (float) (x + 21), (float) (y + 26));
        g.setColor(Tema.TEXTO_SUAVE);
        String turno = motor.getTurno() == 0 ? "A PREPARAR" : "TURNO " + motor.getTurno();
        g.drawString(turno, (float) (x + w - 14 - fm.stringWidth(turno)), (float) (y + 26));

        g.setFont(Tema.texto(Font.BOLD, 10.5f));
        g.setColor(Tema.DOURADO);
        g.drawString("OBJETIVO", (float) (x + 14), (float) (y + 52));
        g.setFont(Tema.texto(Font.PLAIN, 13.5f));
        g.setColor(Tema.TEXTO);
        String objetivo;
        String sala = cena != null && cena.salaObjetivo >= 0 ? planta.nomes[cena.salaObjetivo] : "?";
        boolean terminou = motor.getEstado() == MotorJogo.Estado.TERMINADO && !cena.ocupada();
        if (terminou) {
            objetivo = motor.isSucesso() ? "Cumprido: o objetivo saiu do edifício." : "Falhado.";
        } else if (cena.toCruz().comObjetivo) {
            objetivo = "Levar «" + cena.nomeObjetivo + "» até uma saída";
        } else {
            objetivo = "Recuperar «" + (cena == null ? "" : cena.nomeObjetivo) + "» em " + sala;
        }
        g.drawString(objetivo, (float) (x + 14), (float) (y + 72));

        if (alarme) {
            double a = 0.5 + 0.5 * Math.sin(cena.relogio / 140);
            g.setColor(Tema.comAlfa(Tema.VERMELHO, 40));
            g.fill(new RoundRectangle2D.Double(x + 10, y + 84, w - 20, 26, 8, 8));
            g.setColor(Tema.comAlfa(Tema.VERMELHO, (int) (150 + 105 * a)));
            g.fill(new Ellipse2D.Double(x + 20, y + 92, 10, 10));
            g.setFont(Tema.texto(Font.BOLD, 13f));
            g.setColor(new Color(0xFF8A80));
            int turnos = motor.getTurnosParaFugir();
            g.drawString("ALARME  ·  " + turnos + (turnos == 1 ? " turno" : " turnos") + " para sair",
                    (float) (x + 38), (float) (y + 102));
        }
    }

    /** Mapa pequeno do edifício inteiro, no canto, quando a câmara está perto. */
    private void desenharMiniMapa(Graphics2D g) {
        double w = 230;
        double escala = w / (planta.largura + 2 * MARGEM_FUNDO);
        double h = (planta.altura + 2 * MARGEM_FUNDO) * escala;
        double x = getWidth() - w - 16;
        double y = 16;
        g.setColor(new Color(8, 16, 28, 225));
        g.fill(new RoundRectangle2D.Double(x - 6, y - 6, w + 12, h + 12, 12, 12));
        g.setColor(Tema.CARTAO_BORDA);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x - 6, y - 6, w + 12, h + 12, 12, 12));

        Graphics2D m = (Graphics2D) g.create();
        m.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m.clip(new Rectangle2D.Double(x, y, w, h));
        m.translate(x, y);
        m.scale(escala, escala);
        m.translate(MARGEM_FUNDO, MARGEM_FUNDO);
        Graphics2D imagem = (Graphics2D) m.create();
        imagem.translate(-MARGEM_FUNDO, -MARGEM_FUNDO);
        imagem.scale(1 / RESOLUCAO_FUNDO, 1 / RESOLUCAO_FUNDO);
        imagem.drawImage(fundo, 0, 0, null);
        imagem.dispose();

        m.setColor(new Color(4, 8, 14, 90));
        m.fill(new Rectangle2D.Double(-MARGEM_FUNDO, -MARGEM_FUNDO, planta.largura + 2 * MARGEM_FUNDO, planta.altura + 2 * MARGEM_FUNDO));
        if (cena.objetivoVisivel && cena.salaObjetivo >= 0) {
            Point2D p = cena.posicaoObjetivo(cena.salaObjetivo);
            m.setColor(Tema.DOURADO);
            m.fill(new Ellipse2D.Double(p.getX() - 22, p.getY() - 22, 44, 44));
        }
        for (Figura f : cena.figuras()) {
            if (f.visivel && !f.morto && f.alfa > 0.3) {
                m.setColor(f.toCruz ? Tema.CIANO : Tema.VERMELHO);
                double r = f.toCruz ? 30 : 22;
                m.fill(new Ellipse2D.Double(f.x - r, f.y - r, 2 * r, 2 * r));
            }
        }
        // Zona que a câmara está a mostrar.
        try {
            AffineTransform inversa = transformacao().createInverse();
            Point2D a = inversa.transform(new Point2D.Double(0, topo()), null);
            Point2D b = inversa.transform(new Point2D.Double(getWidth(), topo() + alturaUtil()), null);
            m.setColor(new Color(255, 255, 255, 170));
            m.setStroke(new BasicStroke((float) (2 / escala)));
            m.draw(new Rectangle2D.Double(a.getX(), a.getY(), b.getX() - a.getX(), b.getY() - a.getY()));
        } catch (NoninvertibleTransformException e) {
            // sem retângulo
        }
        m.dispose();
    }

    private void desenharAtalhos(Graphics2D g) {
        if (motor.getEstado() == MotorJogo.Estado.TERMINADO) {
            return;
        }
        String texto = motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA
                ? "Clique numa entrada (contorno verde) para o Tó Cruz entrar"
                : "Clique numa sala vizinha para avançar   ·   A atacar   K kit   F ficar   S sair";
        g.setFont(Tema.texto(Font.PLAIN, 12.5f));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(texto) + 24;
        double x = (getWidth() - w) / 2;
        double y = getHeight() - 38;
        g.setColor(new Color(8, 16, 28, 200));
        g.fill(new RoundRectangle2D.Double(x, y, w, 26, 13, 13));
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString(texto, (float) (x + 12), (float) (y + 17.5));
    }

    private void desenharFaixa(Graphics2D g, String titulo, Color cor, String detalhe, double progresso) {
        double entrada = Math.min(1, progresso / 0.12);
        double saida = detalhe == null ? Math.min(1, (1 - progresso) / 0.15) : 1;
        double a = Math.max(0, Math.min(entrada, saida));
        int altura = detalhe == null ? 96 : 130;
        int y = getHeight() / 2 - altura / 2;
        Graphics2D f = (Graphics2D) g.create();
        f.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a));
        f.setPaint(new GradientPaint(0, y, new Color(5, 10, 18, 235), 0, y + altura, new Color(5, 10, 18, 200)));
        f.fillRect(0, y, getWidth(), altura);
        f.setColor(cor);
        f.fillRect(0, y, getWidth(), 3);
        f.fillRect(0, y + altura - 3, getWidth(), 3);
        f.setFont(Tema.titulo(detalhe == null ? 34f : 42f));
        FontMetrics fm = f.getFontMetrics();
        double escala = 0.85 + 0.15 * a;
        f.translate(getWidth() / 2.0, y + (detalhe == null ? 60 : 64));
        f.scale(escala, escala);
        f.setColor(cor);
        f.drawString(titulo, -fm.stringWidth(titulo) / 2f, 0);
        if (detalhe != null) {
            f.setFont(Tema.texto(Font.PLAIN, 16f));
            fm = f.getFontMetrics();
            f.setColor(Tema.TEXTO);
            f.drawString(detalhe, -fm.stringWidth(detalhe) / 2f, 36);
        }
        f.dispose();
    }

    private void desenharEscolhaDificuldade(Graphics2D g) {
        g.setColor(new Color(4, 9, 16, 205));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(Tema.titulo(30f));
        FontMetrics fm = g.getFontMetrics();
        String titulo = "ESCOLHA A DIFICULDADE";
        Rectangle2D primeiro = cartao(0);
        g.setColor(Tema.TEXTO);
        g.drawString(titulo, (getWidth() - fm.stringWidth(titulo)) / 2, (int) primeiro.getY() - 56);
        g.setFont(Tema.texto(Font.PLAIN, 14f));
        fm = g.getFontMetrics();
        String sub = "Missão «" + nomeMissao + "»  ·  recuperar o objetivo e sair do edifício com vida";
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, (int) primeiro.getY() - 28);

        Dificuldade[] niveis = Dificuldade.values();
        for (int k = 0; k < niveis.length; k++) {
            Rectangle2D r = cartao(k);
            Color cor = corDe(niveis[k]);
            boolean sobre = k == cartaoSobre;
            RoundRectangle2D forma = new RoundRectangle2D.Double(r.getX(), r.getY() - (sobre ? 6 : 0), r.getWidth(), r.getHeight(), 18, 18);
            g.setColor(sobre ? new Color(0x183150) : Tema.CARTAO);
            g.fill(forma);
            g.setColor(sobre ? cor : Tema.CARTAO_BORDA);
            g.setStroke(new BasicStroke(sobre ? 2.5f : 1.3f));
            g.draw(forma);
            double top = forma.getY();
            g.setColor(cor);
            g.fill(new RoundRectangle2D.Double(r.getX() + 20, top + 22, 40, 5, 4, 4));

            for (int c = 0; c <= k; c++) {
                Path2D chevron = new Path2D.Double();
                double cx = r.getMaxX() - 30 - c * 16;
                chevron.moveTo(cx - 6, top + 30);
                chevron.lineTo(cx, top + 22);
                chevron.lineTo(cx + 6, top + 30);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(chevron);
            }

            g.setFont(Tema.titulo(26f));
            g.setColor(Tema.TEXTO);
            g.drawString(niveis[k].getNome().toUpperCase(), (float) (r.getX() + 20), (float) (top + 66));
            g.setFont(Tema.texto(Font.PLAIN, 13.5f));
            g.setColor(Tema.TEXTO_SUAVE);
            escreverComQuebras(g, niveis[k].getDescricao(), r.getX() + 20, top + 94, r.getWidth() - 40, 19);

            g.setFont(Tema.texto(Font.BOLD, 12.5f));
            g.setColor(cor);
            g.drawString(sobre ? "Clique para começar  →" : "Começar", (float) (r.getX() + 20), (float) (top + r.getHeight() - 22));
        }
    }

    private static void escreverComQuebras(Graphics2D g, String texto, double x, double y, double largura, double passo) {
        FontMetrics fm = g.getFontMetrics();
        String[] palavras = texto.split(" ");
        StringBuilder linha = new StringBuilder();
        double yy = y;
        for (String palavra : palavras) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (fm.stringWidth(tentativa) > largura && linha.length() > 0) {
                g.drawString(linha.toString(), (float) x, (float) yy);
                yy += passo;
                linha = new StringBuilder(palavra);
            } else {
                linha = new StringBuilder(tentativa);
            }
        }
        if (linha.length() > 0) {
            g.drawString(linha.toString(), (float) x, (float) yy);
        }
    }

    /** Fase do tracejado (tem de ser positiva); diminuir faz o tracejado andar para a frente. */
    private static float fase(double deslocamento) {
        return (float) (10000 - deslocamento % 10000);
    }

    static Color corDe(Dificuldade d) {
        switch (d) {
            case RECRUTA:
                return Tema.VERDE;
            case ELITE:
                return Tema.VERMELHO;
            default:
                return Tema.CIANO;
        }
    }
}
