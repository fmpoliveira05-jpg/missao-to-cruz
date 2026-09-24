package tocruz.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import tocruz.items.ItemCura;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;
import tocruz.personagens.ToCruz;

/**
 * Coluna da direita: estado do Tó Cruz, ações do turno, opções e registo do que aconteceu.
 *
 * @author Francisco Oliveira
 */
class PainelLateral extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int LARGURA = 372;

    final Botao atacar = new Botao("Atacar", Tema.VERMELHO, true);
    final Botao usarKit = new Botao("Usar kit", Tema.VERDE, false);
    final Botao ficar = new Botao("Ficar", Tema.TEXTO_SUAVE, false);
    final Botao sair = new Botao("Sair do edifício", Tema.VERDE, true);
    final Botao kitUsar = new Botao("Usar já", Tema.VERDE, true);
    final Botao kitGuardar = new Botao("Guardar", Tema.CIANO, false);
    final Botao kitDeixar = new Botao("Deixar", Tema.TEXTO_SUAVE, false);
    final Botao novaSimulacao = new Botao("Nova simulação", Tema.CIANO, false);
    final Botao planoAutomatico = new Botao("Plano automático", Tema.DOURADO, false);
    final Botao jogarSozinho = new Botao("Jogar sozinho", Tema.CIANO, true);
    final Botao abrirMissao = new Botao("Abrir missão…", Tema.TEXTO_SUAVE, false);
    final JCheckBox verSugestao = caixa("Mostrar caminho sugerido", true);
    final JCheckBox verKit = caixa("Mostrar kit mais próximo", true);

    private final Cabecalho cabecalho = new Cabecalho();
    private final CartaoEstado estado = new CartaoEstado();
    private final CardLayout cartasAcoes = new CardLayout();
    private final JPanel acoes = new JPanel(cartasAcoes);
    private final JLabel textoKit = new JLabel();
    private final DefaultListModel<String> linhas = new DefaultListModel<>();
    private final JList<String> registo = new JList<>(linhas);

    PainelLateral() {
        super(new BorderLayout(0, 12));
        setBackground(Tema.PAINEL);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setPreferredSize(new Dimension(LARGURA, 700));

        JPanel topo = coluna();
        topo.add(cabecalho);
        topo.add(Box.createVerticalStrut(12));
        topo.add(estado);
        topo.add(Box.createVerticalStrut(12));

        JPanel normais = new JPanel(new GridLayout(2, 2, 8, 8));
        normais.setOpaque(false);
        normais.add(atacar);
        normais.add(usarKit);
        normais.add(ficar);
        normais.add(sair);

        JPanel decisao = new JPanel(new BorderLayout(0, 6));
        decisao.setOpaque(false);
        textoKit.setForeground(Tema.VERDE);
        textoKit.setFont(Tema.texto(Font.BOLD, 13f));
        decisao.add(textoKit, BorderLayout.NORTH);
        JPanel opcoesKit = new JPanel(new GridLayout(1, 3, 8, 0));
        opcoesKit.setOpaque(false);
        opcoesKit.add(kitUsar);
        opcoesKit.add(kitGuardar);
        opcoesKit.add(kitDeixar);
        decisao.add(opcoesKit, BorderLayout.CENTER);

        acoes.setOpaque(false);
        acoes.add(normais, "normais");
        acoes.add(decisao, "kit");
        acoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        acoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        topo.add(acoes);
        topo.add(Box.createVerticalStrut(10));
        topo.add(verSugestao);
        topo.add(verKit);
        add(topo, BorderLayout.NORTH);

        registo.setBackground(Tema.CARTAO);
        registo.setCellRenderer(new LinhaRegisto());
        registo.setFocusable(false);
        JScrollPane rolar = new JScrollPane(registo);
        rolar.setBorder(BorderFactory.createLineBorder(Tema.CARTAO_BORDA));
        rolar.getViewport().setBackground(Tema.CARTAO);
        rolar.getVerticalScrollBar().setUnitIncrement(16);
        rolar.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolar.getVerticalScrollBar().setUI(new BarraFina());
        rolar.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        JPanel meio = new JPanel(new BorderLayout(0, 6));
        meio.setOpaque(false);
        JLabel titulo = new JLabel("REGISTO DA MISSÃO");
        titulo.setFont(Tema.texto(Font.BOLD, 11f));
        titulo.setForeground(Tema.TEXTO_SUAVE);
        meio.add(titulo, BorderLayout.NORTH);
        meio.add(rolar, BorderLayout.CENTER);
        add(meio, BorderLayout.CENTER);

        JPanel fundo = new JPanel(new GridLayout(2, 2, 8, 8));
        fundo.setOpaque(false);
        fundo.add(novaSimulacao);
        fundo.add(planoAutomatico);
        fundo.add(abrirMissao);
        fundo.add(jogarSozinho);
        add(fundo, BorderLayout.SOUTH);
    }

    private static JPanel coluna() {
        JPanel painel = new JPanel();
        painel.setOpaque(false);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        return painel;
    }

    private static JCheckBox caixa(String texto, boolean marcada) {
        JCheckBox caixa = new JCheckBox(texto, marcada);
        caixa.setOpaque(false);
        caixa.setForeground(Tema.TEXTO_SUAVE);
        caixa.setFont(Tema.texto(Font.PLAIN, 12.5f));
        caixa.setFocusPainted(false);
        caixa.setIcon(new IconeCaixa(false));
        caixa.setSelectedIcon(new IconeCaixa(true));
        caixa.setIconTextGap(8);
        caixa.setAlignmentX(Component.LEFT_ALIGNMENT);
        return caixa;
    }

    void setMissao(String codigo, long versao) {
        cabecalho.subtitulo = "Missão «" + codigo + "» · versão " + versao;
        cabecalho.repaint();
    }

    /** Atualiza os indicadores e os botões ao estado do motor. */
    void atualizar(MotorJogo motor, boolean aJogarSozinho) {
        estado.motor = motor;
        estado.repaint();

        MotorJogo.Estado fase = motor.getEstado();
        boolean manual = !aJogarSozinho;
        cartasAcoes.show(acoes, fase == MotorJogo.Estado.DECIDIR_KIT ? "kit" : "normais");
        if (fase == MotorJogo.Estado.DECIDIR_KIT) {
            ItemCura kit = motor.getKitPorDecidir();
            textoKit.setText("Encontrou um kit de " + kit.getVida_recuperada() + " pontos de vida.");
        }
        atacar.setEnabled(manual && fase == MotorJogo.Estado.A_JOGAR && motor.emCombate());
        usarKit.setEnabled(manual && motor.podeUsarKit());
        ficar.setEnabled(manual && fase == MotorJogo.Estado.A_JOGAR && !motor.emCombate());
        sair.setEnabled(manual && motor.podeSair());
        kitUsar.setEnabled(manual && motor.podeDecidir(MotorJogo.DecisaoKit.USAR));
        kitGuardar.setEnabled(manual && motor.podeDecidir(MotorJogo.DecisaoKit.GUARDAR));
        kitDeixar.setEnabled(manual && motor.podeDecidir(MotorJogo.DecisaoKit.DEIXAR));
        jogarSozinho.setText(aJogarSozinho ? "Parar" : "Jogar sozinho");
        jogarSozinho.setEnabled(fase != MotorJogo.Estado.TERMINADO);
        jogarSozinho.repaint();
    }

    void limparRegisto() {
        linhas.clear();
    }

    void registar(String linha) {
        linhas.addElement(linha);
        registo.ensureIndexIsVisible(linhas.size() - 1);
        SwingUtilities.invokeLater(() -> registo.ensureIndexIsVisible(linhas.size() - 1));
    }

    // ------------------------------------------------------------------ componentes desenhados

    /** Barra de deslocamento fina, sem setas. */
    private static final class BarraFina extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = Tema.CARTAO_BORDA;
            trackColor = Tema.CARTAO;
        }

        @Override
        protected javax.swing.JButton createDecreaseButton(int orientacao) {
            return semBotao();
        }

        @Override
        protected javax.swing.JButton createIncreaseButton(int orientacao) {
            return semBotao();
        }

        private static javax.swing.JButton semBotao() {
            javax.swing.JButton botao = new javax.swing.JButton();
            botao.setPreferredSize(new Dimension(0, 0));
            return botao;
        }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, java.awt.Rectangle r) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isThumbRollover() ? Tema.DIVISAO_BORDA : Tema.CARTAO_BORDA);
            g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
            g.setColor(Tema.CARTAO);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    /** Quadrado com visto, no lugar do ícone da caixa de seleção. */
    private static final class IconeCaixa implements javax.swing.Icon {
        private final boolean marcada;

        IconeCaixa(boolean marcada) {
            this.marcada = marcada;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Double(x + 0.5, y + 0.5, 15, 15, 5, 5);
            if (marcada) {
                g.setColor(Tema.CIANO);
                g.fill(forma);
                g.setColor(Tema.FUNDO);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.geom.Path2D visto = new java.awt.geom.Path2D.Double();
                visto.moveTo(x + 4, y + 8.5);
                visto.lineTo(x + 7, y + 11.5);
                visto.lineTo(x + 12, y + 5);
                g.draw(visto);
            } else {
                g.setColor(Tema.CARTAO_BORDA);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(forma);
            }
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 17;
        }

        @Override
        public int getIconHeight() {
            return 17;
        }
    }

    /** Título da aplicação e missão aberta. */
    private static final class Cabecalho extends JComponent {
        private static final long serialVersionUID = 1L;
        private String subtitulo = "";

        Cabecalho() {
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(LARGURA, 52));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Tema.CIANO);
            g.fill(new RoundRectangle2D.Double(0, 4, 5, 42, 4, 4));
            g.setFont(Tema.titulo(22f));
            g.setColor(Tema.TEXTO);
            g.drawString("MISSÃO TÓ CRUZ", 16, 26);
            g.setFont(Tema.texto(Font.PLAIN, 12.5f));
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(subtitulo, 16, 45);
            g.dispose();
        }
    }

    /** Vida, mochila, alvo e fase do jogo. */
    private static final class CartaoEstado extends JComponent {
        private static final long serialVersionUID = 1L;
        private transient MotorJogo motor;

        CartaoEstado() {
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(LARGURA, 176));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            g.setColor(Tema.CARTAO);
            g.fill(new RoundRectangle2D.Double(0, 0, w - 1, getHeight() - 1, 16, 16));
            g.setColor(Tema.CARTAO_BORDA);
            g.draw(new RoundRectangle2D.Double(0, 0, w - 1, getHeight() - 1, 16, 16));
            if (motor == null) {
                g.dispose();
                return;
            }
            ToCruz to = motor.getToCruz();

            // Fase do jogo.
            String fase;
            Color corFase;
            switch (motor.getEstado()) {
                case ESCOLHER_ENTRADA:
                    fase = "Escolha uma entrada no mapa";
                    corFase = Tema.VERDE;
                    break;
                case DECIDIR_KIT:
                    fase = "Kit de vida encontrado";
                    corFase = Tema.VERDE;
                    break;
                case TERMINADO:
                    fase = motor.isSucesso() ? "Missão cumprida" : "Missão falhada";
                    corFase = motor.isSucesso() ? Tema.VERDE : Tema.VERMELHO;
                    break;
                default:
                    fase = motor.emCombate() ? "Em combate" : "A explorar";
                    corFase = motor.emCombate() ? Tema.VERMELHO : Tema.CIANO;
                    break;
            }
            g.setFont(Tema.texto(Font.BOLD, 11f));
            String turno = "TURNO " + motor.getTurno();
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(turno, 16, 24);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(corFase);
            g.fill(new Ellipse2D.Double(w - 24 - fm.stringWidth(fase.toUpperCase()), 16, 8, 8));
            g.drawString(fase.toUpperCase(), w - 12 - fm.stringWidth(fase.toUpperCase()), 24);

            // Divisão atual.
            g.setFont(Tema.titulo(17f));
            g.setColor(Tema.TEXTO);
            String onde = motor.getDivisaoAtual() == null ? "Fora do edifício" : motor.getDivisaoAtual().getName();
            g.drawString(onde, 16, 50);

            // Barra de vida (a escala vai até 125 para caber um colete).
            long vida = Math.max(0, to.getVida());
            double maximo = Math.max(125, vida);
            int barraX = 16;
            int barraY = 66;
            int barraW = w - 32;
            g.setFont(Tema.texto(Font.PLAIN, 12f));
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("Vida", barraX, barraY + 2);
            g.setFont(Tema.texto(Font.BOLD, 13f));
            g.setColor(Tema.TEXTO);
            String textoVida = vida + " / " + PlaneadorMissao.VIDA_INICIAL;
            g.drawString(textoVida, barraX + barraW - g.getFontMetrics().stringWidth(textoVida), barraY + 2);
            g.setColor(Tema.FUNDO);
            g.fill(new RoundRectangle2D.Double(barraX, barraY + 10, barraW, 12, 12, 12));
            Color corVida = vida > 60 ? Tema.VERDE : vida > 30 ? Tema.DOURADO : Tema.VERMELHO;
            double cheio = barraW * Math.min(vida, PlaneadorMissao.VIDA_INICIAL) / maximo;
            g.setPaint(new GradientPaint(barraX, 0, corVida.darker(), (float) (barraX + cheio), 0, corVida));
            g.fill(new RoundRectangle2D.Double(barraX, barraY + 10, cheio, 12, 12, 12));
            if (vida > PlaneadorMissao.VIDA_INICIAL) {
                double extra = barraW * (vida - PlaneadorMissao.VIDA_INICIAL) / maximo;
                g.setColor(Tema.AZUL);
                g.fill(new RoundRectangle2D.Double(barraX + cheio - 6, barraY + 10, extra + 6, 12, 12, 12));
            }
            g.setColor(Tema.TEXTO_SUAVE);
            double marca = barraX + barraW * PlaneadorMissao.VIDA_INICIAL / maximo;
            g.setStroke(new BasicStroke(1f));
            g.drawLine((int) marca, barraY + 8, (int) marca, barraY + 24);

            // Mochila.
            int linhaY = barraY + 52;
            g.setFont(Tema.texto(Font.PLAIN, 12f));
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("Mochila", 16, linhaY + 5);
            ItemCura[] kits = new ItemCura[to.getMochila().size()];
            for (int i = kits.length - 1; i >= 0; i--) {
                kits[i] = to.getMochila().pop();
            }
            for (ItemCura kit : kits) {
                to.getMochila().push(kit);
            }
            for (int i = 0; i < PlaneadorMissao.CAPACIDADE_MOCHILA; i++) {
                double x = 76 + i * 40;
                RoundRectangle2D espaco = new RoundRectangle2D.Double(x, linhaY - 12, 34, 24, 8, 8);
                if (i < kits.length) {
                    g.setColor(Tema.comAlfa(Tema.VERDE, 50));
                    g.fill(espaco);
                    g.setColor(Tema.VERDE);
                    g.draw(espaco);
                    g.setFont(Tema.texto(Font.BOLD, 11f));
                    String pontos = "+" + kits[i].getVida_recuperada();
                    g.drawString(pontos, (float) (x + 17 - g.getFontMetrics().stringWidth(pontos) / 2.0), linhaY + 4);
                } else {
                    g.setColor(Tema.CARTAO_BORDA);
                    g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
                    g.draw(espaco);
                    g.setStroke(new BasicStroke(1f));
                }
            }

            // Alvo.
            linhaY += 36;
            g.setFont(Tema.texto(Font.PLAIN, 12f));
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("Alvo", 16, linhaY + 5);
            boolean comAlvo = to.isColectedAlvo();
            g.setFont(Tema.texto(Font.BOLD, 12.5f));
            g.setColor(comAlvo ? Tema.DOURADO : Tema.TEXTO_SUAVE);
            g.drawString(comAlvo ? "Apanhado — falta sair com vida" : "Por apanhar", 76, linhaY + 5);
            g.dispose();
        }
    }

    /** Cor de cada linha do registo conforme o que descreve. */
    private static final class LinhaRegisto extends JLabel implements ListCellRenderer<String> {
        private static final long serialVersionUID = 1L;

        LinhaRegisto() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 8));
            setVerticalAlignment(TOP);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> lista, String texto, int indice,
                                                      boolean selecionado, boolean foco) {
            Color cor = Tema.TEXTO_SUAVE;
            int estilo = Font.PLAIN;
            if (texto.startsWith("Turno")) {
                cor = Tema.TEXTO;
                estilo = Font.BOLD;
            } else if (texto.contains("atacou:") || texto.contains("morreu") || texto.contains("falhada")) {
                cor = Tema.VERMELHO;
            } else if (texto.contains("eliminou") || texto.contains("kit") || texto.contains("colete") || texto.contains("cumprida")) {
                cor = Tema.VERDE;
            } else if (texto.contains("alvo") || texto.startsWith("Plano") || texto.startsWith("Entrar")
                    || texto.startsWith("Até") || texto.startsWith("Regresso") || texto.startsWith("Missão viável")) {
                cor = Tema.DOURADO;
            }
            setForeground(cor);
            setFont(Tema.texto(estilo, 12.5f));
            int largura = Math.max(120, LARGURA - 36 - 2 * 18 - 56);
            setText("<html><body style='width:" + largura + "px'>" + escapar(texto) + "</body></html>");
            return this;
        }

        private static String escapar(String texto) {
            return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
