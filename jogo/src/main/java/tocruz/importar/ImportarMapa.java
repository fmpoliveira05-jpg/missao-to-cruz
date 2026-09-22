package tocruz.importar;

import tocruz.exceptions.MapaInvalidoException;
import tocruz.interfaces.Importar;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import ed.linkedlist.LinearLinkedUnorderedList;
import tocruz.mapa.Alvo;
import tocruz.mapa.Divisao;
import tocruz.mapa.Edificio;
import tocruz.missoes.Missao;
import tocruz.personagens.Inimigo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Classe responsável por gerar o Mapa da missão do ToCruz
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 2.0 (validação dos dados e mensagens de erro claras)
 */
public class ImportarMapa implements Importar {

    /**
     * Divisões do edifício que está a ser importado, para procurar pelo nome.
     */
    private LinearLinkedUnorderedList<Divisao> listaDivisoes = new LinearLinkedUnorderedList<>();

    /**
     * Procura uma divisão pelo nome entre as já importadas.
     *
     * @param name nome da divisão
     * @return a divisão ou {@code null} se não existir
     */
    private Divisao findDivisao(String name) {
        for (Divisao divisao : this.listaDivisoes) {
            if (divisao.getName().equals(name)) {
                return divisao;
            }
        }
        return null;
    }

    /**
     * Procura uma divisão que tem obrigatoriamente de existir.
     *
     * @param name nome da divisão
     * @param contexto onde a divisão foi referida (para a mensagem de erro)
     * @return a divisão
     * @throws MapaInvalidoException se a divisão não existir
     */
    private Divisao divisaoObrigatoria(String name, String contexto) throws MapaInvalidoException {
        Divisao divisao = findDivisao(name);
        if (divisao == null) {
            throw new MapaInvalidoException(contexto + " refere a divisão \"" + name + "\", que não existe no edifício.");
        }
        return divisao;
    }

    /**
     * Gera o mapa do edifício e os dados da missão a partir de um ficheiro JSON.
     *
     * <p>O ficheiro é validado por completo: se alguma ligação, inimigo, item ou o alvo
     * referirem uma divisão que não existe, ou se faltarem campos obrigatórios, é lançada uma
     * {@link MapaInvalidoException} com a explicação do problema.</p>
     *
     * @param path caminho para o ficheiro JSON
     * @return a missão criada com os dados do ficheiro
     * @throws NullPointerException se o caminho for nulo
     * @throws FileNotFoundException se o ficheiro não existir
     * @throws MapaInvalidoException se o conteúdo for inválido
     * @throws IOException se ocorrer outro erro de leitura
     */
    @Override
    public Missao gerarMapa(String path) throws NullPointerException, FileNotFoundException, IOException {
        if (path == null) {
            throw new NullPointerException("O caminho do ficheiro nao pode ser null");
        }

        Object obj;
        try (Reader reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
            obj = new JSONParser().parse(reader);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("O ficheiro " + path + " não existe.");
        } catch (ParseException e) {
            throw new MapaInvalidoException("O ficheiro " + path + " não é JSON válido: " + e);
        }

        if (!(obj instanceof JSONObject)) {
            throw new MapaInvalidoException("O ficheiro deve conter um objeto JSON com a descrição da missão.");
        }

        try {
            return construirMissao((JSONObject) obj);
        } catch (ClassCastException e) {
            throw new MapaInvalidoException("Um dos campos do ficheiro tem um tipo inesperado: " + e.getMessage());
        }
    }

    private Missao construirMissao(JSONObject json) throws MapaInvalidoException {
        this.listaDivisoes = new LinearLinkedUnorderedList<>();

        String codigoMissao = (String) campo(json, "cod-missao");
        long versao = (long) campo(json, "versao");
        Edificio edificio = new Edificio();

        for (Object nome : (JSONArray) campo(json, "edificio")) {
            if (findDivisao((String) nome) != null) {
                throw new MapaInvalidoException("A divisão \"" + nome + "\" aparece repetida.");
            }
            Divisao divisao = new Divisao((String) nome);
            edificio.addDivisao(divisao);
            this.listaDivisoes.addToRear(divisao);
        }

        for (Object ligacao : (JSONArray) campo(json, "ligacoes")) {
            JSONArray par = (JSONArray) ligacao;
            if (par.size() != 2) {
                throw new MapaInvalidoException("Cada ligação tem de ter exatamente duas divisões: " + par);
            }
            Divisao origem = divisaoObrigatoria((String) par.get(0), "Uma ligação");
            Divisao destino = divisaoObrigatoria((String) par.get(1), "Uma ligação");
            edificio.addLigacao(origem, destino, 0);
        }

        for (Object item : (JSONArray) campo(json, "inimigos")) {
            JSONObject inimigo = (JSONObject) item;
            String nome = (String) campo(inimigo, "nome");
            long poder = (long) campo(inimigo, "poder");
            if (poder < 0) {
                throw new MapaInvalidoException("O inimigo " + nome + " tem poder negativo.");
            }
            Divisao divisao = divisaoObrigatoria((String) campo(inimigo, "divisao"), "O inimigo " + nome);

            // O peso de uma divisão no grafo é o poder total dos inimigos que lá estão.
            long poderTotal = poder;
            for (Inimigo existente : divisao.getInimigos()) {
                poderTotal += existente.getPoder();
            }
            divisao.addInimigo(new Inimigo(nome, poder));
            edificio.updateWeight(divisao, poderTotal);
        }

        JSONArray entradas = (JSONArray) campo(json, "entradas-saidas");
        if (entradas.isEmpty()) {
            throw new MapaInvalidoException("O edifício tem de ter pelo menos uma entrada/saída.");
        }
        for (Object entrada : entradas) {
            divisaoObrigatoria((String) entrada, "A lista de entradas/saídas").setEntrada_saida(true);
        }

        JSONObject alvo = (JSONObject) campo(json, "alvo");
        Divisao divisaoAlvo = divisaoObrigatoria((String) campo(alvo, "divisao"), "O alvo");
        divisaoAlvo.setAlvo(new Alvo((String) campo(alvo, "tipo")));

        Object itens = json.get("itens");
        if (itens != null) {
            for (Object item : (JSONArray) itens) {
                importarItem((JSONObject) item);
            }
        }

        return new Missao(codigoMissao, versao, edificio);
    }

    private void importarItem(JSONObject item) throws MapaInvalidoException {
        String tipo = (String) campo(item, "tipo");
        TypeItemCura type;
        long pontosVida;
        if ("colete".equalsIgnoreCase(tipo)) {
            type = TypeItemCura.COLETE;
            pontosVida = (long) campo(item, "pontos-extra");
        } else {
            type = TypeItemCura.KIT_VIDA;
            pontosVida = (long) campo(item, "pontos-recuperados");
        }
        if (pontosVida <= 0) {
            throw new MapaInvalidoException("Um item do tipo " + tipo + " tem de dar pontos de vida positivos.");
        }
        Divisao divisao = divisaoObrigatoria((String) campo(item, "divisao"), "Um item do tipo " + tipo);
        divisao.setItem(new ItemCura(type, pontosVida));
    }

    private static Object campo(JSONObject json, String nome) throws MapaInvalidoException {
        Object valor = json.get(nome);
        if (valor == null) {
            throw new MapaInvalidoException("Falta o campo obrigatório \"" + nome + "\".");
        }
        return valor;
    }
}
