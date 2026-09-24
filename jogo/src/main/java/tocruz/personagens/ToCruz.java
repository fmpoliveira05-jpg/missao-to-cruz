package tocruz.personagens;

import tocruz.exceptions.AllLifeException;
import ed.exceptions.EmptyCollectionException;
import tocruz.exceptions.UsedColectedItemException;
import tocruz.exceptions.WrongTypeItemException;
import ed.interfaces.StackADT;
import tocruz.interfaces.ToCruzIt;
import tocruz.items.ItemCura;
import tocruz.items.TypeItemCura;
import ed.stacks.LinkedStack;

/**
 * Classe que representa o personagem principal do jogo, ToCruz.
 *
 * @author Artur Pinto
 * Nº mecanográfico: 8230138
 * @author Francisco Oliveira
 * Nº mecanografico: 8230148
 * @version 1.0
 */
public class ToCruz extends Personagem implements ToCruzIt {

    /**
     * Dano que o Tó Cruz causa a cada inimigo em cada ataque.
     */
    public static final long PODER = 40;

    /**
     * Mochila para armazenar os kits de vida.
     */
    private StackADT<ItemCura> mochila;

    /**
     * Indica se o objetivo principal foi coletado.
     */
    private boolean colectedAlvo;

    /**
     * Construtor padrão que inicializa a personagem ToCruz sem uma divisão inicial.
     */
    public ToCruz() {
        super("ToCruz", PODER);
        this.mochila = new LinkedStack<>();
        this.colectedAlvo = false;
    }

    /**
     * Construtor alternativo para realizar uma deepCopy do ToCruz.
     *
     * @param id_personagem ID da personagem.
     * @param nome          Nome da personagem.
     * @param vida          Vida inicial da personagem.
     * @param poder         Poder inicial da personagem.
     * @param mochila       Mochila a conter os kits de cura.
     */
    public ToCruz(int id_personagem, String nome, long vida, long poder, StackADT<ItemCura> mochila) {
        super(id_personagem, nome, vida, poder);
        this.mochila = new LinkedStack<>();
        this.colectedAlvo = false;
    }

    /**
     * Verifica se o alvo foi coletado pelo To Cruz.
     *
     * @return {@code true} se o alvo foi coletado; caso contrário, {@code false}.
     */
    public boolean isColectedAlvo() {
        return colectedAlvo;
    }

    /**
     * Define o status de coleta do alvo.
     *
     * @param colectedAlvo {@code true} para indicar que o alvo foi coletado, {@code false} caso contrário.
     */
    public void setColectedAlvo(boolean colectedAlvo) {
        this.colectedAlvo = colectedAlvo;
    }

    /**
     * Retorna a mochila do To Cruz.
     *
     * @return A Stack com os kits de vida armazenados na mochila.
     */
    public StackADT<ItemCura> getMochila() {
        return mochila;
    }


    /**
     * Verifica se a mochila está cheia.
     *
     * @return {@code true} se a mochila estiver cheia; caso contrário, {@code false}.
     */
    @Override
    public boolean mochilaIsFull() {
        boolean full = false;

        if (mochila.size() == 3) {
            full = true;
        }

        return full;
    }

    /**
     * Metodo para verificar se a mochila contém pelo menos um kit de vida.
     *
     * @return true se a mochila contiver pelo menos um kit de vida, false se estiver vazia.
     */
    @Override
    public boolean mochilaTemKit() {
        boolean contem = true;

        if (mochila.isEmpty()) {
            contem = false;
        }

        return contem;
    }

    /**
     * Metodo privado que aplica a cura à personagem utilizando um item de cura.
     * Atualiza a vida da personagem com base no tipo e na quantidade de cura fornecida pelo item.
     *
     * @param item O item de cura a ser utilizado.
     */
    private void curar(ItemCura item) throws AllLifeException {
        if (this.getVida() >= 100 && !item.getType().equals(TypeItemCura.COLETE)) {
            throw new AllLifeException("Nao e possivel usar o kit medico, porque a vida esta cheia");
        }

        long cura = item.getVida_recuperada();
        long nova_vida = 0;

        if (item.getType().equals(TypeItemCura.KIT_VIDA)) {
            if (this.getVida() + cura > 100) {
                nova_vida = 100;
            } else {
                nova_vida = this.getVida() + cura;
            }
        } else if (item.getType().equals(TypeItemCura.COLETE)) {
            nova_vida = this.getVida() + cura;
        }

        this.setVida(nova_vida);
        item.setCollected(true);
    }

