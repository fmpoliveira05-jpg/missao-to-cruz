package tocruz.importar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tocruz.exceptions.MapaInvalidoException;
import tocruz.mapa.Divisao;
import tocruz.missoes.Missao;

public class TestImportarMapa {

    /** Ficheiro de exemplo copiado para os recursos de teste (o Gradle corre os testes na pasta do módulo). */
    private static final String EXEMPLO = "src/test/resources/missao-exemplo.json";

    private static final String MISSAO_VALIDA = "{"
            + "\"cod-missao\": \"teste\", \"versao\": 2,"
            + "\"edificio\": [\"Entrada\", \"Sala\"],"
            + "\"ligacoes\": [[\"Entrada\", \"Sala\"]],"
            + "\"inimigos\": [{\"nome\": \"mau\", \"poder\": 10, \"divisao\": \"Sala\"}],"
            + "\"entradas-saidas\": [\"Entrada\"],"
            + "\"alvo\": {\"divisao\": \"Sala\", \"tipo\": \"refem\"},"
            + "\"itens\": [{\"divisao\": \"Entrada\", \"pontos-extra\": 10, \"tipo\": \"colete\"}]"
            + "}";

    @TempDir
    Path pasta;

    private ImportarMapa importM;

    @BeforeEach
    public void setUp() {
        this.importM = new ImportarMapa();
    }

    private String escrever(String conteudo) throws IOException {
        Path ficheiro = this.pasta.resolve("missao.json");
        Files.writeString(ficheiro, conteudo, StandardCharsets.UTF_8);
        return ficheiro.toString();
    }

    @Test
    public void testGerarMapaComFicheiroDeExemplo() throws IOException {
        Missao missao = importM.gerarMapa(EXEMPLO);

        assertNotNull(missao);
        assertEquals("pata de coelho", missao.getcod_missao());
        assertEquals(1, missao.getVersao());
        assertEquals(missao, importM.gerarMapa(EXEMPLO), "importar duas vezes o mesmo ficheiro dá a mesma missão");

        int divisoes = 0;
        int entradas = 0;
        for (Divisao divisao : missao.getEdificio().getPlantaEdificio()) {
            divisoes++;
            if (divisao.isEntrada_saida()) {
                entradas++;
            }
        }
        assertEquals(21, divisoes);
        assertEquals(4, entradas);
    }

    @Test
    public void testGerarMapaMinimo() throws IOException {
        Missao missao = importM.gerarMapa(escrever(MISSAO_VALIDA));
        assertEquals("teste", missao.getcod_missao());
        assertEquals(2, missao.getVersao());
    }

    @Test
    public void testGerarMapaComFicheiroInvalivdoNull() {
        assertThrows(NullPointerException.class, () -> importM.gerarMapa(null));
    }

    @Test
    public void testFicheiroInexistente() {
        assertThrows(FileNotFoundException.class, () -> importM.gerarMapa(pasta.resolve("nao-existe.json").toString()));
    }

    @Test
    public void testJsonMalFormado() throws IOException {
        String ficheiro = escrever("{ \"cod-missao\": ");
        assertThrows(MapaInvalidoException.class, () -> importM.gerarMapa(ficheiro));
    }

    @Test
    public void testLigacaoParaDivisaoInexistente() throws IOException {
        String ficheiro = escrever(MISSAO_VALIDA.replace("[[\"Entrada\", \"Sala\"]]", "[[\"Entrada\", \"Cave\"]]"));
        MapaInvalidoException erro = assertThrows(MapaInvalidoException.class, () -> importM.gerarMapa(ficheiro));
        assertTrue(erro.getMessage().contains("Cave"));
    }

    @Test
    public void testInimigoNumaDivisaoInexistente() throws IOException {
        String ficheiro = escrever(MISSAO_VALIDA.replace("\"poder\": 10, \"divisao\": \"Sala\"", "\"poder\": 10, \"divisao\": \"Telhado\""));
        assertThrows(MapaInvalidoException.class, () -> importM.gerarMapa(ficheiro));
    }

    @Test
    public void testCampoObrigatorioEmFalta() throws IOException {
        String ficheiro = escrever(MISSAO_VALIDA.replace("\"alvo\": {\"divisao\": \"Sala\", \"tipo\": \"refem\"},", ""));
        MapaInvalidoException erro = assertThrows(MapaInvalidoException.class, () -> importM.gerarMapa(ficheiro));
        assertTrue(erro.getMessage().contains("alvo"));
    }
}
