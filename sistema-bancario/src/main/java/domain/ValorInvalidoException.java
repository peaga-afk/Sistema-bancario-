package domain;

/**
 * Lançada quando um valor monetário é nulo, zero, negativo ou tem mais de
 * duas casas decimais. É uma exceção não verificada: indica erro de uso.
 */
public class ValorInvalidoException extends IllegalArgumentException {

    public ValorInvalidoException(String mensagem) {
        super(mensagem);
    }
}
