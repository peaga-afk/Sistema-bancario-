package domain;

import java.math.BigDecimal;

/**
 * Lançada quando um saque ou transferência pede mais do que o saldo da conta.
 * É uma exceção verificada: é uma situação de negócio esperada, e quem chama
 * precisa decidir o que fazer com ela.
 */
public class SaldoInsuficienteException extends Exception {

    private final long numeroConta;
    private final BigDecimal saldoAtual;
    private final BigDecimal valorSolicitado;

    public SaldoInsuficienteException(long numeroConta, BigDecimal saldoAtual, BigDecimal valorSolicitado) {
        super("Saldo insuficiente na conta " + numeroConta
                + ": saldo " + saldoAtual.toPlainString()
                + ", solicitado " + valorSolicitado.toPlainString());
        this.numeroConta = numeroConta;
        this.saldoAtual = saldoAtual;
        this.valorSolicitado = valorSolicitado;
    }

    public long getNumeroConta() {
        return numeroConta;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public BigDecimal getValorSolicitado() {
        return valorSolicitado;
    }
}
