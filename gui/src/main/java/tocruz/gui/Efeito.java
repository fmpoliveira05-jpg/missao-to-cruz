package tocruz.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Efeitos visuais de curta duração (disparos, impactos, números de dano, anéis).
 *
 * @author Francisco Oliveira
 */
abstract class Efeito {

    private final double duracao;
    private double tempo;

    Efeito(double duracao) {
        this.duracao = duracao;
    }

    /** @return se o efeito terminou */
    boolean avancar(double dt) {
        tempo += dt;
        return tempo >= duracao;
    }

    /** @return progresso entre 0 e 1 */
    double progresso() {
        return Math.min(1, tempo / duracao);
    }

    abstract void desenhar(Graphics2D g);

    /** Rasto de uma bala. */
    static final class Rasto extends Efeito {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;
        private final Color cor;

        Rasto(double x1, double y1, double x2, double y2, Color cor) {
            super(150);
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.cor = cor;
        }

        @Override
        void desenhar(Graphics2D g) {
            double p = progresso();
            double inicio = Math.max(0, p * 1.6 - 0.6);
            double fim = Math.min(1, p * 1.6);
            double ax = x1 + (x2 - x1) * inicio;
            double ay = y1 + (y2 - y1) * inicio;
            double bx = x1 + (x2 - x1) * fim;
            double by = y1 + (y2 - y1) * fim;
            int alfa = (int) (255 * (1 - p * 0.6));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Tema.comAlfa(cor, alfa / 4));
            g.draw(new Line2D.Double(ax, ay, bx, by));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 250, 225, alfa));
            g.draw(new Line2D.Double(ax, ay, bx, by));
        }
    }

    /** Clarão na boca da arma. */
    static final class Clarao extends Efeito {
        private final double x;
        private final double y;
        private final double angulo;

        Clarao(double x, double y, double angulo) {
            super(90);
            this.x = x;
            this.y = y;
            this.angulo = angulo;
        }

        @Override
        void desenhar(Graphics2D g) {
            double p = progresso();
            double t = 14 * (1 - p * 0.5);
            Path2D estrela = new Path2D.Double();
            for (int k = 0; k < 10; k++) {
                double a = angulo + k * Math.PI / 5;
                double r = k % 2 == 0 ? t : t * 0.35;
                double px = x + Math.cos(a) * r;
                double py = y + Math.sin(a) * r;
                if (k == 0) {
                    estrela.moveTo(px, py);
                } else {
                    estrela.lineTo(px, py);
                }
            }
            estrela.closePath();
            Desenho.brilhoRedondo(g, x, y, 34, new Color(0xFFD27A), 1 - p);
            g.setColor(new Color(255, 236, 170, (int) (255 * (1 - p))));
            g.fill(estrela);
        }
    }

    /** Faíscas no ponto de impacto. */
    static final class Impacto extends Efeito {
        private final double x;
        private final double y;
        private final double[] angulos = new double[9];
        private final double[] velocidades = new double[9];
        private final Color cor;

        Impacto(double x, double y, Color cor, long semente) {
            super(320);
            this.x = x;
            this.y = y;
            this.cor = cor;
            Random r = new Random(semente);
            for (int k = 0; k < angulos.length; k++) {
                angulos[k] = r.nextDouble() * Math.PI * 2;
                velocidades[k] = 12 + r.nextDouble() * 20;
            }
        }

        @Override
        void desenhar(Graphics2D g) {
            double p = progresso();
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 0; k < angulos.length; k++) {
                double d1 = velocidades[k] * p;
                double d2 = velocidades[k] * Math.min(1, p + 0.25);
                g.setColor(Tema.comAlfa(k % 3 == 0 ? cor : new Color(0xFFE08A), (int) (255 * (1 - p))));
                g.draw(new Line2D.Double(x + Math.cos(angulos[k]) * d1, y + Math.sin(angulos[k]) * d1,
                        x + Math.cos(angulos[k]) * d2, y + Math.sin(angulos[k]) * d2));
            }
        }
    }

    /** Texto que sobe e desaparece (dano, cura, avisos). */
    static final class Texto extends Efeito {
        private final String texto;
        private final double x;
        private final double y;
        private final Color cor;
        private final float tamanho;

        Texto(String texto, double x, double y, Color cor, float tamanho, double duracao) {
            super(duracao);
            this.texto = texto;
            this.x = x;
            this.y = y;
            this.cor = cor;
            this.tamanho = tamanho;
        }

        @Override
        void desenhar(Graphics2D g) {
            double p = progresso();
            double escala = p < 0.15 ? 0.6 + p / 0.15 * 0.5 : 1.1 - Math.min(0.1, (p - 0.15));
            float alfa = (float) (p < 0.7 ? 1 : 1 - (p - 0.7) / 0.3);
            Graphics2D t = (Graphics2D) g.create();
            t.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alfa)));
            t.translate(x, y - 30 * p);
            t.scale(escala, escala);
            t.setFont(Tema.titulo(tamanho));
            FontMetrics fm = t.getFontMetrics();
            float tx = -fm.stringWidth(texto) / 2f;
            t.setColor(new Color(0, 0, 0, 200));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    t.drawString(texto, tx + dx * 1.5f, dy * 1.5f);
                }
            }
            t.setColor(cor);
            t.drawString(texto, tx, 0);
            t.dispose();
        }
    }

    /** Anel que se expande (cura, objetivo apanhado). */
    static final class Anel extends Efeito {
        private final double x;
        private final double y;
        private final Color cor;
        private final double raio;

        Anel(double x, double y, Color cor, double raio) {
            super(620);
            this.x = x;
            this.y = y;
            this.cor = cor;
            this.raio = raio;
        }

        @Override
        void desenhar(Graphics2D g) {
            double p = progresso();
            double r = 10 + raio * p;
            g.setStroke(new BasicStroke((float) (5 * (1 - p) + 1)));
            g.setColor(Tema.comAlfa(cor, (int) (230 * (1 - p))));
            g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        }
    }
}
