package tocruz.exceptions;

import java.io.IOException;

/**
 * Lançada quando o ficheiro JSON de uma missão existe mas tem dados inconsistentes
 * (divisões inexistentes, campos em falta, valores negativos, ...).
 *
 * <p>Estende {@link IOException} para que quem já tratava erros de leitura trate também
 * este caso sem alterações.</p>
 *
 * @author Francisco Oliveira
 * @version 2.0
 */
public class MapaInvalidoException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message descrição do problema encontrado no ficheiro
     */
    public MapaInvalidoException(String message) {
        super(message);
    }
}