    /**
     * Metodo para guardar um kit de vida na mochila.
     * Caso a mochila esteja cheia, a personagem poderá usá-lo imediatamente se necessário.
     *
     * @param item O item de cura a ser guardado.
     * @return Uma mensagem a indicar o estado da operação (guardado, usado ou descartado).
     * @throws NullPointerException   Se o item for nulo.
     * @throws WrongTypeItemException Se o tipo do item não for um kit de vida.
     */
    @Override
    public ItemCura guardarKit(ItemCura item) throws NullPointerException, WrongTypeItemException, AllLifeException, UsedColectedItemException {
        if (item == null) {
            throw new NullPointerException("O kit medico a guardar na mochila nao pode ser null");
        }

        if (item.getType() != TypeItemCura.KIT_VIDA) {
            throw new WrongTypeItemException("O tipo de item de cura a guardar na mochila e do tipo Kit de vida");
        }

        if (item.isCollected()) {
            throw new UsedColectedItemException("O item a usar ja foi utilizado anteriormente pelo To Cruz");
        }

        if (this.mochila.size() == 3) {
            // Mochila cheia: o kit é usado logo, se a vida não estiver cheia.
            try {
                curar(item);
            } catch (AllLifeException e) {
                // Vida cheia: o kit fica na divisão.
            }
        } else {
            mochila.push(item);
            item.setCollected(true);
        }

        return item;
    }

    /**
     * Metodo para usar um kit de vida da mochila.
     * Apenas possível se houver kits na mochila e se a vida não estiver cheia.
     *
     * @return Uma mensagem a indicar o resultado da operação.
     */
    @Override
    public ItemCura usarKit() throws EmptyCollectionException {
        ItemCura kit;

        if (!this.mochila.isEmpty()) {
            // ">=" e não "==": com um colete a vida pode passar dos 100, e nesse caso o kit era
            // retirado da mochila e perdido (curar() recusava-o depois de ter saído da pilha).
            if (this.getVida() >= 100) {
                throw new AllLifeException("Nao e possível usar o kit medico, porque a vida esta cheia");
            } else {
                kit = mochila.pop();
                curar(kit);
            }
        } else {
            throw new EmptyCollectionException("O To Cruz esta sem kits de vida na mochila");
        }

        return kit;
    }

    /**
     * Metodo para usar um item.
     *
     * @param item O item do tipo colete a ser usado.
     * @return Uma mensagem a indicar que o colete foi usado.
     * @throws NullPointerException   Se o colete for nulo.
     * @throws WrongTypeItemException Se o item não for do tipo colete.
     */
    @Override
    public ItemCura usarItem(ItemCura item) throws NullPointerException, UsedColectedItemException {
        if (item == null) {
            throw new NullPointerException("O item nao pode ser nulo");
        }

        if (item.isCollected()) {
            throw new UsedColectedItemException("O item a usar ja foi utilizado anteriormente pelo To Cruz");
        }

        try {
            curar(item);
        } catch (AllLifeException e) {
            // Vida cheia: o item fica por usar.
        }

        return item;
    }

    /**
     * Retorna uma string com todos os dados do ToCruz.
     *
     * @return Uma string com detalhes da personagem e respetiva mochila.
     */
    @Override
    public String toString() {
        return "ToCruz{" + "mochila=" + mochila + '}';
    }

    /**
     * Compara este objeto com outro para verificar igualdade.
     *
     * @param o O objeto a ser comparado.
     * @return {@code true} se os objetos forem iguais; caso contrário, {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Retorna o código hash para este objeto.
     *
     * @return O código hash calculado.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
