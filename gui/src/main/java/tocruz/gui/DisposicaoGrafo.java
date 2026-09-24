package tocruz.gui;

import java.util.Random;

/**
 * Calcula onde desenhar cada divisão do edifício.
 *
 * <p>O ficheiro da missão só diz que divisões estão ligadas, não onde ficam. Para o desenho
 * parecer a planta de um edifício, as divisões são colocadas numa grelha (uma divisão por célula,
 * por isso nunca se sobrepõem) e a posição de cada uma é escolhida por <i>simulated annealing</i>:
 * trocam-se divisões de célula ao acaso e aceita-se a troca se o desenho melhorar (ou, no início,
 * mesmo que piore um pouco, para não ficar preso num mínimo local). O desenho é pior quanto mais
 * compridas forem as ligações, quantas mais se cruzarem e quantas passarem por cima de outra
 * divisão. As sementes são fixas, por isso a mesma missão é sempre desenhada da mesma forma.</p>
 *
 * @author Francisco Oliveira
 */
final class DisposicaoGrafo {

    private static final int TENTATIVAS = 4;
    private static final int ITERACOES = 40_000;
    private static final double PESO_CRUZAMENTO = 6;
    private static final double PESO_POR_CIMA = 10;

    /** Centro de cada caixa. */
    final double[] x;
    final double[] y;
    /** Tamanho total do desenho. */
    double largura;
    double altura;

    private final boolean[][] ligado;
    private final double[] larguras;
    private final double[] alturas;
    private final int colunas;
    private final int linhas;
    private final double celulaW;
    private final double celulaH;
    private final int[] celula;
    private final int[] ocupante;

    private DisposicaoGrafo(boolean[][] ligado, double[] larguras, double[] alturas, int colunas, int linhas,
                            double celulaW, double celulaH) {
        int n = ligado.length;
        this.ligado = ligado;
        this.larguras = larguras;
        this.alturas = alturas;
        this.colunas = colunas;
        this.linhas = linhas;
        this.celulaW = celulaW;
        this.celulaH = celulaH;
        this.x = new double[n];
        this.y = new double[n];
        this.celula = new int[n];
        this.ocupante = new int[colunas * linhas];
    }

    /**
     * @param ligado matriz de adjacência (simétrica)
     * @param larguras largura da caixa de cada divisão
     * @param alturas altura da caixa de cada divisão
     * @param proporcao largura / altura desejada para o desenho
     * @return a melhor disposição encontrada
     */
    static DisposicaoGrafo calcular(boolean[][] ligado, double[] larguras, double[] alturas, double proporcao) {
        int n = ligado.length;
        double maiorW = 0;
        double maiorH = 0;
        for (int i = 0; i < n; i++) {
            maiorW = Math.max(maiorW, larguras[i]);
            maiorH = Math.max(maiorH, alturas[i]);
        }
        double celulaW = maiorW + 34;
        double celulaH = maiorH + 56;

        // Grelha com folga (cerca de 1,6 células por divisão) e com a proporção pedida.
        int melhorColunas = 1;
        int melhorLinhas = Math.max(1, n);
        double melhorDiferenca = Double.MAX_VALUE;
        for (int colunas = 1; colunas <= Math.max(1, n); colunas++) {
            int linhas = Math.max(1, (int) Math.ceil(n * 1.6 / colunas));
            double diferenca = Math.abs(Math.log(colunas * celulaW / (linhas * celulaH) / proporcao));
            if (diferenca < melhorDiferenca) {
                melhorDiferenca = diferenca;
                melhorColunas = colunas;
                melhorLinhas = linhas;
            }
        }

        DisposicaoGrafo melhor = null;
        double melhorCusto = Double.MAX_VALUE;
        for (int semente = 0; semente < TENTATIVAS; semente++) {
            DisposicaoGrafo tentativa = new DisposicaoGrafo(ligado, larguras, alturas, melhorColunas, melhorLinhas, celulaW, celulaH);
            double custo = tentativa.recozer(new Random(semente));
            if (custo < melhorCusto) {
                melhor = tentativa;
                melhorCusto = custo;
            }
        }
        melhor.compactar();
        return melhor;
    }

    private double recozer(Random aleatorio) {
        int n = x.length;
        int celulas = ocupante.length;
        for (int k = 0; k < celulas; k++) {
            ocupante[k] = -1;
        }
        // Posição inicial: ordem aleatória das células.
        int[] ordem = new int[celulas];
        for (int k = 0; k < celulas; k++) {
            ordem[k] = k;
        }
        for (int k = celulas - 1; k > 0; k--) {
            int j = aleatorio.nextInt(k + 1);
            int t = ordem[k];
            ordem[k] = ordem[j];
            ordem[j] = t;
        }
        for (int i = 0; i < n; i++) {
            colocar(i, ordem[i]);
        }
        if (n < 2) {
            return custo();
        }

        double atual = custo();
        double melhorCusto = atual;
        int[] melhorCelula = celula.clone();
        double temperatura = 4;
        double arrefecimento = Math.pow(0.002 / temperatura, 1.0 / ITERACOES);

        for (int it = 0; it < ITERACOES; it++) {
            int a = aleatorio.nextInt(n);
            int destino = aleatorio.nextInt(celulas);
            int origem = celula[a];
            if (destino == origem) {
                continue;
            }
            int b = ocupante[destino];
            colocar(a, destino);
            if (b >= 0) {
                colocar(b, origem);
            } else {
                ocupante[origem] = -1;
            }

            double novo = custo();
            double delta = novo - atual;
            if (delta <= 0 || aleatorio.nextDouble() < Math.exp(-delta / temperatura)) {
                atual = novo;
                if (atual < melhorCusto) {
                    melhorCusto = atual;
                    melhorCelula = celula.clone();
                }
            } else {
                colocar(a, origem);
                if (b >= 0) {
                    colocar(b, destino);
                } else {
                    ocupante[destino] = -1;
                }
            }
            temperatura *= arrefecimento;
        }

        for (int k = 0; k < celulas; k++) {
            ocupante[k] = -1;
        }
        for (int i = 0; i < n; i++) {
            colocar(i, melhorCelula[i]);
        }
        return melhorCusto;
    }

