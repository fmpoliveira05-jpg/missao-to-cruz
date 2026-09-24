package tocruz.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Cores e letras da interface (estilo "planta de arquitetura").
 *
 * @author Francisco Oliveira
 */
final class Tema {

    static final Color FUNDO = new Color(0x0A1A2F);
    static final Color GRELHA = new Color(0x0F2340);
    static final Color GRELHA_FORTE = new Color(0x132C4E);
    static final Color LIGACAO = new Color(0x2C567F);
    static final Color DIVISAO = new Color(0x0F2A47);
    static final Color DIVISAO_REALCE = new Color(0x173A60);
    static final Color DIVISAO_BORDA = new Color(0x4A77A3);
    static final Color TEXTO = new Color(0xE6EEF7);
    static final Color TEXTO_SUAVE = new Color(0x8FA8C4);
    static final Color PAINEL = new Color(0x0C1828);
    static final Color CARTAO = new Color(0x12243A);
    static final Color CARTAO_BORDA = new Color(0x1E3A5C);

    /** Tó Cruz e caminho sugerido. */
    static final Color CIANO = new Color(0x35D0FF);
    /** Saídas e kits de vida. */
    static final Color VERDE = new Color(0x3DDC97);
    /** Inimigos. */
    static final Color VERMELHO = new Color(0xFF5A5F);
    /** Alvo e plano automático. */
    static final Color DOURADO = new Color(0xF5C542);
    /** Regresso no plano automático. */
    static final Color LARANJA = new Color(0xFF9F43);
    /** Coletes. */
    static final Color AZUL = new Color(0x7AA7FF);

    private static final String[] LETRAS_TEXTO = {"Segoe UI", "Inter", "Helvetica Neue", "DejaVu Sans", "Liberation Sans"};
    private static final String[] LETRAS_TITULO = {"Segoe UI Semibold", "Poppins", "Montserrat", "Segoe UI", "DejaVu Sans"};

    private static final String FAMILIA_TEXTO = escolher(LETRAS_TEXTO, true);
    private static final String FAMILIA_TITULO = escolher(LETRAS_TITULO, false);

    private Tema() {
    }

    /**
     * Primeira família instalada da lista. O texto do jogo usa setas (→), por isso a letra do texto
     * tem de as saber desenhar.
     */
    private static String escolher(String[] familias, boolean precisaDeSetas) {
        String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String familia : familias) {
            for (String instalada : instaladas) {
                if (instalada.equalsIgnoreCase(familia)
                        && (!precisaDeSetas || new Font(familia, Font.PLAIN, 12).canDisplay('→'))) {
                    return familia;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    static Font texto(int estilo, float tamanho) {
        return new Font(FAMILIA_TEXTO, estilo, 12).deriveFont(tamanho);
    }

    static Font titulo(float tamanho) {
        int estilo = FAMILIA_TITULO.endsWith("Semibold") ? Font.PLAIN : Font.BOLD;
        return new Font(FAMILIA_TITULO, estilo, 12).deriveFont(tamanho);
    }

    static Color comAlfa(Color cor, int alfa) {
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alfa);
    }
}
