package simulacao;

import domain.Conta;
import domain.SaldoInsuficienteException;

import java.math.BigDecimal;

/**
 * Simula um cliente fazendo operações sobre uma conta.
 *
 * A cada repetição: deposita 'valorDeposito' e depois saca 'valorSaque'.
 * Esta classe NÃO tem regra de negócio e NÃO altera saldo diretamente:
 * apenas chama a Conta.
 *
 * Os contadores abaixo são escritos só pela própria thread; podem ser lidos
 * com segurança depois de thread.join(), que garante a visibilidade.
 */
public class OperacaoBancariaThread implements Runnable {

    private final Conta conta;
    private final BigDecimal valorDeposito;
    private final BigDecimal valorSaque;
    private final int repeticoes;

    private int depositosRealizados;
    private int saquesRealizados;
    private int saquesRecusados;

    public OperacaoBancariaThread(Conta conta, BigDecimal valorDeposito, BigDecimal valorSaque, int repeticoes) {
        if (conta == null) {
            throw new IllegalArgumentException("Conta é obrigatória");
        }
        if (repeticoes < 0) {
            throw new IllegalArgumentException("Repetições não pode ser negativo");
        }
        this.conta = conta;
        this.valorDeposito = valorDeposito;
        this.valorSaque = valorSaque;
        this.repeticoes = repeticoes;
    }

    @Override
    public void run() {
        for (int i = 0; i < repeticoes; i++) {
            conta.depositar(valorDeposito);
            depositosRealizados++;
            try {
                conta.sacar(valorSaque);
                saquesRealizados++;
            } catch (SaldoInsuficienteException e) {
                saquesRecusados++;
            }
        }
    }

    public int getDepositosRealizados() {
        return depositosRealizados;
    }

    public int getSaquesRealizados() {
        return saquesRealizados;
    }

    public int getSaquesRecusados() {
        return saquesRecusados;
    }
}
