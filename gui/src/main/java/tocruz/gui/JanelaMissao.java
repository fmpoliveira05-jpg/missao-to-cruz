package tocruz.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import tocruz.importar.ImportarMapa;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.missoes.Missao;
import tocruz.motor.Dificuldade;
import tocruz.motor.JogadorAutomatico;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;
import tocruz.motor.Plano;

/**
 * Interface gráfica do simulador.
 *
 * <p>Mostra o edifício como uma planta vista de cima e deixa jogar com o rato e o teclado:
 * escolher a dificuldade, clicar numa entrada para entrar, clicar numa sala vizinha para avançar e
 * usar os botões (ou as teclas A, K, F e S) para atacar, usar um kit, ficar ou sair. Cada ação é
 * animada a partir dos eventos do motor. Todas as regras vêm de {@link MotorJogo}, o mesmo motor
 * usado pela aplicação de consola.</p>
 *
 * <p>Uso: {@code JanelaMissao [ficheiro.json]}. Sem ficheiro, abre a missão de exemplo.</p>
 *
 * @author Francisco Oliveira
 */
public class JanelaMissao extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int PAUSA_JOGO_SOZINHO_MS = 350;

    private final PainelPlanta painel = new PainelPlanta();
    private final PainelLateral lateral = new PainelLateral();
    private final transient JogadorAutomatico jogador = new JogadorAutomatico();
    private final Timer relogio;

    private transient Missao missao;
    private transient Planta planta;
    private transient Cena cena;
    private transient MotorJogo motor;
    private transient Random aleatorio = new Random();
    private transient Dificuldade dificuldade = Dificuldade.AGENTE;
    private boolean jogarSozinho;
    private boolean estavaOcupada;
    private long ultimoTique;
    private double parado;

    /**
     * @param missao missão a abrir
     */
    public JanelaMissao(Missao missao) {
        super("Missão Tó Cruz");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FUNDO);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(painel, BorderLayout.CENTER);
        getContentPane().add(lateral, BorderLayout.EAST);
        setMinimumSize(new Dimension(1180, 720));

        relogio = new Timer(16, e -> tique());

        painel.setOuvinte(new PainelPlanta.Ouvinte() {
            @Override
            public void clicouSala(Divisao divisao) {
                clicouEm(divisao);
            }

            @Override
            public void escolheuDificuldade(Dificuldade escolhida) {
                novaSimulacao(escolhida);
            }
        });
        lateral.atacar.addActionListener(e -> agir(() -> motor.atacar()));
        lateral.usarKit.addActionListener(e -> agir(() -> motor.usarKit()));
        lateral.ficar.addActionListener(e -> agir(() -> motor.aguardar()));
        lateral.sair.addActionListener(e -> agir(() -> motor.sair()));
        lateral.kitUsar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.USAR)));
        lateral.kitGuardar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.GUARDAR)));
        lateral.kitDeixar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.DEIXAR)));
        lateral.novaSimulacao.addActionListener(e -> pedirDificuldade());
        lateral.planoAutomatico.addActionListener(e -> alternarPlano());
        lateral.jogarSozinho.addActionListener(e -> alternarJogoSozinho());
        lateral.abrirMissao.addActionListener(e -> escolherFicheiro());
        lateral.verSugestao.addActionListener(e -> painel.setMostrarSugestao(lateral.verSugestao.isSelected()));
        lateral.verKit.addActionListener(e -> painel.setMostrarKit(lateral.verKit.isSelected()));
        lateral.seguir.addActionListener(e -> painel.setSeguir(lateral.seguir.isSelected()));

        atalho("A", () -> seAtivo(lateral.atacar));
        atalho("K", () -> seAtivo(lateral.usarKit));
        atalho("F", () -> seAtivo(lateral.ficar));
        atalho("SPACE", () -> seAtivo(lateral.ficar));
        atalho("S", () -> seAtivo(lateral.sair));
        atalho("M", () -> lateral.seguir.doClick(0));
        atalho("1", () -> seAtivo(lateral.kitUsar));
        atalho("2", () -> seAtivo(lateral.kitGuardar));
        atalho("3", () -> seAtivo(lateral.kitDeixar));

        abrir(missao);
        relogio.start();
    }

    private void atalho(String tecla, Runnable acao) {
        String nome = "atalho-" + tecla;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla), nome);
        getRootPane().getActionMap().put(nome, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private static void seAtivo(Botao botao) {
        if (botao.isEnabled()) {
            botao.doClick(0);
        }
    }

    // ------------------------------------------------------------------ missão e simulações

    private void abrir(Missao novaMissao) {
        this.missao = novaMissao;
        this.planta = Planta.criar(novaMissao.getEdificio());
        lateral.setMissao(novaMissao.getcod_missao(), novaMissao.getVersao());
        painel.setPlanta(planta);
        painel.setNomeMissao(novaMissao.getcod_missao());
        novaSimulacao(dificuldade);
        lateral.limparRegisto();
        lateral.registar("Escolha a dificuldade para começar a missão «" + novaMissao.getcod_missao() + "».");
        pedirDificuldade();
    }

    private void pedirDificuldade() {
        jogarSozinho = false;
        painel.setEscolherDificuldade(true);
        atualizarBotoes();
    }

    /** Começa uma simulação numa cópia do edifício (inimigos e itens na posição original). */
    private void novaSimulacao(Dificuldade escolhida) {
        dificuldade = escolhida;
        jogarSozinho = false;
        Edificio base = missao.getEdificio();
        motor = new MotorJogo(new Edificio(base.getId(), base.getName(), base.getPlantaEdificio()), aleatorio, escolhida);
        cena = new Cena(planta);
        cena.carregar(motor);
        painel.setJogo(motor, cena);
        painel.setEscolherDificuldade(false);
        lateral.limparRegisto();
        lateral.registar("Nível " + escolhida.getNome() + ": " + escolhida.getDescricao());
        lateral.registar("Clique numa das entradas (contorno verde) para o Tó Cruz entrar.");
        atualizarBotoes();
    }

    private void clicouEm(Divisao divisao) {
        if (jogarSozinho || cena.ocupada()) {
            return;
        }
        if (motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA) {
            if (divisao.isEntrada_saida()) {
                agir(() -> motor.entrar(divisao));
            } else {
                lateral.registar("O Tó Cruz só pode entrar por uma entrada/saída.");
            }
        } else if (motor.getEstado() == MotorJogo.Estado.A_JOGAR) {
            if (motor.emCombate()) {
                lateral.registar("Em combate não pode sair da sala: ataque (A) ou use um kit (K).");
            } else if (motor.getDestinosPossiveis().contains(divisao)) {
                agir(() -> motor.mover(divisao));
            } else if (!divisao.equals(motor.getDivisaoAtual())) {
                lateral.registar(divisao.getName() + " não é vizinha de " + motor.getDivisaoAtual().getName() + ".");
            }
        }
    }

    /** Executa uma ação do jogador e põe na fila as animações do que aconteceu. */
    private void agir(Runnable acao) {
        if (cena.ocupada() || painel.isEscolherDificuldade()) {
            return;
        }
        try {
            acao.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            lateral.registar(e.getMessage());
        }
        for (String frase : motor.consumirRelato()) {
            lateral.registarDepois(frase);
        }
        cena.acrescentar(motor.consumirEventos());
        if (!cena.ocupada()) {
            lateral.mostrarPendentes();
        }
        painel.setPlano(null);
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        lateral.atualizar(motor, jogarSozinho, cena != null && cena.ocupada() || painel.isEscolherDificuldade());
    }

    /** Relógio da animação (cerca de 60 imagens por segundo). */
    private void tique() {
        long agora = System.nanoTime();
        double dt = ultimoTique == 0 ? 16 : Math.min(50, (agora - ultimoTique) / 1e6);
        ultimoTique = agora;
        if (cena == null) {
            return;
        }
        cena.avancar(dt);
        painel.avancarCamera(dt);
        boolean ocupada = cena.ocupada();
        if (estavaOcupada && !ocupada) {
            cena.sincronizar(motor);
            lateral.mostrarPendentes();
            atualizarBotoes();
        }
        estavaOcupada = ocupada;

        if (jogarSozinho && !ocupada) {
            parado += dt;
            if (motor.getEstado() == MotorJogo.Estado.TERMINADO || motor.getTurno() >= JogadorAutomatico.LIMITE_TURNOS) {
                jogarSozinho = false;
                atualizarBotoes();
            } else if (parado >= PAUSA_JOGO_SOZINHO_MS) {
                parado = 0;
                passoAutomatico();
            }
        }
        painel.repaint();
    }

    private void passoAutomatico() {
        if (motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA) {
            Plano plano = new PlaneadorMissao().planear(motor.getEdificio());
            agirSemBloqueio(() -> motor.entrar(plano.getEntrada()));
        } else {
            agirSemBloqueio(() -> jogador.jogarTurno(motor));
        }
    }

    private void agirSemBloqueio(Runnable acao) {
        boolean antes = jogarSozinho;
        agir(acao);
        jogarSozinho = antes;
    }

    private void alternarPlano() {
        if (painel.getPlano() != null) {
            painel.setPlano(null);
            return;
        }
        try {
            Plano plano = new PlaneadorMissao().planear(motor.getEdificio());
            painel.setPlano(plano);
            lateral.registar("Plano automático (dourado: ida, laranja: regresso), com os inimigos nas posições atuais:");
            for (String linha : plano.descrever().split("\n")) {
                lateral.registar(linha);
            }
        } catch (IllegalStateException e) {
            lateral.registar(e.getMessage());
        }
    }

    private void alternarJogoSozinho() {
        if (motor.getEstado() == MotorJogo.Estado.TERMINADO) {
            return;
        }
        jogarSozinho = !jogarSozinho;
        parado = PAUSA_JOGO_SOZINHO_MS;
        atualizarBotoes();
    }

    private void escolherFicheiro() {
        JFileChooser escolha = new JFileChooser(new File("."));
        escolha.setFileFilter(new FileNameExtensionFilter("Missões (JSON)", "json"));
        if (escolha.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                abrir(new ImportarMapa().gerarMapa(escolha.getSelectedFile().getPath()));
            } catch (IOException | RuntimeException e) {
                JOptionPane.showMessageDialog(this, "Não foi possível abrir a missão:\n" + e.getMessage(),
                        "Missão inválida", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ------------------------------------------------------------------ arranque

    /**
     * Abre a missão indicada, ou a de exemplo.
     *
     * @param args {@code [ficheiro.json]}, ou {@code --captura imagem.png largura altura turnos semente nivel ms}
     * @throws Exception se a missão não puder ser lida
     */
    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("--captura")) {
            capturar(args);
            return;
        }
        Missao missao = carregar(args.length > 0 ? args[0] : null);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // fica o aspeto por omissão
            }
            aplicarCoresDasDicas();
            JanelaMissao janela = new JanelaMissao(missao);
            janela.setSize(1440, 860);
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
        });
    }

    /** As dicas (tooltips) do mapa seguem as cores da interface. */
    private static void aplicarCoresDasDicas() {
        UIManager.put("ToolTip.background", Tema.CARTAO);
        UIManager.put("ToolTip.foreground", Tema.TEXTO);
        UIManager.put("ToolTip.font", Tema.texto(java.awt.Font.PLAIN, 13f));
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Tema.DIVISAO_BORDA),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    /** Lê a missão de um ficheiro ou, sem ficheiro, a missão de exemplo incluída no .jar. */
    static Missao carregar(String caminho) throws IOException {
        if (caminho != null) {
            return new ImportarMapa().gerarMapa(caminho);
        }
        try (InputStream exemplo = JanelaMissao.class.getResourceAsStream("/missao-exemplo.json")) {
            if (exemplo == null) {
                throw new IOException("A missão de exemplo não está incluída; indique um ficheiro JSON.");
            }
            Path temporario = Files.createTempFile("missao-exemplo", ".json");
            temporario.toFile().deleteOnExit();
            Files.copy(exemplo, temporario, StandardCopyOption.REPLACE_EXISTING);
            return new ImportarMapa().gerarMapa(temporario.toString());
        }
    }

    /**
     * Gera uma imagem da janela a meio de um jogo automático (usada no README): joga
     * {@code turnos} turnos sem animação, faz mais uma jogada e tira a fotografia {@code ms}
     * milissegundos depois do início da animação.
     */
    private static void capturar(String[] args) throws Exception {
        String destino = args[1];
        int largura = args.length > 3 ? Integer.parseInt(args[2]) : 1440;
        int altura = args.length > 3 ? Integer.parseInt(args[3]) : 860;
        int turnos = args.length > 4 ? Integer.parseInt(args[4]) : 6;
        long semente = args.length > 5 ? Long.parseLong(args[5]) : 4;
        Dificuldade nivel = args.length > 6 ? Dificuldade.valueOf(args[6]) : Dificuldade.AGENTE;
        double ms = args.length > 7 ? Double.parseDouble(args[7]) : 0;
        Missao missao = carregar(null);

        SwingUtilities.invokeAndWait(() -> {
            JanelaMissao janela = new JanelaMissao(missao);
            janela.relogio.stop();
            janela.aleatorio = new Random(semente);
            janela.novaSimulacao(nivel);
            janela.setUndecorated(true);
            janela.setSize(largura, altura);
            janela.setVisible(true);
            janela.validate();

            if (turnos < 0) {
                janela.painel.setEscolherDificuldade(true);
            }
            if (turnos > 0) {
                Plano plano = new PlaneadorMissao().planear(janela.motor.getEdificio());
                janela.motor.entrar(plano.getEntrada());
            }
            while (turnos > 0 && janela.motor.getTurno() < turnos && janela.motor.getEstado() != MotorJogo.Estado.TERMINADO) {
                janela.jogador.jogarTurno(janela.motor);
            }
            for (String frase : janela.motor.consumirRelato()) {
                janela.lateral.registar(frase);
            }
            janela.motor.consumirEventos();
            janela.cena.sincronizar(janela.motor);
            janela.cena.assentar();
            janela.cena.avancar(400);
            janela.cena.assentar();
            janela.atualizarBotoes();

            if (ms > 0 && turnos > 0 && janela.motor.getEstado() != MotorJogo.Estado.TERMINADO) {
                janela.agir(() -> janela.jogador.jogarTurno(janela.motor));
                for (double t = 0; t < ms; t += 16) {
                    janela.cena.avancar(16);
                }
            } else {
                janela.cena.avancar(800);
            }
            janela.getContentPane().validate();
            janela.painel.assentarCamera();

            BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = imagem.createGraphics();
            janela.getContentPane().paint(g);
            g.dispose();
            try {
                ImageIO.write(imagem, "png", new File(destino));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            janela.dispose();
        });
        System.exit(0);
    }
}
