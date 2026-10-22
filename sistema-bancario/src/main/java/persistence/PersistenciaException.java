package persistence;

/** Erro ao acessar o armazenamento (banco de dados). Encapsula SQLException. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
