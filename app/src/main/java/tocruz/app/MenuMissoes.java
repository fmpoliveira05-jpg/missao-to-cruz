package tocruz.app;

import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import tocruz.exportar.ExportarDado;
import tocruz.importar.ImportarMapa;
import tocruz.missoes.Missao;
import tocruz.missoes.Missoes;
import tocruz.util.Consola;

/**
 * Ponto de entrada do simulador de missões do Tó Cruz.
 *
 * <p>Uso: {@code java -jar missao-to-cruz.jar [pasta-das-missoes]}. Por omissão as missões são
 * procuradas na pasta {@code missoes/} e os trajetos exportados vão para {@code exportacoes/}.</p>
 *
 * @author Artur Pinto
 * @author Francisco Oliveira
 * @version 2.0
 */
public final class MenuMissoes {

    private final Missoes missoes = new Missoes();
    private final Path pastaMissoes;
    private final Path pastaExportacoes;

    private MenuMissoes(Path pastaMissoes, Path pastaExportacoes) {
        this.pastaMissoes = pastaMissoes;
        this.pastaExportacoes = pastaExportacoes;
    }

    /**
     * @param args opcionalmente, a pasta com os ficheiros JSON das missões
     */
    public static void main(String[] args) {
        Path pasta = Path.of(args.length > 0 ? args[0] : "missoes");
        try {
            new MenuMissoes(pasta, Path.of("exportacoes")).executar();
        } catch (NoSuchElementException fimDaEntrada) {
            System.out.println();
            System.out.println("Entrada terminada. Missão abortada!");
        }
    }

    private void executar() {
        int opcao;
        do {
            System.out.println();
            System.out.println("=== Simulador de missões do Tó Cruz ===");
            System.out.println("Missões carregadas: " + this.missoes.getContMissoes());
            System.out.println("1 - Importar missão (ficheiro JSON)");
            System.out.println("2 - Jogar uma missão");
            System.out.println("3 - Ver o mapa de uma missão");
            System.out.println("4 - Ver resultados das simulações manuais");
            System.out.println("5 - Exportar trajetos das simulações");
            System.out.println("0 - Sair");
            opcao = Consola.lerInteiro("Opção: ", 0, 5);
            switch (opcao) {
                case 1:
                    importar();
                    break;
                case 2:
                    jogar();
                    break;
                case 3:
                    verMapa();
                    break;
                case 4:
                    verResultados();
                    break;
                case 5:
                    exportar();
                    break;
                default:
                    break;
            }
        } while (opcao != 0);
    }

    private void importar() {
        listarFicheirosDisponiveis();
        String nome = Consola.lerTexto("Nome do ficheiro (ex.: missao-exemplo) ou caminho completo: ");
        Path ficheiro = Path.of(nome);
        if (!Files.exists(ficheiro)) {
            ficheiro = this.pastaMissoes.resolve(nome.toLowerCase().endsWith(".json") ? nome : nome + ".json");
        }
        try {
            Missao missao = new ImportarMapa().gerarMapa(ficheiro.toString());
            this.missoes.addMissao(missao);
            System.out.println("Missão \"" + missao.getcod_missao() + "\" (versão " + missao.getVersao() + ") importada.");
        } catch (IOException ex) {
            System.out.println("Não foi possível importar: " + ex.getMessage());
        }
    }

    private void listarFicheirosDisponiveis() {
        try (var ficheiros = Files.list(this.pastaMissoes)) {
            System.out.println("Ficheiros em " + this.pastaMissoes + ":");
            ficheiros.filter(f -> f.toString().toLowerCase().endsWith(".json"))
                    .forEach(f -> System.out.println("  " + f.getFileName()));
        } catch (IOException ex) {
            System.out.println("(a pasta " + this.pastaMissoes + " não existe)");
        }
    }

    /**
     * Pede ao utilizador que escolha uma das missões carregadas.
     *
     * @return a missão escolhida ou {@code null} se não houver missões ou o utilizador voltar atrás
     */
    private Missao escolherMissao() {
        if (this.missoes.getContMissoes() == 0) {
            System.out.println("Ainda não há missões carregadas. Use a opção 1.");
            return null;
        }
        Missao[] lista = new Missao[this.missoes.getContMissoes()];
        int i = 0;
        System.out.println("0 - Voltar");
        for (Missao missao : this.missoes.getListaMissao()) {
            lista[i++] = missao;
            System.out.println(i + " - Missão \"" + missao.getcod_missao() + "\", versão " + missao.getVersao()
                    + " (" + missao.getTot_simulacoes() + " simulações manuais)");
        }
        int opcao = Consola.lerInteiro("Missão: ", 0, lista.length);
        return opcao == 0 ? null : lista[opcao - 1];
    }

    private void jogar() {
        Missao missao = escolherMissao();
        if (missao == null) {
            return;
        }
        System.out.println("1 - Modo manual (o jogador escolhe cada movimento)");
        System.out.println("2 - Modo automático (melhor trajeto calculado para todas as entradas)");
        System.out.println("3 - Jogo automático (o Tó Cruz joga sozinho, turno a turno)");
        System.out.println("0 - Voltar");
        switch (Consola.lerInteiro("Modo: ", 0, 3)) {
            case 1:
                missao.modoManual();
                break;
            case 2:
                missao.modoAutomatico();
                break;
            case 3:
                missao.jogoAutomatico();
                break;
            default:
                break;
        }
    }

    private void verMapa() {
        Missao missao = escolherMissao();
        if (missao != null) {
            missao.getEdificio().drawMapa();
        }
    }

    private void verResultados() {
        Missao missao = escolherMissao();
        if (missao == null) {
            return;
        }
        if (missao.getTot_simulacoes() == 0) {
            System.out.println("Esta versão ainda não tem simulações manuais.");
            return;
        }
        System.out.println("Resultados da missão \"" + missao.getcod_missao() + "\", versão " + missao.getVersao()
                + " (ordenados pela vida restante do Tó Cruz):");
        missao.viewSimulacoes();
    }

    /**
     * Exporta um ficheiro por código de missão, com todas as versões e respetivas simulações.
     */
    private void exportar() {
        if (this.missoes.getContMissoes() == 0) {
            System.out.println("Não há missões para exportar.");
            return;
        }
        ExportarDado exportador = new ExportarDado();
        UnorderedListADT<Missao> versoes = new LinearLinkedUnorderedList<>();
        String codigoAtual = null;
        for (Missao missao : this.missoes.getListaMissao()) {
            if (codigoAtual != null && !codigoAtual.equals(missao.getcod_missao())) {
                exportarVersoes(exportador, codigoAtual, versoes);
                versoes = new LinearLinkedUnorderedList<>();
            }
            codigoAtual = missao.getcod_missao();
            versoes.addToRear(missao);
        }
        exportarVersoes(exportador, codigoAtual, versoes);
    }

    private void exportarVersoes(ExportarDado exportador, String codigo, UnorderedListADT<Missao> versoes) {
        Path destino = this.pastaExportacoes.resolve(codigo.replaceAll("[^A-Za-z0-9_-]", "") + ".json");
        exportador.exportarVersoes(destino.toString(), versoes);
        System.out.println("Trajetos exportados para " + destino);
    }
}
