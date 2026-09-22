# Missão Tó Cruz – simulador de incursões

Jogo/simulador de consola em Java onde o agente Tó Cruz entra num edifício controlado por criminosos, tem de chegar ao alvo (resgatar um refém, recuperar um objeto, desarmar uma arma) e sair com vida. O edifício é um grafo, e as decisões do modo automático são tomadas com algoritmos de caminho mais curto sobre esse grafo.

Trabalho prático de grupo de **Estruturas de Dados** (2.º ano da Licenciatura em Engenharia Informática, ESTG – Politécnico do Porto, 2024/25), revisto em 2026.

[![CI](https://github.com/fmpoliveira05-jpg/missao-to-cruz/actions/workflows/ci.yml/badge.svg)](https://github.com/fmpoliveira05-jpg/missao-to-cruz/actions/workflows/ci.yml)

## Equipa

| | |
|---|---|
| Artur Pinto | [@ArturESTG](https://github.com/ArturESTG) |
| Francisco Oliveira | [@fmpoliveira05-jpg](https://github.com/fmpoliveira05-jpg) |

O histórico de *commits* original foi mantido, por isso o contributo de cada um continua visível no separador *Commits*.

## O enunciado em poucas palavras

A IMF quer um simulador para o Tó Cruz treinar as missões antes de as fazer. Cada missão vem num ficheiro JSON com as divisões do edifício, as ligações entre elas, os inimigos (e o seu poder de ataque), as entradas/saídas, a posição do alvo e os itens espalhados pelo edifício (kits de vida e coletes).

Regras principais:

- o Tó Cruz começa com 100 pontos de vida e guarda os kits numa **mochila** (usa sempre o do topo);
- os coletes somam pontos imediatamente, mesmo acima dos 100;
- quando entra numa sala com inimigos há confronto; os inimigos que sobrevivem atacam no turno seguinte;
- a cada turno os inimigos podem mover-se até duas divisões a partir da sala que guardam;
- a missão só tem sucesso se o Tó Cruz passar pelo alvo e sair do edifício com vida.

O enunciado proibia a Java Collections Framework: todas as coleções usadas são as desenvolvidas pelo grupo nas aulas.

## Funcionalidades

- **Importar missões** a partir de JSON, com validação completa do ficheiro (divisões inexistentes, campos em falta, valores inválidos são reportados com uma mensagem clara).
- **Modo manual** – o jogador escolhe a entrada e, turno a turno, para onde se mover, se ataca ou se usa um kit. No fim de cada turno é sugerido o melhor caminho até ao alvo e até ao kit mais próximo.
- **Modo automático** – calcula, para todas as entradas possíveis, o trajeto que chega ao alvo com mais vida e indica se é possível voltar a sair sem morrer.
- **Jogo automático** – o Tó Cruz joga sozinho, turno a turno, seguindo as mesmas regras.
- **Ver o mapa** do edifício na consola.
- **Resultados das simulações manuais** de cada missão, ordenados pela vida restante.
- **Exportar** os trajetos das simulações para JSON (um ficheiro por missão, com todas as versões).

## Estruturas de dados e onde são usadas

| Estrutura | Uso no jogo |
|---|---|
| Grafo pesado (`Network`, matriz de adjacência) | o edifício; o peso de cada divisão é o poder total dos inimigos que lá estão |
| Dijkstra com *heap* mínima (`LinkedHeap`) | caminho que chega ao alvo perdendo menos vida; caminho até ao kit mais próximo |
| Pilha (`LinkedStack`) | mochila de kits de vida |
| Fila (`LinkedQueue`) | trajeto percorrido pelo Tó Cruz, pela ordem em que foi feito |
| Lista ordenada (`LinearLinkedOrderedList`) | missões por código/versão e simulações por vida restante |
| Lista não ordenada | inimigos em cada divisão, inimigos abatidos, itens recolhidos |

Estão também implementadas (e testadas) listas em vetor, listas duplamente ligadas, árvores binárias de pesquisa e filas de prioridade, que fazem parte da biblioteca da unidade curricular.

## Estrutura do projeto

O projeto é um *build* Gradle com três módulos:

```
estruturas-dados/   biblioteca de coleções (pacotes ed.*)            142 testes
jogo/               modelo e regras do simulador (pacotes tocruz.*)    46 testes
app/                menu de consola; missoes/ tem o ficheiro de exemplo
```

## Como executar

Requisitos: **Java 17** ou superior. Não é preciso instalar o Gradle, o *wrapper* trata disso.

```bash
git clone https://github.com/fmpoliveira05-jpg/missao-to-cruz.git
cd missao-to-cruz
./gradlew build                      # compila e corre os 188 testes (no Windows: gradlew.bat build)
./gradlew :app:run --console=plain   # arranca o simulador
```

Também é possível gerar um `.jar` único e executá-lo dentro da pasta `app/`:

```bash
./gradlew :app:jar
cd app && java -jar build/libs/missao-to-cruz.jar
```

### Primeira partida

1. `1` – importar missão → escrever `missao-exemplo`.
2. `3` – ver o mapa, para perceber onde estão os inimigos, os itens e o alvo (um laboratório com uma arma química).
3. `2` – jogar → escolher a missão → `1` (modo manual). Escolhe-se a entrada e, a cada turno, a divisão seguinte. Quando há inimigos na sala escolhe-se entre atacar ou usar um kit.
4. `4` – ver os resultados das tentativas de todas as versões dessa missão, da melhor para a pior (com a versão de cada uma).
5. `5` – exportar os trajetos para `app/exportacoes/`.

## O que mudou na revisão de 2026

- Os três projetos IntelliJ independentes (que dependiam uns dos outros através de `.jar` copiados à mão) passaram a ser um único *build* Gradle multi-módulo com dependências entre projetos.
- Os pacotes passaram a seguir as convenções Java (`ed.linkedlist` em vez de `LinkedList`, por exemplo). Antes existiam pacotes `Exceptions` e `Interfaces` com o mesmo nome em dois projetos diferentes.
- O histórico foi limpo de ficheiros gerados (`build/`, `.gradle/`, `.idea/`, `.jar`), o que reduziu o repositório de ~80 MB para ~5 MB, mantendo todos os *commits* e autores.
- A importação passou a **validar** o ficheiro: antes, uma divisão mal escrita causava um `NullPointerException` e um ficheiro inexistente resultava numa missão `null`. O ficheiro também não era fechado depois de lido.
- A exportação repetia a mesma versão uma vez por cada simulação e contava mal o total de simulações.
- Cada classe criava o seu próprio `Scanner` sobre `System.in`, o que fazia perder entrada; agora há um único ponto de leitura.
- O menu ganhou as opções "ver mapa" e "ver resultados" pedidas no enunciado, e os erros de importação deixaram de terminar a aplicação.
- O teste da importação de um ficheiro válido apontava para um caminho que não existia e acabava por comparar `null` com `null`; foi reescrito com um ficheiro de teste real e passaram a existir testes para ficheiros inválidos.
