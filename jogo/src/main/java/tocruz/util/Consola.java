package tocruz.util;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Ponto único de leitura do teclado.
 *
 * <p>Antes, várias classes criavam o seu próprio {@code Scanner} sobre {@code System.in}.
 * Cada {@code Scanner} guarda parte da entrada num buffer interno, por isso o que um lia podia
 * "desaparecer" para o outro (sobretudo quando a entrada vinha de um ficheiro ou de um pipe).
 * Todas as classes passam a partilhar esta instância.</p>
 *
 * @author Francisco Oliveira
 * @version 2.0
 */
public final class Consola {

    /** Scanner partilhado por toda a aplicação. */
    public static final Scanner SCANNER = new Scanner(System.in, StandardCharsets.UTF_8);

    private Consola() {
    }

    /**
     * Lê um número inteiro dentro de um intervalo, repetindo a pergunta até ser válido.
     *
     * @param pergunta texto a mostrar
     * @param min valor mínimo aceite
     * @param max valor máximo aceite
     * @return o número introduzido
     * @throws NoSuchElementException se a entrada terminar
     */
    public static int lerInteiro(String pergunta, int min, int max) {
        while (true) {
            System.out.print(pergunta);
            String linha = lerLinha().trim();
            try {
                int valor = Integer.parseInt(linha);
                if (valor >= min && valor <= max) {
                    return valor;
                }
            } catch (NumberFormatException ignorada) {
                // mensagem abaixo
            }
            System.out.println("Opção inválida: introduza um número entre " + min + " e " + max + ".");
        }
    }

    /**
     * Lê uma linha de texto não vazia.
     *
     * @param pergunta texto a mostrar
     * @return o texto, sem espaços nas pontas
     * @throws NoSuchElementException se a entrada terminar
     */
    public static String lerTexto(String pergunta) {
        while (true) {
            System.out.print(pergunta);
            String linha = lerLinha().trim();
            if (!linha.isEmpty()) {
                return linha;
            }
        }
    }

    private static String lerLinha() {
        String linha = SCANNER.nextLine();
        // Uma leitura anterior com nextInt() pode ter deixado o fim de linha por consumir.
        while (linha.isEmpty()) {
            linha = SCANNER.nextLine();
        }
        return linha;
    }
}
