package tocruz.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.Random;

/**
 * Desenho da planta (chão, paredes, corredores, mobília), das personagens vistas de cima e dos
 * objetos. Tudo é desenhado com formas simples em código, sem imagens externas.
 *
 * @author Francisco Oliveira
 */
final class Desenho {

    static final Color EXTERIOR = new Color(0x0B1016);
    static final Color PAREDE = new Color(0x687584);
    static final Color PAREDE_TOPO = new Color(0x93A0AF);
    static final Color CORREDOR_CHAO = new Color(0x3A4148);

    private static final TexturePaint RUIDO_EXTERIOR = textura(EXTERIOR, 12, 1);
    private static final TexturePaint AZULEJO = azulejos(new Color(0x4C5965), new Color(0x3D4852), 26);
    private static final TexturePaint AZULEJO_CLARO = azulejos(new Color(0x56636E), new Color(0x46525C), 18);
    private static final TexturePaint MADEIRA = tabuas(new Color(0x5E4C3E), new Color(0x4C3D31));
    private static final TexturePaint ALCATIFA = textura(new Color(0x39455A), 10, 2);
    private static final TexturePaint BETAO = textura(new Color(0x484D53), 14, 3);
    private static final TexturePaint BETAO_CORREDOR = textura(CORREDOR_CHAO, 10, 4);

    private Desenho() {
    }

    // ------------------------------------------------------------------ texturas

