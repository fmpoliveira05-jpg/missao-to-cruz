package tocruz.gui;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;

/**
 * Geometria da planta: onde fica cada sala, por onde passam os corredores e onde ficam as portas
 * para o exterior.
 *
 * <p>As salas ocupam as células calculadas por {@link DisposicaoGrafo}. Cada ligação do grafo
 * vira um corredor: reto quando as salas estão na mesma linha ou coluna, em "L" nos outros casos
 * (escolhe-se o "L" que não passa por cima de outras salas). Quando vários corredores saem do
 * mesmo lado de uma sala, as portas são distribuídas ao longo da parede.</p>
 *
 * @author Francisco Oliveira
 */
final class Planta {

    static final double SALA_W = 216;
    static final double SALA_H = 152;
    static final double FOLGA_X = 78;
    static final double FOLGA_Y = 70;
    static final double PAREDE = 7;
    static final double CORREDOR = 34;
    static final double MARGEM = 70;
    private static final double PROPORCAO = 1.45;

    /** Lados de uma sala. */
    static final int ESQUERDA = 0;
    static final int DIREITA = 1;
    static final int CIMA = 2;
    static final int BAIXO = 3;

    final String[] nomes;
    final boolean[][] ligado;
    final boolean[] saida;
    final Rectangle2D[] salas;
    final double largura;
    final double altura;

    /** Corredor de cada ligação: pontos x0,y0,x1,y1,... da porta de a até à porta de b. */
    private final double[][][] corredor;
    /** Porta exterior de cada saída: porta (x,y), ponto lá fora (x,y) e lado. */
    final double[][] portaExterior;
    /** Número de ligações entre cada par de salas (pesquisa em largura a partir de cada uma). */
    final int[][] distancia;

    private Planta(String[] nomes, boolean[][] ligado, boolean[] saida, DisposicaoGrafo d) {
        int n = nomes.length;
        this.nomes = nomes;
        this.ligado = ligado;
        this.saida = saida;
        this.salas = new Rectangle2D[n];
        this.corredor = new double[n][n][];
        this.portaExterior = new double[n][];
        for (int i = 0; i < n; i++) {
            salas[i] = new Rectangle2D.Double(MARGEM + d.x[i] - SALA_W / 2, MARGEM + d.y[i] - SALA_H / 2, SALA_W, SALA_H);
        }
        this.largura = d.largura + 2 * MARGEM;
        this.altura = d.altura + 2 * MARGEM;
        tracarCorredores();
        tracarSaidas();
        this.distancia = calcularDistancias();
    }

    private int[][] calcularDistancias() {
        int n = nomes.length;
        int[][] d = new int[n][n];
        for (int origem = 0; origem < n; origem++) {
            for (int j = 0; j < n; j++) {
                d[origem][j] = Integer.MAX_VALUE;
            }
            int[] fila = new int[n];
            int inicio = 0;
            int fim = 0;
            d[origem][origem] = 0;
            fila[fim++] = origem;
            while (inicio < fim) {
                int atual = fila[inicio++];
                for (int j = 0; j < n; j++) {
                    if (ligado[atual][j] && d[origem][j] == Integer.MAX_VALUE) {
                        d[origem][j] = d[origem][atual] + 1;
                        fila[fim++] = j;
                    }
                }
            }
        }
        return d;
    }

