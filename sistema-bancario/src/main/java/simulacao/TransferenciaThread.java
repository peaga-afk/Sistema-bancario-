package simulacao;

import domain.Conta;
import domain.SaldoInsuficienteException;

import java.math.BigDecimal;

/**
 * Simula um cliente fazendo transferências repetidas de 'origem' para
 * 'destino'. Duas dessas tarefas em sentidos opostos (A->B e B->A) são o
 * cenário clássico de deadlock quando a ordem de bloqueio não é consistente.
 *
 * Os contadores são escritos só pela própria thread; leia-os depois de join().
 */
public class TransferenciaThread implements Runnable {

    private final Conta origem;
    private final Conta destino;
    private final BigDecimal valor;
    private final int repeticoes;

    private int realizadas;
    private int recusadas;

    public TransferenciaThread(Conta origem, Conta destino, BigDecimal valor, int repeticoes) {
        if (origem == null || destino == null) {
            throw new IllegalArgumentException("Origem e destino são obrigatórias");
        }
        if (repeticoes < 0) {
            throw new IllegalArgumentException("Repetições não pode ser negativo");
        }
        this.origem = origem;
        this.destino = destino;
        this.valor = valor;
        this.repeticoes = repeticoes;
    }

    @Override
    public void run() {
        for (int i = 0; i < repeticoes; i++) {
            try {
                origem.transferirPara(destino, valor);
                realizadas++;
            } catch (SaldoInsuficienteException e) {
                recusadas++;
            }
        }
    }

    public int getRealizadas() {
        return realizadas;
    }

    public int getRecusadas() {
        return recusadas;
    }
}