    private static TexturePaint textura(Color base, int variacao, long semente) {
        int lado = 96;
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(semente);
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                int d = r.nextInt(variacao * 2 + 1) - variacao;
                img.setRGB(x, y, new Color(limitar(base.getRed() + d), limitar(base.getGreen() + d), limitar(base.getBlue() + d)).getRGB());
            }
        }
        return new TexturePaint(img, new Rectangle(0, 0, lado, lado));
    }

    private static TexturePaint azulejos(Color base, Color junta, int lado) {
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(base);
        g.fillRect(0, 0, lado, lado);
        g.setColor(base.brighter().darker().brighter());
        g.fillRect(1, 1, lado / 2, 1);
        g.setColor(junta);
        g.fillRect(0, lado - 1, lado, 1);
        g.fillRect(lado - 1, 0, 1, lado);
        g.dispose();
        return new TexturePaint(img, new Rectangle(0, 0, lado, lado));
    }

    private static TexturePaint tabuas(Color base, Color junta) {
        int w = 96;
        int h = 48;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random r = new Random(7);
        for (int fila = 0; fila < 4; fila++) {
            int d = r.nextInt(12) - 6;
            g.setColor(new Color(limitar(base.getRed() + d), limitar(base.getGreen() + d), limitar(base.getBlue() + d)));
            g.fillRect(0, fila * 12, w, 12);
            g.setColor(junta);
            g.fillRect(0, fila * 12 + 11, w, 1);
            int corte = (fila % 2 == 0 ? 20 : 64);
            g.fillRect(corte, fila * 12, 1, 12);
        }
        g.dispose();
        return new TexturePaint(img, new Rectangle(0, 0, w, h));
    }

    private static int limitar(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Tira acentos e passa a minúsculas, para reconhecer o tipo de sala pelo nome. */
    static String simplificar(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    // ------------------------------------------------------------------ planta

    static TexturePaint texturaExterior() {
        return RUIDO_EXTERIOR;
    }

    /** Desenha a parte fixa do edifício (o que não muda durante o jogo), sobre fundo transparente. */
    static void desenharEdificio(Graphics2D g, Planta p) {

        double[][] corredores = p.corredores();

        // Sombras projetadas.
        for (int camada = 3; camada >= 1; camada--) {
            g.setColor(new Color(0, 0, 0, 34));
            float grossura = (float) (Planta.CORREDOR + 2 * Planta.PAREDE + camada * 8);
            g.setStroke(new BasicStroke(grossura, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            for (double[] c : corredores) {
                g.draw(deslocar(caminho(c), 6, 8));
            }
            for (Rectangle2D r : p.salas) {
                g.fill(new RoundRectangle2D.Double(r.getX() + 6 - camada * 4, r.getY() + 8 - camada * 4,
                        r.getWidth() + camada * 8, r.getHeight() + camada * 8, 18, 18));
            }
        }

        // Paredes dos corredores.
        g.setColor(PAREDE);
        g.setStroke(new BasicStroke((float) (Planta.CORREDOR + 2 * Planta.PAREDE), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (double[] c : corredores) {
            g.draw(caminho(c));
        }

        // Salas: paredes e chão.
        for (int i = 0; i < p.salas.length; i++) {
            Rectangle2D r = p.salas[i];
            g.setColor(PAREDE);
            g.fill(r);
            g.setColor(PAREDE_TOPO);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Rectangle2D.Double(r.getX() + 0.75, r.getY() + 0.75, r.getWidth() - 1.5, r.getHeight() - 1.5));
            Rectangle2D chao = interior(r);
            g.setPaint(chaoDe(p.nomes[i]));
            g.fill(chao);
        }

        // Chão dos corredores (por cima das paredes, abre as portas).
        g.setPaint(BETAO_CORREDOR);
        g.setStroke(new BasicStroke((float) Planta.CORREDOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (double[] c : corredores) {
            g.draw(caminho(c));
        }
        // Linha de sinalização a meio do corredor.
        g.setColor(new Color(255, 255, 255, 18));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{10f, 8f}, 0f));
        for (double[] c : corredores) {
            g.draw(caminho(c));
        }

        for (int i = 0; i < p.salas.length; i++) {
            Rectangle2D chao = interior(p.salas[i]);
            desenharMobilia(g, p.nomes[i], chao);
            sombraInterior(g, chao);
            luzDoTeto(g, chao);
            desenharEtiqueta(g, p.nomes[i], chao);
        }

        for (int i = 0; i < p.salas.length; i++) {
            if (p.portaExterior[i] != null) {
                desenharPortaExterior(g, p.portaExterior[i]);
            }
        }
    }

    static Rectangle2D interior(Rectangle2D r) {
        return new Rectangle2D.Double(r.getX() + Planta.PAREDE, r.getY() + Planta.PAREDE,
                r.getWidth() - 2 * Planta.PAREDE, r.getHeight() - 2 * Planta.PAREDE);
    }

    static Path2D caminho(double[] pontos) {
        Path2D linha = new Path2D.Double();
        linha.moveTo(pontos[0], pontos[1]);
        for (int k = 2; k < pontos.length; k += 2) {
            linha.lineTo(pontos[k], pontos[k + 1]);
        }
        return linha;
    }

    private static java.awt.Shape deslocar(Path2D forma, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(forma);
    }

    private static TexturePaint chaoDe(String nome) {
        String n = simplificar(nome);
        if (n.contains("laborat") || n.contains("wc") || n.contains("cozinha") || n.contains("balne")) {
            return n.contains("wc") ? AZULEJO_CLARO : AZULEJO;
        }
        if (n.contains("escritorio") || n.contains("camarat") || n.contains("hall") || n.contains("sala")) {
            return MADEIRA;
        }
        if (n.contains("seguranca") || n.contains("porteiro") || n.contains("recec")) {
            return ALCATIFA;
        }
        return BETAO;
    }

    /** Escurece o chão junto às paredes (dá profundidade). */
    private static void sombraInterior(Graphics2D g, Rectangle2D r) {
        int t = 12;
        g.setPaint(new GradientPaint((float) r.getX(), 0, new Color(0, 0, 0, 90), (float) (r.getX() + t), 0, new Color(0, 0, 0, 0)));
        g.fill(new Rectangle2D.Double(r.getX(), r.getY(), t, r.getHeight()));
        g.setPaint(new GradientPaint((float) r.getMaxX(), 0, new Color(0, 0, 0, 70), (float) (r.getMaxX() - t), 0, new Color(0, 0, 0, 0)));
        g.fill(new Rectangle2D.Double(r.getMaxX() - t, r.getY(), t, r.getHeight()));
        g.setPaint(new GradientPaint(0, (float) r.getY(), new Color(0, 0, 0, 100), 0, (float) (r.getY() + t), new Color(0, 0, 0, 0)));
        g.fill(new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), t));
        g.setPaint(new GradientPaint(0, (float) r.getMaxY(), new Color(0, 0, 0, 60), 0, (float) (r.getMaxY() - t), new Color(0, 0, 0, 0)));
        g.fill(new Rectangle2D.Double(r.getX(), r.getMaxY() - t, r.getWidth(), t));
    }

    /** Mancha de luz de um candeeiro no teto, a meio da sala. */
    private static void luzDoTeto(Graphics2D g, Rectangle2D r) {
        float[] fracoes = {0f, 1f};
        Color[] cores = {new Color(255, 240, 210, 38), new Color(255, 240, 210, 0)};
        java.awt.geom.Point2D centro = new java.awt.geom.Point2D.Double(r.getCenterX(), r.getCenterY() - r.getHeight() * 0.08);
        g.setPaint(new java.awt.RadialGradientPaint(centro, (float) (r.getWidth() * 0.62), fracoes, cores));
        g.fill(r);
    }

    private static void desenharEtiqueta(Graphics2D g, String nome, Rectangle2D r) {
        g.setFont(Tema.texto(Font.BOLD, 13f));
        String texto = nome.toUpperCase();
        FontMetrics fm = g.getFontMetrics();
        double x = r.getX() + 8;
        double y = r.getY() + 18;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Double(x - 4, y - 13, fm.stringWidth(texto) + 8, 18, 6, 6));
        g.setColor(new Color(0xC9D6E3));
        g.drawString(texto, (float) x, (float) y);
    }

    private static void desenharPortaExterior(Graphics2D g, double[] porta) {
        double px = porta[0];
        double py = porta[1];
        double ox = porta[2];
        double oy = porta[3];
        int lado = (int) porta[4];
        boolean horizontal = lado == Planta.ESQUERDA || lado == Planta.DIREITA;
        double meio = Planta.CORREDOR / 2 + 2;

        // Abertura na parede, com um pequeno patamar do lado de fora.
        double dist = Math.hypot(ox - px, oy - py);
        double ux = (ox - px) / dist;
        double uy = (oy - py) / dist;
        g.setPaint(BETAO_CORREDOR);
        g.setStroke(new BasicStroke((float) Planta.CORREDOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(px, py, px + ux * (Planta.PAREDE + 12), py + uy * (Planta.PAREDE + 12)));
        g.setColor(Tema.comAlfa(Tema.VERDE, 200));
        g.setStroke(new BasicStroke(2.5f));
        double ex = px + ux * Planta.PAREDE;
        double ey = py + uy * Planta.PAREDE;
        g.draw(new Line2D.Double(ex - uy * meio, ey + ux * meio, ex + uy * meio, ey - ux * meio));

        // Faixas de passadeira no exterior.
        g.setColor(new Color(0x3DDC97));
        for (int k = 0; k < 3; k++) {
            double t = 0.35 + k * 0.22;
            double cx = px + (ox - px) * t;
            double cy = py + (oy - py) * t;
            g.setColor(Tema.comAlfa(Tema.VERDE, 150 - k * 40));
            Path2D seta = new Path2D.Double();
            seta.moveTo(-5, -8);
            seta.lineTo(4, 0);
            seta.lineTo(-5, 8);
            AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
            at.rotate(Math.atan2(oy - py, ox - px) + Math.PI);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(at.createTransformedShape(seta));
        }

        // Placa "SAÍDA" junto à porta.
        g.setFont(Tema.texto(Font.BOLD, 9f));
        FontMetrics fm = g.getFontMetrics();
        String texto = "SAÍDA";
        double w = fm.stringWidth(texto) + 8;
        double sx = horizontal ? (ox + px) / 2 - w / 2 : px + meio + 4;
        double sy = horizontal ? py - meio - 16 : (oy + py) / 2 - 6;
        g.setColor(new Color(0x0E3B28));
        g.fill(new RoundRectangle2D.Double(sx, sy, w, 12, 3, 3));
        g.setColor(Tema.VERDE);
        g.drawString(texto, (float) (sx + 4), (float) (sy + 9.5));
    }

    // ------------------------------------------------------------------ mobília

    private static final Color MOVEL = new Color(0x5B6570);
    private static final Color MOVEL_ESCURO = new Color(0x2A3038);
    private static final Color MADEIRA_MOVEL = new Color(0x6A5646);

    private static void desenharMobilia(Graphics2D g, String nome, Rectangle2D r) {
        String n = simplificar(nome);
        double x = r.getX();
        double y = r.getY();
        double w = r.getWidth();
        double h = r.getHeight();
        g.setStroke(new BasicStroke(1f));
        if (n.contains("escada")) {
            escada(g, x + w * 0.58, y + h * 0.14, w * 0.34, h * 0.72);
        } else if (n.contains("heliporto")) {
            heliporto(g, r);
        } else if (n.contains("garagem")) {
            garagem(g, r);
        } else if (n.contains("laborat")) {
            for (int k = 0; k < 2; k++) {
                bancada(g, x + w * 0.1, y + h * (0.3 + k * 0.36), w * 0.5, h * 0.14);
            }
            caixa(g, x + w * 0.78, y + h * 0.12, w * 0.16, h * 0.2, MOVEL);
        } else if (n.contains("escritorio")) {
            secretaria(g, x + w * 0.12, y + h * 0.28, 0);
            secretaria(g, x + w * 0.12, y + h * 0.64, 0);
            estante(g, x + w * 0.9, y + h * 0.15, w * 0.06, h * 0.7);
        } else if (n.contains("camarat")) {
            for (int k = 0; k < 3; k++) {
                cama(g, x + w * (0.1 + k * 0.28), y + h * 0.1, w * 0.18, h * 0.36);
            }
        } else if (n.contains("armazem")) {
            for (int k = 0; k < 4; k++) {
                caixote(g, x + w * (0.08 + (k % 2) * 0.14), y + h * (0.2 + (k / 2) * 0.28), w * 0.12);
            }
            estante(g, x + w * 0.9, y + h * 0.1, w * 0.06, h * 0.8);
        } else if (n.contains("wc")) {
            for (int k = 0; k < 3; k++) {
                caixa(g, x + w * (0.08 + k * 0.2), y + h * 0.08, w * 0.16, h * 0.3, new Color(0x4B5763));
                g.setColor(new Color(0x8FA3B3));
                g.fill(new Ellipse2D.Double(x + w * (0.12 + k * 0.2), y + h * 0.14, w * 0.08, h * 0.12));
            }
        } else if (n.contains("seguranca")) {
            balcao(g, x + w * 0.08, y + h * 0.12, w * 0.5, h * 0.18);
            for (int k = 0; k < 3; k++) {
                g.setColor(new Color(0x1B2B3A));
                g.fill(new Rectangle2D.Double(x + w * (0.12 + k * 0.14), y + h * 0.14, w * 0.1, h * 0.05));
                g.setColor(new Color(0x2E6C8A));
                g.fill(new Rectangle2D.Double(x + w * (0.125 + k * 0.14), y + h * 0.145, w * 0.09, h * 0.035));
            }
            estante(g, x + w * 0.9, y + h * 0.15, w * 0.06, h * 0.7);
        } else if (n.contains("porteiro") || n.contains("recec")) {
            balcao(g, x + w * 0.1, y + h * 0.15, w * 0.42, h * 0.16);
            planta(g, x + w * 0.88, y + h * 0.2, 9);
        } else if (n.contains("hall")) {
            g.setColor(new Color(0x3F2F2F));
            g.fill(new RoundRectangle2D.Double(x + w * 0.25, y + h * 0.25, w * 0.5, h * 0.5, 10, 10));
            g.setColor(new Color(0x5A4545));
            g.draw(new RoundRectangle2D.Double(x + w * 0.28, y + h * 0.3, w * 0.44, h * 0.4, 8, 8));
            for (int k = 0; k < 4; k++) {
                pilar(g, x + w * (k % 2 == 0 ? 0.15 : 0.85), y + h * (k < 2 ? 0.22 : 0.8));
            }
        } else if (n.contains("corredor")) {
            planta(g, x + w * 0.08, y + h * 0.2, 8);
            planta(g, x + w * 0.92, y + h * 0.8, 8);
            for (int k = 0; k < 4; k++) {
                caixa(g, x + w * (0.3 + k * 0.1), y + h * 0.04, w * 0.08, h * 0.1, MOVEL);
            }
        } else {
            secretaria(g, x + w * 0.2, y + h * 0.3, 0);
        }
    }

    private static void caixa(Graphics2D g, double x, double y, double w, double h, Color cor) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Rectangle2D.Double(x + 2, y + 3, w, h));
        g.setColor(cor);
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(cor.brighter());
        g.draw(new Line2D.Double(x, y, x + w, y));
    }

    private static void secretaria(Graphics2D g, double x, double y, double angulo) {
        caixa(g, x, y, 52, 24, MADEIRA_MOVEL);
        g.setColor(MOVEL_ESCURO);
        g.fill(new Rectangle2D.Double(x + 16, y + 3, 20, 5));
        g.setColor(new Color(0x2E6C8A));
        g.fill(new Rectangle2D.Double(x + 17, y + 4, 18, 3));
        g.setColor(new Color(0x39414B));
        g.fill(new Ellipse2D.Double(x + 18, y + 26, 16, 14));
    }

    private static void estante(Graphics2D g, double x, double y, double w, double h) {
        caixa(g, x, y, w, h, MOVEL_ESCURO);
        g.setColor(new Color(0x6F5B4A));
        for (double yy = y + 6; yy < y + h - 4; yy += 10) {
            g.fill(new Rectangle2D.Double(x + 2, yy, w - 4, 4));
        }
    }

    private static void bancada(Graphics2D g, double x, double y, double w, double h) {
        caixa(g, x, y, w, h, new Color(0x6E7A86));
        Color[] cores = {new Color(0x3DDC97), new Color(0x7AA7FF), new Color(0xF5C542)};
        for (int k = 0; k < 4; k++) {
            g.setColor(Tema.comAlfa(cores[k % 3], 200));
            g.fill(new Ellipse2D.Double(x + 8 + k * w / 4.5, y + h * 0.25, 7, 7));
        }
    }

    private static void cama(Graphics2D g, double x, double y, double w, double h) {
        caixa(g, x, y, w, h, new Color(0x4A5566));
        g.setColor(new Color(0x9AA8B8));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 3, w - 6, h * 0.2, 6, 6));
        g.setColor(new Color(0x34506E));
        g.fill(new Rectangle2D.Double(x + 2, y + h * 0.45, w - 4, h * 0.52));
    }

    private static void caixote(Graphics2D g, double x, double y, double lado) {
        caixa(g, x, y, lado, lado, new Color(0x7A6246));
        g.setColor(new Color(0x5A4632));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Line2D.Double(x + 2, y + 2, x + lado - 2, y + lado - 2));
        g.draw(new Line2D.Double(x + lado - 2, y + 2, x + 2, y + lado - 2));
        g.setStroke(new BasicStroke(1f));
    }

    private static void balcao(Graphics2D g, double x, double y, double w, double h) {
        caixa(g, x, y, w, h, new Color(0x4D5967));
        g.setColor(new Color(0x39414B));
        g.fill(new Ellipse2D.Double(x + w * 0.4, y + h + 4, 16, 14));
    }

    private static void planta(Graphics2D g, double cx, double cy, double r) {
        g.setColor(new Color(0x3A2E26));
        g.fill(new Ellipse2D.Double(cx - r * 0.7, cy - r * 0.7, r * 1.4, r * 1.4));
        g.setColor(new Color(0x2F6B45));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            g.fill(new Ellipse2D.Double(cx + Math.cos(a) * r * 0.55 - r * 0.45, cy + Math.sin(a) * r * 0.55 - r * 0.45, r * 0.9, r * 0.9));
        }
    }

    private static void pilar(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(cx - 6, cy - 4, 14, 14));
        g.setColor(new Color(0x7D8792));
        g.fill(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
    }

    private static void escada(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x4A5058));
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(new Color(0x2B3036));
        int degraus = 9;
        for (int k = 1; k < degraus; k++) {
            double yy = y + k * h / degraus;
            g.draw(new Line2D.Double(x, yy, x + w, yy));
        }
        g.setColor(new Color(0x8B96A2));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(x - 3, y, x - 3, y + h));
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 70));
        Path2D seta = new Path2D.Double();
        seta.moveTo(x + w / 2, y + h * 0.2);
        seta.lineTo(x + w / 2 + 6, y + h * 0.32);
        seta.lineTo(x + w / 2 - 6, y + h * 0.32);
        seta.closePath();
        g.fill(seta);
    }

    private static void heliporto(Graphics2D g, Rectangle2D r) {
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        double raio = Math.min(r.getWidth(), r.getHeight()) * 0.36;
        g.setColor(new Color(0xF5C542));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(cx - raio, cy - raio, raio * 2, raio * 2));
        g.setFont(Tema.titulo(40f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(245, 197, 66, 170));
        g.drawString("H", (float) (cx - fm.stringWidth("H") / 2.0), (float) (cy + fm.getAscent() * 0.36));
        g.setStroke(new BasicStroke(1f));
    }

    private static void garagem(Graphics2D g, Rectangle2D r) {
        g.setColor(new Color(0xD9DDE2));
        g.setStroke(new BasicStroke(2f));
        for (int k = 0; k < 4; k++) {
            double x = r.getX() + r.getWidth() * (0.08 + k * 0.28);
            g.draw(new Line2D.Double(x, r.getY() + 4, x, r.getY() + r.getHeight() * 0.42));
        }
        g.setStroke(new BasicStroke(1f));
        double cx = r.getX() + r.getWidth() * 0.22;
        double cy = r.getY() + r.getHeight() * 0.08;
        double w = r.getWidth() * 0.2;
        double h = r.getHeight() * 0.32;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(cx + 3, cy + 4, w, h, 12, 12));
        g.setColor(new Color(0x7A2E35));
        g.fill(new RoundRectangle2D.Double(cx, cy, w, h, 12, 12));
        g.setColor(new Color(0x1E2B38));
        g.fill(new RoundRectangle2D.Double(cx + 4, cy + h * 0.2, w - 8, h * 0.2, 5, 5));
        g.fill(new RoundRectangle2D.Double(cx + 4, cy + h * 0.66, w - 8, h * 0.14, 5, 5));
    }

    // ------------------------------------------------------------------ personagens

    /**
     * Soldado visto de cima, virado para a direita quando {@code angulo} é 0.
     *
     * @param inimigo cores de inimigo (vermelho) ou do Tó Cruz (azul)
     * @param flash intensidade do clarão quando é atingido (0 a 1)
     */
    static void soldado(Graphics2D g, double x, double y, double angulo, boolean inimigo, double alfa, double flash, double escala) {
        Graphics2D s = (Graphics2D) g.create();
        s.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        s.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, alfa))));
        s.translate(x, y);
        s.scale(escala, escala);

        s.setColor(new Color(0, 0, 0, 110));
        s.fill(new Ellipse2D.Double(-13, -10, 30, 26));

        s.rotate(angulo);
        Color corpo = inimigo ? new Color(0x6E3B3B) : new Color(0x3F5A78);
        Color colete = inimigo ? new Color(0x4A2A2A) : new Color(0x26384C);
        Color detalhe = inimigo ? new Color(0xFF5A5F) : new Color(0x35D0FF);
        Color capacete = inimigo ? new Color(0xC7403B) : new Color(0x2E4560);

        // Arma.
        s.setColor(new Color(0x15181C));
        s.fill(new RoundRectangle2D.Double(4, 2.5, 22, 4.2, 2, 2));
        s.setColor(new Color(0x2A2F36));
        s.fill(new Rectangle2D.Double(8, 1.6, 6, 6));
        // Braços.
        s.setColor(corpo.darker());
        s.fill(new Ellipse2D.Double(3, -7, 11, 7));
        s.fill(new Ellipse2D.Double(5, 3, 11, 7));
        // Ombros e tronco.
        s.setColor(corpo);
        s.fill(new RoundRectangle2D.Double(-9, -12, 17, 24, 12, 12));
        s.setColor(colete);
        s.fill(new RoundRectangle2D.Double(-6, -8, 11, 16, 6, 6));
        s.setColor(detalhe);
        s.fill(new Rectangle2D.Double(-3, -8, 2.2, 16));
        // Cabeça/capacete.
        s.setColor(capacete);
        s.fill(new Ellipse2D.Double(-6, -6.5, 13, 13));
        s.setColor(new Color(255, 255, 255, 60));
        s.fill(new Ellipse2D.Double(-3.5, -4.5, 5, 4));
        if (!inimigo) {
            s.setColor(detalhe);
            s.setStroke(new BasicStroke(1.6f));
            s.draw(new java.awt.geom.Arc2D.Double(-5, -5.5, 11, 11, -50, 100, java.awt.geom.Arc2D.OPEN));
        }

        s.dispose();
        if (flash > 0) {
            Graphics2D f = (Graphics2D) g.create();
            f.translate(x, y);
            f.scale(escala, escala);
            brilhoRedondo(f, 0, 0, 22, inimigo ? new Color(0xFFE08A) : new Color(0xFF4D4D), Math.min(1, flash) * 2.5);
            f.dispose();
        }
    }

    /** Barra de vida por cima de uma personagem. */
    static void barraVida(Graphics2D g, double x, double y, double fracao, Color cor, double largura) {
        double f = Math.max(0, Math.min(1, fracao));
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(new RoundRectangle2D.Double(x - largura / 2 - 1, y - 1, largura + 2, 6, 4, 4));
        g.setColor(cor);
        g.fill(new RoundRectangle2D.Double(x - largura / 2, y, largura * f, 4, 3, 3));
    }

    // ------------------------------------------------------------------ objetos

    static void kit(Graphics2D g, double cx, double cy, double brilho) {
        brilhoRedondo(g, cx, cy, 26, Tema.VERDE, brilho);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(cx - 11 + 2, cy - 8 + 3, 22, 16, 4, 4));
        g.setColor(new Color(0xE8EDF2));
        g.fill(new RoundRectangle2D.Double(cx - 11, cy - 8, 22, 16, 4, 4));
        g.setColor(new Color(0x9AA6B2));
        g.fill(new Rectangle2D.Double(cx - 4, cy - 10, 8, 3));
        g.setColor(new Color(0x21A366));
        g.fill(new Rectangle2D.Double(cx - 1.8, cy - 5.5, 3.6, 11));
        g.fill(new Rectangle2D.Double(cx - 5.5, cy - 1.8, 11, 3.6));
    }

    static void colete(Graphics2D g, double cx, double cy, double brilho) {
        brilhoRedondo(g, cx, cy, 26, Tema.AZUL, brilho);
        Path2D forma = new Path2D.Double();
        forma.moveTo(cx - 10, cy - 10);
        forma.lineTo(cx - 4, cy - 10);
        forma.lineTo(cx - 2, cy - 6);
        forma.lineTo(cx + 2, cy - 6);
        forma.lineTo(cx + 4, cy - 10);
        forma.lineTo(cx + 10, cy - 10);
        forma.lineTo(cx + 11, cy + 10);
        forma.lineTo(cx - 11, cy + 10);
        forma.closePath();
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(AffineTransform.getTranslateInstance(2, 3).createTransformedShape(forma));
        g.setColor(new Color(0x3E5A7A));
        g.fill(forma);
        g.setColor(new Color(0x7AA7FF));
        g.fill(new Rectangle2D.Double(cx - 7, cy - 2, 14, 3));
        g.fill(new Rectangle2D.Double(cx - 7, cy + 4, 14, 3));
    }

    /** O objeto a recuperar: um bidão de químicos ou uma mala, com um anel dourado a pulsar. */
    static void objetivo(Graphics2D g, String nome, double cx, double cy, double pulso) {
        double r = 22 + 6 * pulso;
        g.setColor(Tema.comAlfa(Tema.DOURADO, (int) (120 * (1 - pulso))));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        brilhoRedondo(g, cx, cy, 30, Tema.DOURADO, 0.8);

        String n = simplificar(nome);
        if (n.contains("quim") || n.contains("bio") || n.contains("arma") || n.contains("radio")) {
            g.setColor(new Color(0, 0, 0, 100));
            g.fill(new Ellipse2D.Double(cx - 10 + 2, cy - 10 + 3, 20, 20));
            g.setColor(new Color(0xD9A72B));
            g.fill(new Ellipse2D.Double(cx - 10, cy - 10, 20, 20));
            g.setColor(new Color(0x8C6A12));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
            Path2D triangulo = new Path2D.Double();
            triangulo.moveTo(cx, cy - 5);
            triangulo.lineTo(cx + 5, cy + 4);
            triangulo.lineTo(cx - 5, cy + 4);
            triangulo.closePath();
            g.setColor(new Color(0x1B1B1B));
            g.fill(triangulo);
            g.setColor(new Color(0xF5C542));
            g.fill(new Rectangle2D.Double(cx - 0.7, cy - 2, 1.4, 3.5));
        } else {
            g.setColor(new Color(0, 0, 0, 100));
            g.fill(new RoundRectangle2D.Double(cx - 12 + 2, cy - 8 + 3, 24, 16, 4, 4));
            g.setColor(new Color(0x6B4A2B));
            g.fill(new RoundRectangle2D.Double(cx - 12, cy - 8, 24, 16, 4, 4));
            g.setColor(new Color(0xC9A45C));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(cx - 4, cy - 12, 8, 5, 3, 3));
            g.fill(new Rectangle2D.Double(cx - 2, cy - 1, 4, 3));
        }
        g.setStroke(new BasicStroke(1f));
    }

    static void brilhoRedondo(Graphics2D g, double cx, double cy, double raio, Color cor, double intensidade) {
        if (intensidade <= 0) {
            return;
        }
        float[] fracoes = {0f, 1f};
        Color[] cores = {Tema.comAlfa(cor, (int) (90 * Math.min(1, intensidade))), Tema.comAlfa(cor, 0)};
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Double(cx, cy), (float) raio, fracoes, cores));
        g.fill(new Ellipse2D.Double(cx - raio, cy - raio, raio * 2, raio * 2));
    }
}