    /** Lê as divisões e ligações do edifício e calcula a planta. */
    static Planta criar(Edificio edificio) {
        int n = 0;
        Iterator<Divisao> it = edificio.IteratorMapa();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        String[] nomes = new String[n];
        boolean[] saida = new boolean[n];
        it = edificio.IteratorMapa();
        for (int i = 0; i < n; i++) {
            Divisao divisao = it.next();
            nomes[i] = divisao.getName();
            saida[i] = divisao.isEntrada_saida();
        }
        boolean[][] ligado = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            Iterator<Divisao> vizinhos = edificio.getNextDivisoes(edificio.getDivisao(nomes[i]));
            while (vizinhos.hasNext()) {
                String vizinho = vizinhos.next().getName();
                for (int j = 0; j < n; j++) {
                    if (nomes[j].equals(vizinho)) {
                        ligado[i][j] = true;
                        ligado[j][i] = true;
                    }
                }
            }
        }
        double[] larguras = new double[n];
        double[] alturas = new double[n];
        for (int i = 0; i < n; i++) {
            larguras[i] = SALA_W;
            alturas[i] = SALA_H;
        }
        DisposicaoGrafo d = DisposicaoGrafo.calcular(ligado, larguras, alturas, PROPORCAO, FOLGA_X, FOLGA_Y);
        return new Planta(nomes, ligado, saida, d);
    }

    int indice(String nome) {
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    Point2D centro(int i) {
        return new Point2D.Double(salas[i].getCenterX(), salas[i].getCenterY());
    }

    /** @return pontos do corredor de a até b (da porta de a à porta de b), ou {@code null} */
    double[] corredor(int a, int b) {
        return corredor[a][b];
    }

    /** @return todos os corredores, um por ligação */
    double[][] corredores() {
        int total = 0;
        for (int a = 0; a < nomes.length; a++) {
            for (int b = a + 1; b < nomes.length; b++) {
                if (corredor[a][b] != null) {
                    total++;
                }
            }
        }
        double[][] todos = new double[total][];
        int k = 0;
        for (int a = 0; a < nomes.length; a++) {
            for (int b = a + 1; b < nomes.length; b++) {
                if (corredor[a][b] != null) {
                    todos[k++] = corredor[a][b];
                }
            }
        }
        return todos;
    }

    /** @return sala que contém o ponto, ou -1 */
    int salaEm(double x, double y) {
        for (int i = 0; i < salas.length; i++) {
            if (salas[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ corredores

    /** Formas possíveis de um corredor. */
    private static final int Z_HORIZONTAL_PERTO_A = 0;
    private static final int Z_HORIZONTAL_PERTO_B = 1;
    private static final int Z_VERTICAL_PERTO_A = 2;
    private static final int Z_VERTICAL_PERTO_B = 3;
    private static final int L_SAI_NA_HORIZONTAL = 4;
    private static final int L_SAI_NA_VERTICAL = 5;

    private void tracarCorredores() {
        int n = nomes.length;
        // lado[a][b]: lado da sala a por onde sai o corredor para b; forma[a][b]: tipo de corredor.
        int[][] lado = new int[n][n];
        int[][] forma = new int[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (!ligado[a][b]) {
                    continue;
                }
                double dx = salas[b].getCenterX() - salas[a].getCenterX();
                double dy = salas[b].getCenterY() - salas[a].getCenterY();
                int[] candidatas;
                if (Math.abs(dy) < 1) {
                    candidatas = new int[]{Z_HORIZONTAL_PERTO_A};
                } else if (Math.abs(dx) < 1) {
                    candidatas = new int[]{Z_VERTICAL_PERTO_A};
                } else {
                    // Primeiro os corredores que passam pelos espaços entre salas (em "Z"),
                    // depois os que cortam uma célula (em "L").
                    candidatas = new int[]{Z_HORIZONTAL_PERTO_A, Z_HORIZONTAL_PERTO_B, Z_VERTICAL_PERTO_A,
                            Z_VERTICAL_PERTO_B, L_SAI_NA_HORIZONTAL, L_SAI_NA_VERTICAL};
                }
                int escolhida = candidatas[0];
                int menos = Integer.MAX_VALUE;
                for (int c : candidatas) {
                    int colisoes = colisoes(construir(a, b, c, 0.5, 0.5), a, b);
                    if (colisoes < menos) {
                        menos = colisoes;
                        escolhida = c;
                    }
                }
                forma[a][b] = escolhida;
                lado[a][b] = ladoA(a, b, escolhida);
                lado[b][a] = ladoB(a, b, escolhida);
            }
        }

        // Posição de cada porta ao longo da parede (fração entre 0 e 1), para os corredores do
        // mesmo lado não se sobreporem; ordena-se pela posição da sala do outro lado.
        double[][] posicao = new double[n][n];
        for (int a = 0; a < n; a++) {
            for (int s = 0; s < 4; s++) {
                int[] outros = new int[n];
                int total = 0;
                for (int b = 0; b < n; b++) {
                    if (ligado[a][b] && lado[a][b] == s) {
                        outros[total++] = b;
                    }
                }
                for (int i = 1; i < total; i++) {
                    for (int j = i; j > 0 && chaveOrdem(outros[j], s) < chaveOrdem(outros[j - 1], s); j--) {
                        int t = outros[j];
                        outros[j] = outros[j - 1];
                        outros[j - 1] = t;
                    }
                }
                for (int i = 0; i < total; i++) {
                    posicao[a][outros[i]] = total == 1 ? 0.5 : (i + 1.0) / (total + 1.0);
                }
            }
        }

        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (ligado[a][b]) {
                    double[] pontos = construir(a, b, forma[a][b], posicao[a][b], posicao[b][a]);
                    corredor[a][b] = pontos;
                    corredor[b][a] = inverter(pontos);
                }
            }
        }
    }

    private int ladoA(int a, int b, int forma) {
        double dx = salas[b].getCenterX() - salas[a].getCenterX();
        double dy = salas[b].getCenterY() - salas[a].getCenterY();
        boolean horizontal = forma == Z_HORIZONTAL_PERTO_A || forma == Z_HORIZONTAL_PERTO_B || forma == L_SAI_NA_HORIZONTAL;
        if (horizontal) {
            return dx > 0 ? DIREITA : ESQUERDA;
        }
        return dy > 0 ? BAIXO : CIMA;
    }

    private int ladoB(int a, int b, int forma) {
        double dx = salas[b].getCenterX() - salas[a].getCenterX();
        double dy = salas[b].getCenterY() - salas[a].getCenterY();
        boolean horizontal = forma == Z_HORIZONTAL_PERTO_A || forma == Z_HORIZONTAL_PERTO_B || forma == L_SAI_NA_VERTICAL;
        if (horizontal) {
            return dx > 0 ? ESQUERDA : DIREITA;
        }
        return dy > 0 ? CIMA : BAIXO;
    }

    /** Pontos de um corredor de a até b com uma dada forma e posição das portas. */
    private double[] construir(int a, int b, int forma, double fracA, double fracB) {
        int la = ladoA(a, b, forma);
        int lb = ladoB(a, b, forma);
        Point2D pa = porta(a, la, fracA);
        Point2D pb = porta(b, lb, fracB);
        switch (forma) {
            case Z_HORIZONTAL_PERTO_A:
            case Z_HORIZONTAL_PERTO_B: {
                if (Math.abs(pa.getY() - pb.getY()) < 0.5) {
                    return new double[]{pa.getX(), pa.getY(), pb.getX(), pb.getY()};
                }
                double faixa = forma == Z_HORIZONTAL_PERTO_A
                        ? (la == DIREITA ? salas[a].getMaxX() + FOLGA_X / 2 : salas[a].getMinX() - FOLGA_X / 2)
                        : (lb == ESQUERDA ? salas[b].getMinX() - FOLGA_X / 2 : salas[b].getMaxX() + FOLGA_X / 2);
                return new double[]{pa.getX(), pa.getY(), faixa, pa.getY(), faixa, pb.getY(), pb.getX(), pb.getY()};
            }
            case Z_VERTICAL_PERTO_A:
            case Z_VERTICAL_PERTO_B: {
                if (Math.abs(pa.getX() - pb.getX()) < 0.5) {
                    return new double[]{pa.getX(), pa.getY(), pb.getX(), pb.getY()};
                }
                double faixa = forma == Z_VERTICAL_PERTO_A
                        ? (la == BAIXO ? salas[a].getMaxY() + FOLGA_Y / 2 : salas[a].getMinY() - FOLGA_Y / 2)
                        : (lb == CIMA ? salas[b].getMinY() - FOLGA_Y / 2 : salas[b].getMaxY() + FOLGA_Y / 2);
                return new double[]{pa.getX(), pa.getY(), pa.getX(), faixa, pb.getX(), faixa, pb.getX(), pb.getY()};
            }
            default: {
                boolean horizontalA = forma == L_SAI_NA_HORIZONTAL;
                double cx = horizontalA ? pb.getX() : pa.getX();
                double cy = horizontalA ? pa.getY() : pb.getY();
                return new double[]{pa.getX(), pa.getY(), cx, cy, pb.getX(), pb.getY()};
            }
        }
    }

    private double chaveOrdem(int outra, int lado) {
        return lado == ESQUERDA || lado == DIREITA ? salas[outra].getCenterY() : salas[outra].getCenterX();
    }

    /** Ponto no meio da parede (dentro da espessura) onde fica a porta. */
    private Point2D porta(int i, int lado, double fracao) {
        Rectangle2D r = salas[i];
        double margemPorta = CORREDOR * 0.9;
        switch (lado) {
            case ESQUERDA:
                return new Point2D.Double(r.getMinX() + PAREDE, r.getMinY() + margemPorta + fracao * (r.getHeight() - 2 * margemPorta));
            case DIREITA:
                return new Point2D.Double(r.getMaxX() - PAREDE, r.getMinY() + margemPorta + fracao * (r.getHeight() - 2 * margemPorta));
            case CIMA:
                return new Point2D.Double(r.getMinX() + margemPorta + fracao * (r.getWidth() - 2 * margemPorta), r.getMinY() + PAREDE);
            default:
                return new Point2D.Double(r.getMinX() + margemPorta + fracao * (r.getWidth() - 2 * margemPorta), r.getMaxY() - PAREDE);
        }
    }

    private int colisoes(double[] pontos, int a, int b) {
        int total = 0;
        for (int k = 0; k + 3 < pontos.length; k += 2) {
            Line2D segmento = new Line2D.Double(pontos[k], pontos[k + 1], pontos[k + 2], pontos[k + 3]);
            for (int i = 0; i < salas.length; i++) {
                if (i == a || i == b) {
                    continue;
                }
                Rectangle2D r = salas[i];
                Rectangle2D maior = new Rectangle2D.Double(r.getX() - CORREDOR / 2, r.getY() - CORREDOR / 2,
                        r.getWidth() + CORREDOR, r.getHeight() + CORREDOR);
                if (segmento.intersects(maior)) {
                    total++;
                }
            }
        }
        return total;
    }

    private static double[] inverter(double[] pontos) {
        double[] invertidos = new double[pontos.length];
        for (int k = 0; k < pontos.length; k += 2) {
            invertidos[pontos.length - 2 - k] = pontos[k];
            invertidos[pontos.length - 1 - k] = pontos[k + 1];
        }
        return invertidos;
    }

    // ------------------------------------------------------------------ saídas

    /** Cada saída ganha uma porta para o exterior no lado livre mais perto do limite do edifício. */
    private void tracarSaidas() {
        int n = nomes.length;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Rectangle2D r : salas) {
            minX = Math.min(minX, r.getMinX());
            maxX = Math.max(maxX, r.getMaxX());
            minY = Math.min(minY, r.getMinY());
            maxY = Math.max(maxY, r.getMaxY());
        }
        for (int i = 0; i < n; i++) {
            if (!saida[i]) {
                continue;
            }
            boolean[] ocupado = new boolean[4];
            for (int b = 0; b < n; b++) {
                if (corredor[i][b] != null) {
                    ocupado[ladoDoPonto(i, corredor[i][b][0], corredor[i][b][1])] = true;
                }
            }
            Rectangle2D r = salas[i];
            double[] distancia = {r.getMinX() - minX, maxX - r.getMaxX(), r.getMinY() - minY, maxY - r.getMaxY()};
            int melhor = -1;
            for (int s = 0; s < 4; s++) {
                if (!ocupado[s] && (melhor < 0 || distancia[s] < distancia[melhor])) {
                    melhor = s;
                }
            }
            double fracao = 0.5;
            if (melhor < 0) {
                melhor = distancia[0] <= distancia[1] ? ESQUERDA : DIREITA;
                fracao = 0.15;
            }
            Point2D p = porta(i, melhor, fracao);
            double fora = 58;
            double ox = p.getX();
            double oy = p.getY();
            switch (melhor) {
                case ESQUERDA:
                    ox -= fora;
                    break;
                case DIREITA:
                    ox += fora;
                    break;
                case CIMA:
                    oy -= fora;
                    break;
                default:
                    oy += fora;
                    break;
            }
            portaExterior[i] = new double[]{p.getX(), p.getY(), ox, oy, melhor};
        }
    }

    private int ladoDoPonto(int i, double x, double y) {
        Rectangle2D r = salas[i];
        double[] d = {Math.abs(x - r.getMinX()), Math.abs(x - r.getMaxX()), Math.abs(y - r.getMinY()), Math.abs(y - r.getMaxY())};
        int melhor = 0;
        for (int s = 1; s < 4; s++) {
            if (d[s] < d[melhor]) {
                melhor = s;
            }
        }
        return melhor;
    }
}
