package domain;

/** Lançada quando se pede uma conta cujo número não existe. */
public class ContaNaoEncontradaException extends RuntimeException {

    private final long numeroConta;

    public ContaNaoEncontradaException(long numeroConta) {
        super("Conta " + numeroConta + " não encontrada");
        this.numeroConta = numeroConta;
    }

    public long getNumeroConta() {
        return numeroConta;
    }
}
