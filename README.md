# Missão Tó Cruz — Mission Simulator

A Java simulator in which agent Tó Cruz enters a building controlled by criminals, reaches an objective (rescue a hostage, recover an object, or disable a weapon), and escapes alive. The building is a weighted graph; automatic route planning uses Dijkstra's algorithm. Play through the console or a graphical interface with a top-down tactical floor plan, animations, three difficulty levels, and enemies that pursue the player.

A group assignment for **Data Structures**, in the second year of the BSc in Computer Engineering at ESTG – Polytechnic of Porto (2024/25), reviewed in 2026.

[![CI](https://github.com/fmpoliveira05-jpg/missao-to-cruz/actions/workflows/ci.yml/badge.svg)](https://github.com/fmpoliveira05-jpg/missao-to-cruz/actions/workflows/ci.yml)

![Elite difficulty: Tó Cruz fires at an enemy in Office 3; the objective is in the adjacent laboratory](docs/interface.png)

## Team

| Contributor | GitHub |
|---|---|
| Artur Pinto | [@ArturESTG](https://github.com/ArturESTG) |
| Francisco Oliveira | [@fmpoliveira05-jpg](https://github.com/fmpoliveira05-jpg) |

The original commit history has been preserved, so individual contributions remain visible.

**My contribution:** in the submitted version, I mainly worked on simulation logic (enemy movement and attacks, combat, and Tó Cruz's health-kit use), automatic mode, and Javadoc. In the 2026 review, I reorganised the project into a Gradle multi-module build, reviewed and corrected data-structure choices, extracted a testable game engine from the console code, and built the graphical interface and difficulty levels.

## Assignment overview

The IMF needs a simulator for Tó Cruz to practise missions. Each mission is a JSON file describing rooms, connections, enemies and attack power, entrances/exits, the objective, and items such as health kits and protective vests.

Main rules:

- Tó Cruz starts with 100 health points and carries up to three health kits in a **backpack**, always using the most recently stored kit.
- Protective vests immediately add health, even above 100.
- Tó Cruz attacks first on entering a room with enemies; survivors retaliate during their phase.
- Each turn, enemies may move up to two rooms but must remain within two connections of their assigned room.
- A mission succeeds only if Tó Cruz reaches the objective and leaves the building alive.

The assignment prohibited the Java Collections Framework. The collections used here were developed by the team during the course.

## Features

- **Graphical interface:** furnished top-down rooms (laboratory, garage, helipad, dormitory, etc.), animated movement, gunfire, falls, and healing; a following camera and minimap; mouse and keyboard controls. It displays safe routes to the objective and nearest kit, and can show or execute an automatic plan.
- **Three difficulty levels:** Recruit (half enemy attack power), Agent (assignment rules), and Elite (stronger enemies that hear and pursue the player, limited room visibility, and an escape countdown after collecting the objective).
- **Mandatory objective:** leaving without it, dying, or running out of alarm turns in Elite mode fails the mission.
- **JSON mission import:** clear validation errors for unknown rooms, missing fields, and invalid values.
- **Manual console mode:** choose an entrance, then move, attack, use a kit, wait, or leave. Each turn suggests routes to the objective and nearest kit.
- **Automatic planning:** evaluate every entrance/exit combination, select the route with the most remaining health, and report whether the mission is feasible.
- **Autoplay:** enemies move and the agent recalculates its path each turn.
- **Simulation results:** manual runs ranked by remaining health, with route export to JSON.

## Data structures: where and why

| Structure | Use | Reason |
|---|---|---|
| Directed weighted graph (`Network`, adjacency matrix) | Building | Edge cost is damage suffered on entering the destination room, so direction matters. With 20–30 rooms, a matrix is small and provides O(1) edge-weight access. |
| Min-priority queue (`PriorityQueue`, backed by `ArrayHeap`) | Dijkstra | Extract the unprocessed room with the smallest accumulated distance. |
| Queue (`LinkedQueue`) | BFS for patrol areas and Elite pursuit; agent route | BFS visits by connection distance, finding a path with the fewest edges. Route steps are consumed in travel order. |
| Array list (`ArrayUnordered`) | Candidate enemy destinations and paths | Random destination selection needs indexed access. |
| Stack (`LinkedStack`) | Backpack | The most recently stored kit must be used first. |
| Ordered list (`LinearLinkedOrderedList`) | Missions by code/version; simulations by remaining health | Keep entries sorted as they are inserted. |
| Unordered linked list | Room enemies, defeated enemies, and turn events | Frequent insertion/removal with no required ordering. |

The course library also contains doubly linked lists, array-based and linked binary search trees, and heaps, with tests. `NetworkListaAdjacencia` remains incomplete; an adjacency-list representation would be more appropriate for very large, sparse buildings.

## Project structure

```text
estruturas-dados/  Collection library (ed.*)                         145 tests
jogo/             Model, engine (tocruz.motor), and mission modes     80 tests
app/              Console menus; missoes/ contains the example mission
gui/              Swing graphical interface                           4 tests
```

Game rules live in `tocruz.motor`, without console input/output:

- `MotorJogo` applies player actions and turn phases and records `Evento` objects for the interface to animate.
- `Dificuldade` defines the three difficulty levels.
- `MovimentoInimigos` handles patrols and pursuit.
- `PlaneadorMissao` calculates automatic plans.
- `JogadorAutomatico` executes autoplay.

Both the console (`Simulacoes`) and graphical interface (`JanelaMissao`) use the same engine.

## Run locally

Requires **Java 17+**. The Gradle wrapper handles Gradle installation.

```bash
git clone https://github.com/fmpoliveira05-jpg/missao-to-cruz.git
cd missao-to-cruz
./gradlew build                     # Build and run 229 tests; Windows: gradlew.bat build
./gradlew :gui:run                  # GUI with the example mission
./gradlew :app:run --console=plain  # Console version
```

Generate executable JARs:

```bash
./gradlew :gui:jar :app:jar
java -jar gui/build/libs/missao-to-cruz-gui.jar [mission.json]
cd app && java -jar build/libs/missao-to-cruz.jar
```

### First game in the GUI

![Difficulty selection](docs/niveis.png)

The interface uses Portuguese labels; the instructions below include their English meanings.

1. Choose **Recruta** (Recruit), **Agente** (Agent), or **Elite**.
2. Click a flashing green entrance to enter.
3. Click an adjacent room to move. Reachable rooms have dashed outlines. Blue shows the lowest-damage route to the objective; dashed green shows the route to the nearest kit. Hover over a room to see enemies and expected damage.
4. In a room with enemies, choose **Atacar** (Attack, A) or **Usar kit** (Use kit, K). When finding a kit, use, store, or leave it with 1, 2, or 3. **F** waits; **M** toggles the following camera.
5. After collecting the objective, return to an entrance and choose **Sair do edifício** (Leave building, S). In Elite mode, watch the alarm countdown.
6. **Plano automático** draws the automatic plan; **Jogar sozinho** starts turn-by-turn autoplay.

## Changes in the 2026 review

### Organisation

- Three separate IntelliJ projects connected through manually copied JARs became one Gradle multi-module build.
- Packages now follow Java naming conventions, such as `ed.linkedlist` instead of `LinkedList`.
- Generated files (`build/`, `.gradle/`, `.idea/`, and JARs) were removed from history, reducing the repository from approximately 80 MB to 5 MB while retaining commits and authors.

### Data structures and rules

- **Dijkstra:** the heap previously ordered rooms by creation order instead of accumulated distance. A distance-based priority queue fixes this; a regression test uses a graph where the old version chose the wrong path.
- **Graph weights:** room costs were written in both directions, and updating one room overwrote its neighbour's cost. Costs now apply only when entering a room and no longer depend on update order.
- **Entry cost:** now reflects actual combat damage: the agent attacks first and survivors retaliate until the room is cleared. Automatic health estimates match play with stationary enemies, and the return route does not count defeated enemies again.
- **Enemy movement:** previously unrestricted; bounded BFS now keeps patrols within two connections of the assigned room. Random choice uses an indexed list instead of a stack.
- **Building copy:** simulations previously copied the building with every room connected to every other room. Copies now reproduce only existing connections.
- **Graph iterator:** DFS from the first room omitted disconnected rooms. The iterator now visits all vertices.
- **Smaller fixes:** `Divisao.equals` compared names with `==`; using a kit above 100 health lost it; an enemy-movement condition could never be true.

### Engine and interface

- **Engine extracted from console:** `Simulacoes` mixed rules and prompts in 1,200 lines. Rules moved to `tocruz.motor`, with 23 new tests, and are shared by the console and GUI.
- **New graphical interface:** simulated annealing positions rooms on a grid to shorten connections and avoid crossings or rooms blocking corridors. Connections become straight, Z-shaped, or L-shaped corridors. Java2D draws everything in code without third-party images; animations follow engine events.
- **Difficulty:** Agent preserves assignment and console rules. Elite pursuit uses BFS; enemies advance one room per turn and, without an alarm, remain within three edges of their assigned room. Enemies reaching the agent's room are ambushed. The alarm allows the shortest exit path's turn count plus three turns.
- **Import:** validation replaces `NullPointerException` failures for misspelled rooms and `null` missions for missing files.
- **Export and menus:** fixed repeated versions and incorrect simulation counts; added the required map and results options.
