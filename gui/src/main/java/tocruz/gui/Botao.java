package tocruz.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/**
 * Botão plano com cantos redondos, no estilo do resto da interface.
 *
 * @author Francisco Oliveira
 */
class Botao extends JButton {

    private static final long serialVersionUID = 1L;

    private final Color cor;
    private final boolean cheio;

    /**
     * @param texto texto do botão
     * @param cor cor de destaque
     * @param cheio se o fundo é preenchido com a cor (ação principal) ou só contornado
     */
    Botao(String texto, Color cor, boolean cheio) {
        super(texto);
        this.cor = cor;
        this.cheio = cheio;
        setFont(Tema.texto(Font.BOLD, 13f));
        setForeground(cheio ? Tema.FUNDO : cor);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 28, 36);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean ativo = isEnabled();
        boolean sobre = ativo && getModel().isRollover();
        boolean premido = ativo && getModel().isPressed();

        RoundRectangle2D forma = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
        Color base = ativo ? cor : Tema.CARTAO_BORDA;
        if (cheio && ativo) {
            g.setColor(premido ? base.darker() : sobre ? base.brighter() : base);
            g.fill(forma);
        } else {
            g.setColor(Tema.comAlfa(base, !ativo ? 0 : premido ? 70 : sobre ? 45 : 18));
            g.fill(forma);
            g.setColor(base);
            g.setStroke(new BasicStroke(1.4f));
            g.draw(forma);
        }

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        Color texto = !ativo ? Tema.comAlfa(Tema.TEXTO_SUAVE, 110) : cheio ? Tema.FUNDO : cor;
        g.setColor(texto);
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(getText(), x, y);
        g.dispose();
    }
}