    private void colocar(int i, int k) {
        celula[i] = k;
        ocupante[k] = i;
        x[i] = (k % colunas + 0.5) * celulaW;
        y[i] = (k / colunas + 0.5) * celulaH;
    }

    private double custo() {
        double comprimento = 0;
        for (int a = 0; a < x.length; a++) {
            for (int b = a + 1; b < x.length; b++) {
                if (ligado[a][b]) {
                    double dc = (x[a] - x[b]) / celulaW;
                    double dl = (y[a] - y[b]) / celulaH;
                    comprimento += dc * dc + dl * dl;
                }
            }
        }
        return comprimento + PESO_CRUZAMENTO * cruzamentos() + PESO_POR_CIMA * ligacoesPorCimaDeCaixas();
    }

    /** Remove linhas e colunas vazias da grelha e calcula o tamanho final do desenho. */
    private void compactar() {
        boolean[] colunaUsada = new boolean[colunas];
        boolean[] linhaUsada = new boolean[linhas];
        for (int i = 0; i < x.length; i++) {
            colunaUsada[celula[i] % colunas] = true;
            linhaUsada[celula[i] / colunas] = true;
        }
        int[] novaColuna = new int[colunas];
        int usadas = 0;
        for (int c = 0; c < colunas; c++) {
            novaColuna[c] = usadas;
            if (colunaUsada[c]) {
                usadas++;
            }
        }
        int[] novaLinha = new int[linhas];
        int usadasL = 0;
        for (int l = 0; l < linhas; l++) {
            novaLinha[l] = usadasL;
            if (linhaUsada[l]) {
                usadasL++;
            }
        }
        for (int i = 0; i < x.length; i++) {
            x[i] = (novaColuna[celula[i] % colunas] + 0.5) * celulaW;
            y[i] = (novaLinha[celula[i] / colunas] + 0.5) * celulaH;
        }
        largura = Math.max(1, usadas) * celulaW;
        altura = Math.max(1, usadasL) * celulaH;
    }

    /** @return número de pares de ligações que se cruzam (sem contar as que partilham uma divisão) */
    int cruzamentos() {
        int n = x.length;
        int total = 0;
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (!ligado[a][b]) {
                    continue;
                }
                for (int c = a + 1; c < n; c++) {
                    for (int d = c + 1; d < n; d++) {
                        boolean partilham = c == b || d == b;
                        if (ligado[c][d] && !partilham && cruzam(a, b, c, d)) {
                            total++;
                        }
                    }
                }
            }
        }
        return total;
    }

    /** @return número de vezes que uma ligação passa por cima da caixa de uma terceira divisão */
    int ligacoesPorCimaDeCaixas() {
        int n = x.length;
        int total = 0;
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (!ligado[a][b]) {
                    continue;
                }
                for (int c = 0; c < n; c++) {
                    if (c != a && c != b && segmentoTocaCaixa(a, b, x[c], y[c], larguras[c] / 2 + 6, alturas[c] / 2 + 6)) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /** @return se as caixas de duas divisões se sobrepõem */
    boolean sobrepoem(int i, int j) {
        return Math.abs(x[i] - x[j]) < (larguras[i] + larguras[j]) / 2
                && Math.abs(y[i] - y[j]) < (alturas[i] + alturas[j]) / 2;
    }

    /** Recorte de Liang–Barsky: o segmento a-b entra no retângulo centrado em (cx, cy)? */
    private boolean segmentoTocaCaixa(int a, int b, double cx, double cy, double meiaW, double meiaH) {
        double x0 = x[a];
        double y0 = y[a];
        double ddx = x[b] - x0;
        double ddy = y[b] - y0;
        double[] p = {-ddx, ddx, -ddy, ddy};
        double[] q = {x0 - (cx - meiaW), (cx + meiaW) - x0, y0 - (cy - meiaH), (cy + meiaH) - y0};
        double t0 = 0;
        double t1 = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return false;
                }
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) {
                    t0 = Math.max(t0, t);
                } else {
                    t1 = Math.min(t1, t);
                }
            }
        }
        return t0 <= t1;
    }

    private boolean cruzam(int a, int b, int c, int d) {
        double d1 = orientacao(x[c], y[c], x[d], y[d], x[a], y[a]);
        double d2 = orientacao(x[c], y[c], x[d], y[d], x[b], y[b]);
        double d3 = orientacao(x[a], y[a], x[b], y[b], x[c], y[c]);
        double d4 = orientacao(x[a], y[a], x[b], y[b], x[d], y[d]);
        return d1 * d2 < 0 && d3 * d4 < 0;
    }

    private static double orientacao(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }
}
