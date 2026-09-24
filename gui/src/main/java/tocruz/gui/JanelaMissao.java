package tocruz.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import tocruz.importar.ImportarMapa;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.missoes.Missao;
import tocruz.motor.JogadorAutomatico;
import tocruz.motor.MotorJogo;
import tocruz.motor.PlaneadorMissao;
import tocruz.motor.Plano;

/**
 * Interface gráfica do simulador.
 *
 * <p>Mostra o edifício como uma planta e deixa jogar o modo manual com o rato: clicar numa
 * entrada para entrar, clicar numa divisão vizinha para avançar e usar os botões para atacar,
 * usar kits ou sair. Também mostra o plano do modo automático e pode jogar sozinha, turno a turno.
 * Todas as regras vêm de {@link MotorJogo}, o mesmo motor usado pela aplicação de consola.</p>
 *
 * <p>Uso: {@code JanelaMissao [ficheiro.json]}. Sem ficheiro, abre a missão de exemplo.</p>
 *
 * @author Francisco Oliveira
 */
public class JanelaMissao extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int PAUSA_JOGO_SOZINHO_MS = 550;

    private final PainelMapa mapa = new PainelMapa();
    private final PainelLateral lateral = new PainelLateral();
    private final transient JogadorAutomatico jogador = new JogadorAutomatico();
    private final Timer relogio;

    private transient Missao missao;
    private transient MotorJogo motor;
    private transient Random aleatorio = new Random();

    /**
     * @param missao missão a abrir
     */
    public JanelaMissao(Missao missao) {
        super("Missão Tó Cruz");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FUNDO);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mapa, BorderLayout.CENTER);
        getContentPane().add(lateral, BorderLayout.EAST);
        setMinimumSize(new Dimension(1100, 680));

        relogio = new Timer(PAUSA_JOGO_SOZINHO_MS, e -> passoAutomatico());

        mapa.setOuvinte(this::clicouEm);
        lateral.atacar.addActionListener(e -> agir(() -> motor.atacar()));
        lateral.usarKit.addActionListener(e -> agir(() -> motor.usarKit()));
        lateral.ficar.addActionListener(e -> agir(() -> motor.aguardar()));
        lateral.sair.addActionListener(e -> agir(() -> motor.sair()));
        lateral.kitUsar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.USAR)));
        lateral.kitGuardar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.GUARDAR)));
        lateral.kitDeixar.addActionListener(e -> agir(() -> motor.decidirKit(MotorJogo.DecisaoKit.DEIXAR)));
        lateral.novaSimulacao.addActionListener(e -> novaSimulacao());
        lateral.planoAutomatico.addActionListener(e -> alternarPlano());
        lateral.jogarSozinho.addActionListener(e -> alternarJogoSozinho());
        lateral.abrirMissao.addActionListener(e -> escolherFicheiro());
        lateral.verSugestao.addActionListener(e -> mapa.setMostrarSugestao(lateral.verSugestao.isSelected()));
        lateral.verKit.addActionListener(e -> mapa.setMostrarKit(lateral.verKit.isSelected()));

        abrir(missao);
    }

    // ------------------------------------------------------------------ missão e simulações

    private void abrir(Missao novaMissao) {
        this.missao = novaMissao;
        lateral.setMissao(novaMissao.getcod_missao(), novaMissao.getVersao());
        mapa.setEdificio(novaMissao.getEdificio());
        novaSimulacao();
    }

    /** Começa uma simulação numa cópia do edifício (inimigos e itens na posição original). */
    private void novaSimulacao() {
        relogio.stop();
        Edificio base = missao.getEdificio();
        motor = new MotorJogo(new Edificio(base.getId(), base.getName(), base.getPlantaEdificio()), aleatorio);
        mapa.setMotor(motor);
        lateral.limparRegisto();
        lateral.registar("Nova simulação. Clique numa das entradas (contorno verde) para o Tó Cruz entrar.");
        atualizar();
    }

    private void clicouEm(Divisao divisao) {
        if (relogio.isRunning()) {
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
                lateral.registar("Em combate não pode sair da divisão: ataque ou use um kit.");
            } else if (motor.getDestinosPossiveis().contains(divisao)) {
                agir(() -> motor.mover(divisao));
            } else if (!divisao.equals(motor.getDivisaoAtual())) {
                lateral.registar(divisao.getName() + " não é vizinha de " + motor.getDivisaoAtual().getName() + ".");
            }
        }
    }

    /** Executa uma ação do jogador e mostra o que aconteceu. */
    private void agir(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            lateral.registar(e.getMessage());
        }
        for (String frase : motor.consumirRelato()) {
            lateral.registar(frase);
        }
        atualizar();
    }

    private void atualizar() {
        lateral.atualizar(motor, relogio.isRunning());
        mapa.repaint();
    }

    private void alternarPlano() {
        if (mapa.getPlano() != null) {
            mapa.setPlano(null);
            return;
        }
        try {
            Plano plano = new PlaneadorMissao().planear(motor.getEdificio());
            mapa.setPlano(plano);
            lateral.registar("Plano automático (dourado: ida, laranja: regresso), com os inimigos nas posições atuais:");
            for (String linha : plano.descrever().split("\n")) {
                lateral.registar(linha);
            }
        } catch (IllegalStateException e) {
            lateral.registar(e.getMessage());
        }
    }

    private void alternarJogoSozinho() {
        if (relogio.isRunning()) {
            relogio.stop();
        } else if (motor.getEstado() != MotorJogo.Estado.TERMINADO) {
            relogio.start();
        }
        atualizar();
    }

    private void passoAutomatico() {
        if (motor.getEstado() == MotorJogo.Estado.TERMINADO || motor.getTurno() >= JogadorAutomatico.LIMITE_TURNOS) {
            relogio.stop();
            atualizar();
            return;
        }
        if (motor.getEstado() == MotorJogo.Estado.ESCOLHER_ENTRADA) {
            Plano plano = new PlaneadorMissao().planear(motor.getEdificio());
            agir(() -> motor.entrar(plano.getEntrada()));
        } else {
            agir(() -> jogador.jogarTurno(motor));
        }
        if (motor.getEstado() == MotorJogo.Estado.TERMINADO) {
            relogio.stop();
            atualizar();
        }
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
     * @param args {@code [ficheiro.json]} ou {@code --captura imagem.png [largura altura [turnos [semente]]]}
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
            janela.setSize(1360, 820);
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
     * Gera uma imagem da janela a meio de um jogo automático (usada no README).
     */
    private static void capturar(String[] args) throws Exception {
        String destino = args[1];
        int largura = args.length > 3 ? Integer.parseInt(args[2]) : 1440;
        int altura = args.length > 3 ? Integer.parseInt(args[3]) : 860;
        int turnos = args.length > 4 ? Integer.parseInt(args[4]) : 6;
        long semente = args.length > 5 ? Long.parseLong(args[5]) : 4;
        Missao missao = carregar(args.length > 6 ? args[6] : null);

        SwingUtilities.invokeAndWait(() -> {
            JanelaMissao janela = new JanelaMissao(missao);
            janela.aleatorio = new Random(semente);
            janela.novaSimulacao();
            janela.setUndecorated(true);
            janela.setSize(largura, altura);
            janela.setVisible(true);
            janela.validate();

            Plano plano = new PlaneadorMissao().planear(janela.motor.getEdificio());
            janela.agir(() -> janela.motor.entrar(plano.getEntrada()));
            while (janela.motor.getTurno() < turnos && janela.motor.getEstado() != MotorJogo.Estado.TERMINADO) {
                janela.agir(() -> janela.jogador.jogarTurno(janela.motor));
            }
            janela.atualizar();
            janela.getContentPane().validate();

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
