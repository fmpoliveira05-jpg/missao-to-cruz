package tocruz.exportar;

import tocruz.interfaces.ExportarInt;
import ed.interfaces.QueueADT;
import ed.interfaces.UnorderedListADT;
import ed.linkedlist.LinearLinkedUnorderedList;
import tocruz.mapa.Divisao;
import tocruz.missoes.Missao;
import tocruz.missoes.Simulacoes;
import tocruz.personagens.Inimigo;
import ed.queue.LinkedQueue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Classe que implementa a interface ExportarInt para exportar dados
 * das versões de missões para um ficheiro em formato JSON.
 * Esta classe permite gerar um ficheiro com informações detalhadas
 * sobre as missões, simulações e inimigos mortos.
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanográfico: 8230148
 *
 * @version 1.0
 */
public class ExportarDado implements ExportarInt {

    /**
     * Construtor da classe ExportarDado.
     * Inicializa a classe sem a necessidade de parâmetros.
     */
    public ExportarDado() {
    }

    /**
     * Metodo que exporta as versões das missões e os seus dados relacionados para um ficheiro JSON.
     * O ficheiro JSON gerado contém as versões das missões, simulações e os inimigos abatidos,
     * sendo guardado no caminho especificado.
     *
     * @param path O caminho para onde o ficheiro JSON será guardado.
     * @param listaVersoes A lista de missões com as respetivas versões e simulações.
     */
    @Override
    public void exportarVersoes(String path, UnorderedListADT<Missao> listaVersoes) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("cod_missao", listaVersoes.first().getcod_missao());

        JSONArray versaoArray = new JSONArray();
        int totalSimulacoes = 0;

        for (Missao missao : listaVersoes) {
            JSONObject versaoObject = new JSONObject();
            versaoObject.put("versao", missao.getVersao());

            JSONArray simulacoesArray = new JSONArray();
            Iterator<Simulacoes> iterator = missao.getSimulacoes().iterator();
            while (iterator.hasNext()) {
                Simulacoes simulacao = iterator.next();
                JSONObject simulacaoObject = new JSONObject();
                simulacaoObject.put("versao_simulacao", simulacao.getVersao_simulacao());
                simulacaoObject.put("vida_to_cruz", simulacao.getVida_to());

                JSONArray trajetoArray = new JSONArray();
                JSONArray inimigosArray = new JSONArray();

                QueueADT<Divisao> trajetoQueue = simulacao.getTrajeto_to();
                QueueADT<Divisao> tempTrajetoQueue = new LinkedQueue<>();
                UnorderedListADT<Inimigo> inimigosList = simulacao.getInimigos_dead();
                UnorderedListADT<Inimigo> tempInimigosList = new LinearLinkedUnorderedList<>();

                while (!trajetoQueue.isEmpty()) {
                    Divisao divisao = trajetoQueue.dequeue();
                    tempTrajetoQueue.enqueue(divisao);

                    trajetoArray.add(divisao.getName());
                }

                while (!tempTrajetoQueue.isEmpty()) {
                    trajetoQueue.enqueue(tempTrajetoQueue.dequeue());
                }
                simulacaoObject.put("trajetoPercorrido", trajetoArray);

                while (!inimigosList.isEmpty()) {
                    Inimigo inimigo = inimigosList.removeFirst();
                    tempInimigosList.addToFront(inimigo);

                    JSONObject inimigoObject = new JSONObject();
                    inimigoObject.put("nome", inimigo.getNome());
                    inimigoObject.put("poder", inimigo.getPoder());

                    inimigosArray.add(inimigoObject);

                }

                while (!tempInimigosList.isEmpty()) {
                    inimigosList.addToFront(tempInimigosList.removeFirst());
                }

                simulacaoObject.put("inimigosAbatidos", inimigosArray);

                simulacoesArray.add(simulacaoObject);
            }

            // Cada versão é escrita uma única vez, mesmo que não tenha simulações.
            versaoObject.put("simulacoes", simulacoesArray);
            versaoArray.add(versaoObject);
            totalSimulacoes += missao.getTot_simulacoes();
        }

        jsonObject.put("versoes", versaoArray);
        jsonObject.put("tot_simulacoes", totalSimulacoes);

        try {
            Path destino = Path.of(path);
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            Files.writeString(destino, jsonObject.toJSONString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Erro: " + e.getMessage() + " ao exportar trajetos");
        }
    }
}