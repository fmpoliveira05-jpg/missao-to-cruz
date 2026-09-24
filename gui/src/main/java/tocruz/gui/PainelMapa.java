package tocruz.gui;

import ed.interfaces.ArrayUnorderedADT;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.motor.MotorJogo;
import tocruz.motor.Plano;
import tocruz.personagens.Inimigo;

/**
 * Desenha o edifício como uma planta: divisões, ligações, inimigos, itens, alvo, o Tó Cruz e os
 * caminhos sugeridos. Um clique numa divisão é passado à janela, que decide o que fazer.
 *
 * @author Francisco Oliveira
 */
class PainelMapa extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final double ALTURA_CAIXA = 58;
    /** Proporção (largura / altura) da área do mapa na janela. */
    private static final double PROPORCAO = 1.4;

    /** Quem recebe os cliques nas divisões. */
    interface OuvinteDivisao {
        void clicou(Divisao divisao);
    }

    private transient MotorJogo motor;
    private transient Plano plano;
    private transient OuvinteDivisao ouvinte;

    private String[] nomes = new String[0];
    private boolean[][] ligado = new boolean[0][0];
    private double[] larguras = new double[0];
    private double[] alturas = new double[0];
    private transient DisposicaoGrafo disposicao;

    private boolean mostrarSugestao = true;
    private boolean mostrarKit = true;
    private int sobre = -1;

    PainelMapa() {
        setPreferredSize(new Dimension(900, 640));
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter rato = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int indice = divisaoEm(e.getX(), e.getY());
                if (indice != sobre) {
                    sobre = indice;
                    setCursor(Cursor.getPredefinedCursor(indice >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobre = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int indice = divisaoEm(e.getX(), e.getY());
                if (indice >= 0 && ouvinte != null && motor != null) {
                    ouvinte.clicou(motor.getEdificio().getDivisao(nomes[indice]));
                }
            }
        };
        addMouseListener(rato);
        addMouseMotionListener(rato);
    }

    void setOuvinte(OuvinteDivisao ouvinte) {
        this.ouvinte = ouvinte;
    }

    /**
     * Calcula a disposição das divisões de uma missão (só é preciso quando se abre uma missão).
     */
    void setEdificio(Edificio edificio) {
        int n = 0;
        Iterator<Divisao> it = edificio.IteratorMapa();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        nomes = new String[n];
        it = edificio.IteratorMapa();
        for (int i = 0; i < n; i++) {
            nomes[i] = it.next().getName();
        }

        ligado = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            Iterator<Divisao> vizinhos = edificio.getNextDivisoes(edificio.getDivisao(nomes[i]));
            while (vizinhos.hasNext()) {
                int j = indice(vizinhos.next().getName());
                ligado[i][j] = true;
                ligado[j][i] = true;
            }
        }

        FontMetrics medidas = getFontMetrics(Tema.texto(Font.BOLD, 14f));
        larguras = new double[n];
        alturas = new double[n];
        for (int i = 0; i < n; i++) {
            larguras[i] = Math.max(122, medidas.stringWidth(nomes[i]) + 34);
            alturas[i] = ALTURA_CAIXA;
        }
        disposicao = DisposicaoGrafo.calcular(ligado, larguras, alturas, PROPORCAO);
        repaint();
    }

    void setMotor(MotorJogo motor) {
        this.motor = motor;
        this.plano = null;
        repaint();
    }

    void setPlano(Plano plano) {
        this.plano = plano;
        repaint();
    }

    Plano getPlano() {
        return plano;
    }

    void setMostrarSugestao(boolean mostrar) {
        this.mostrarSugestao = mostrar;
        repaint();
    }

    void setMostrarKit(boolean mostrar) {
        this.mostrarKit = mostrar;
        repaint();
    }

    // ------------------------------------------------------------------ geometria

    private int indice(String nome) {
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    private AffineTransform transformacao() {
        // Reserva uma faixa em baixo para a legenda.
        double alturaUtil = getHeight() - 70;
        double escala = Math.min(1.35, Math.min((getWidth() - 40) / disposicao.largura, alturaUtil / disposicao.altura));
        AffineTransform t = new AffineTransform();
        t.translate((getWidth() - disposicao.largura * escala) / 2, 16 + (alturaUtil - disposicao.altura * escala) / 2);
        t.scale(escala, escala);
        return t;
    }

    private int divisaoEm(int px, int py) {
        if (disposicao == null) {
            return -1;
        }
        Point2D ponto;
        try {
            ponto = transformacao().inverseTransform(new Point2D.Double(px, py), null);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            return -1;
        }
        for (int i = 0; i < nomes.length; i++) {
            if (caixa(i).contains(ponto)) {
                return i;
            }
        }
        return -1;
    }

    private RoundRectangle2D caixa(int i) {
        return new RoundRectangle2D.Double(disposicao.x[i] - larguras[i] / 2, disposicao.y[i] - alturas[i] / 2,
                larguras[i], alturas[i], 16, 16);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = divisaoEm(e.getX(), e.getY());
        if (i < 0 || motor == null) {
            return null;
        }
        Divisao divisao = motor.getEdificio().getDivisao(nomes[i]);
        StringBuilder texto = new StringBuilder("<html><b>").append(divisao.getName()).append("</b>");
        for (Inimigo inimigo : divisao.getInimigos()) {
            texto.append("<br>").append(inimigo.getNome()).append(": vida ").append(inimigo.getVida())
                    .append(", poder ").append(inimigo.getPoder());
        }
        if (divisao.haveInimigo()) {
            texto.append("<br>Dano previsto ao entrar: ").append(motor.getEdificio().custoEntrada(divisao));
        }
        if (divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected()) {
            ItemCura item = (ItemCura) divisao.getItem();
            texto.append("<br>").append(item.getType() == TypeItemCura.COLETE ? "Colete" : "Kit de vida")
                    .append(" +").append(item.getVida_recuperada());
        }
        if (divisao.getAlvo() != null) {
            texto.append("<br>Alvo: ").append(divisao.getAlvo().getNome());
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
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        desenharFundo(g);
        if (disposicao == null || motor == null) {
            g.dispose();
            return;
        }

        Graphics2D m = (Graphics2D) g.create();
        m.transform(transformacao());
        desenharLigacoes(m);
        if (plano != null) {
            desenharCaminho(m, plano.getCaminhoIda(), Tema.DOURADO, false, 7f);
            desenharCaminho(m, plano.getCaminhoVolta(), Tema.LARANJA, true, 5f);
        }
        if (motor.getEstado() != MotorJogo.Estado.TERMINADO && motor.getEstado() != MotorJogo.Estado.ESCOLHER_ENTRADA) {
            if (mostrarKit) {
                desenharCaminho(m, motor.caminhoParaKit(), Tema.VERDE, true, 4f);
            }
            if (mostrarSugestao) {
                desenharCaminho(m, motor.caminhoSugerido(), Tema.CIANO, false, 6f);
            }
        }
        for (int i = 0; i < nomes.length; i++) {
            desenharDivisao(m, i);
        }
        m.dispose();

        desenharLegenda(g);
        if (motor.getEstado() == MotorJogo.Estado.TERMINADO) {
            desenharFim(g);
        }
        g.dispose();
    }

    private void desenharFundo(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, Tema.FUNDO, 0, getHeight(), new Color(0x071426)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x < getWidth(); x += 24) {
            g.setColor(x % 120 == 0 ? Tema.GRELHA_FORTE : Tema.GRELHA);
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 24) {
            g.setColor(y % 120 == 0 ? Tema.GRELHA_FORTE : Tema.GRELHA);
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void desenharLigacoes(Graphics2D g) {
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Tema.LIGACAO);
        for (int i = 0; i < nomes.length; i++) {
            for (int j = i + 1; j < nomes.length; j++) {
                if (ligado[i][j]) {
                    g.draw(new java.awt.geom.Line2D.Double(disposicao.x[i], disposicao.y[i], disposicao.x[j], disposicao.y[j]));
                }
            }
        }
    }

    private void desenharCaminho(Graphics2D g, ArrayUnorderedADT<Divisao> caminho, Color cor, boolean tracejado, float grossura) {
        if (caminho == null || caminho.size() < 2) {
            return;
        }
        Path2D linha = new Path2D.Double();
        for (int p = 0; p < caminho.size(); p++) {
            int i = indice(caminho.find(p).getName());
            if (p == 0) {
                linha.moveTo(disposicao.x[i], disposicao.y[i]);
            } else {
                linha.lineTo(disposicao.x[i], disposicao.y[i]);
            }
        }
        if (!tracejado) {
            g.setColor(Tema.comAlfa(cor, 45));
            g.setStroke(new BasicStroke(grossura * 3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linha);
        }
        Stroke traco = tracejado
                ? new BasicStroke(grossura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10f, 10f}, 0f)
                : new BasicStroke(grossura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setColor(cor);
        g.setStroke(traco);
        g.draw(linha);

        // Setas a meio de cada troço, para se perceber o sentido.
        for (int p = 1; p < caminho.size(); p++) {
            int a = indice(caminho.find(p - 1).getName());
            int b = indice(caminho.find(p).getName());
            double mx = (disposicao.x[a] + disposicao.x[b]) / 2;
            double my = (disposicao.y[a] + disposicao.y[b]) / 2;
            double angulo = Math.atan2(disposicao.y[b] - disposicao.y[a], disposicao.x[b] - disposicao.x[a]);
            Path2D seta = new Path2D.Double();
            double t = grossura * 1.9;
            seta.moveTo(t, 0);
            seta.lineTo(-t, -t);
            seta.lineTo(-t * 0.4, 0);
            seta.lineTo(-t, t);
            seta.closePath();
            AffineTransform at = AffineTransform.getTranslateInstance(mx, my);
            at.rotate(angulo);
            g.fill(at.createTransformedShape(seta));
        }
    }

    private void desenharDivisao(Graphics2D g, int i) {
        Divisao divisao = motor.getEdificio().getDivisao(nomes[i]);
        RoundRectangle2D caixa = caixa(i);
        boolean aqui = motor.getDivisaoAtual() != null && motor.getDivisaoAtual().getName().equals(nomes[i]);
        boolean alcancavel = contem(motor.getDestinosPossiveis(), divisao)
                || (motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA && divisao.isEntrada_saida());
        boolean alvo = divisao.getAlvo() != null && !divisao.getAlvo().isAtinigido();

        // Brilho à volta das divisões importantes.
        Color brilho = aqui ? Tema.CIANO : alvo ? Tema.DOURADO : divisao.haveInimigo() ? Tema.VERMELHO : null;
        if (brilho != null) {
            for (int r = 3; r >= 1; r--) {
                g.setColor(Tema.comAlfa(brilho, 18 * r));
                g.setStroke(new BasicStroke(r * 5f));
                g.draw(caixa);
            }
        }

        g.setColor(i == sobre ? Tema.DIVISAO_REALCE : Tema.DIVISAO);
        g.fill(caixa);

        Color borda = aqui ? Tema.CIANO : alvo ? Tema.DOURADO : divisao.isEntrada_saida() ? Tema.VERDE : Tema.DIVISAO_BORDA;
        float grossura = aqui ? 3f : (alvo || divisao.isEntrada_saida()) ? 2.4f : 1.5f;
        g.setColor(borda);
        g.setStroke(alcancavel && !aqui
                ? new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 5f}, 0f)
                : new BasicStroke(grossura));
        if (alcancavel && !aqui) {
            g.setColor(Tema.CIANO);
        }
        g.draw(caixa);

        // Nome da divisão.
        g.setFont(Tema.texto(Font.BOLD, 14f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        double cx = disposicao.x[i];
        double topo = caixa.getY();
        long poder = 0;
        for (Inimigo inimigo : divisao.getInimigos()) {
            poder += inimigo.getPoder();
        }
        ItemCura item = divisao.getItem() instanceof ItemCura && !divisao.getItem().isCollected() ? (ItemCura) divisao.getItem() : null;
        boolean temIcones = poder > 0 || item != null || alvo;
        double linhaNome = temIcones ? topo + 23 : disposicao.y[i] + fm.getAscent() / 2.0 - 2;
        g.drawString(nomes[i], (float) (cx - fm.stringWidth(nomes[i]) / 2.0), (float) linhaNome);

        // Ícones por baixo do nome.
        double iconeY = topo + 40;
        double larguraIcones = 0;
        g.setFont(Tema.texto(Font.BOLD, 11.5f));
        FontMetrics fi = g.getFontMetrics();
        String textoInimigos = divisao.getInimigos().size() + " × " + poder;
        String textoItem = item == null ? "" : "+" + item.getVida_recuperada();
        if (poder > 0) {
            larguraIcones += 18 + fi.stringWidth(textoInimigos) + 8;
        }
        if (item != null) {
            larguraIcones += 16 + fi.stringWidth(textoItem) + 8;
        }
        if (alvo) {
            larguraIcones += 18;
        }
        double ix = cx - larguraIcones / 2;

        if (poder > 0) {
            double w = 18 + fi.stringWidth(textoInimigos);
            g.setColor(Tema.comAlfa(Tema.VERMELHO, 60));
            g.fill(new RoundRectangle2D.Double(ix - 2, iconeY - 8, w + 4, 16, 16, 16));
            desenharCaveira(g, ix + 7, iconeY);
            g.setColor(Tema.VERMELHO);
            g.drawString(textoInimigos, (float) (ix + 15), (float) (iconeY + 4));
            ix += w + 8;
        }
        if (item != null) {
            if (item.getType() == TypeItemCura.COLETE) {
                desenharColete(g, ix + 6, iconeY);
                g.setColor(Tema.AZUL);
            } else {
                desenharKit(g, ix + 6, iconeY);
                g.setColor(Tema.VERDE);
            }
            g.drawString(textoItem, (float) (ix + 15), (float) (iconeY + 4));
            ix += 16 + fi.stringWidth(textoItem) + 8;
        }
        if (alvo) {
            desenharAlvo(g, ix + 8, iconeY);
        }

        if (divisao.isEntrada_saida()) {
            desenharEtiqueta(g, "ENTRADA / SAÍDA", caixa.getX() + 10, topo - 8, Tema.VERDE);
        }
        if (aqui) {
            desenharToCruz(g, caixa.getMaxX() - 4, topo + 4);
        }
    }

    private static boolean contem(ArrayUnorderedADT<Divisao> lista, Divisao divisao) {
        for (Divisao d : lista) {
            if (d.equals(divisao)) {
                return true;
            }
        }
        return false;
    }

    private void desenharEtiqueta(Graphics2D g, String texto, double x, double y, Color cor) {
        g.setFont(Tema.texto(Font.BOLD, 9.5f));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(texto) + 10;
        g.setColor(Tema.FUNDO);
        g.fill(new RoundRectangle2D.Double(x, y - 7, w, 14, 8, 8));
        g.setColor(cor);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x, y - 7, w, 14, 8, 8));
        g.drawString(texto, (float) (x + 5), (float) (y + 3.5));
    }

    private void desenharToCruz(Graphics2D g, double cx, double cy) {
        double r = 15;
        g.setColor(Tema.comAlfa(Tema.CIANO, 70));
        g.fill(new Ellipse2D.Double(cx - r - 5, cy - r - 5, 2 * r + 10, 2 * r + 10));
        g.setColor(Tema.CIANO);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(Tema.FUNDO);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setFont(Tema.titulo(13f));
        FontMetrics fm = g.getFontMetrics();
        String iniciais = "TC";
        g.drawString(iniciais, (float) (cx - fm.stringWidth(iniciais) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 2));
    }

    private static void desenharCaveira(Graphics2D g, double cx, double cy) {
        g.setColor(Tema.VERMELHO);
        g.fill(new Ellipse2D.Double(cx - 5, cy - 6, 10, 9));
        g.fill(new RoundRectangle2D.Double(cx - 3.2, cy + 1, 6.4, 4.5, 2, 2));
        g.setColor(Tema.DIVISAO);
        g.fill(new Ellipse2D.Double(cx - 3.4, cy - 3.5, 2.8, 2.8));
        g.fill(new Ellipse2D.Double(cx + 0.6, cy - 3.5, 2.8, 2.8));
    }

    private static void desenharKit(Graphics2D g, double cx, double cy) {
        g.setColor(Tema.VERDE);
        g.fill(new RoundRectangle2D.Double(cx - 6, cy - 6, 12, 12, 4, 4));
        g.setColor(Tema.DIVISAO);
        g.fill(new java.awt.geom.Rectangle2D.Double(cx - 1.2, cy - 4, 2.4, 8));
        g.fill(new java.awt.geom.Rectangle2D.Double(cx - 4, cy - 1.2, 8, 2.4));
    }

    private static void desenharColete(Graphics2D g, double cx, double cy) {
        Path2D escudo = new Path2D.Double();
        escudo.moveTo(cx, cy - 7);
        escudo.lineTo(cx + 6, cy - 4.5);
        escudo.curveTo(cx + 6, cy + 2, cx + 3, cy + 5.5, cx, cy + 7);
        escudo.curveTo(cx - 3, cy + 5.5, cx - 6, cy + 2, cx - 6, cy - 4.5);
        escudo.closePath();
        g.setColor(Tema.AZUL);
        g.fill(escudo);
    }

    private static void desenharAlvo(Graphics2D g, double cx, double cy) {
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(Tema.DOURADO);
        g.draw(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
        g.draw(new Ellipse2D.Double(cx - 3.5, cy - 3.5, 7, 7));
        g.fill(new Ellipse2D.Double(cx - 1.3, cy - 1.3, 2.6, 2.6));
    }

    private void desenharLegenda(Graphics2D g) {
        String[] textos = {"Tó Cruz", "Inimigos", "Kit de vida", "Colete", "Alvo", "Entrada / saída", "Caminho sugerido", "Kit mais próximo"};
        g.setFont(Tema.texto(Font.PLAIN, 12f));
        FontMetrics fm = g.getFontMetrics();
        double x = 18;
        double y = getHeight() - 20;
        for (int i = 0; i < textos.length; i++) {
            double cx = x + 8;
            switch (i) {
                case 0:
                    g.setColor(Tema.CIANO);
                    g.fill(new Ellipse2D.Double(cx - 6, y - 6, 12, 12));
                    break;
                case 1:
                    desenharCaveira(g, cx, y);
                    break;
                case 2:
                    desenharKit(g, cx, y);
                    break;
                case 3:
                    desenharColete(g, cx, y);
                    break;
                case 4:
                    desenharAlvo(g, cx, y);
                    break;
                case 5:
                    g.setColor(Tema.VERDE);
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new RoundRectangle2D.Double(cx - 7, y - 5, 14, 10, 4, 4));
                    break;
                case 6:
                    g.setColor(Tema.CIANO);
                    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new java.awt.geom.Line2D.Double(cx - 8, y, cx + 8, y));
                    break;
                default:
                    g.setColor(Tema.VERDE);
                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 5f}, 0f));
                    g.draw(new java.awt.geom.Line2D.Double(cx - 8, y, cx + 8, y));
                    break;
            }
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(textos[i], (float) (x + 22), (float) (y + 4.5));
            x += 22 + fm.stringWidth(textos[i]) + 20;
        }
    }

    private void desenharFim(Graphics2D g) {
        boolean sucesso = motor.isSucesso();
        Color cor = sucesso ? Tema.VERDE : Tema.VERMELHO;
        int alturaFaixa = 120;
        int y = getHeight() / 2 - alturaFaixa / 2;
        g.setColor(new Color(5, 12, 22, 215));
        g.fillRect(0, y, getWidth(), alturaFaixa);
        g.setColor(cor);
        g.fillRect(0, y, getWidth(), 3);
        g.fillRect(0, y + alturaFaixa - 3, getWidth(), 3);

        String titulo = sucesso ? "MISSÃO CUMPRIDA" : "MISSÃO FALHADA";
        g.setFont(Tema.titulo(40f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(titulo, (getWidth() - fm.stringWidth(titulo)) / 2, y + 60);

        String detalhe = "Vida final: " + motor.getToCruz().getVida() + "   ·   inimigos eliminados: "
                + motor.getInimigosMortos().size() + "   ·   turnos: " + motor.getTurno();
        g.setFont(Tema.texto(Font.PLAIN, 16f));
        fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        g.drawString(detalhe, (getWidth() - fm.stringWidth(detalhe)) / 2, y + 92);
    }
}
