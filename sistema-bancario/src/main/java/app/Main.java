package app;

import domain.Cliente;
import domain.Conta;
import simulacao.OperacaoBancariaThread;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SEMANA 2 — Expondo a race condition.
 *
 * Dispara várias threads depositando e sacando na MESMA conta e compara o
 * saldo obtido com o saldo esperado. Roda várias rodadas: sem sincronização,
 * o saldo final varia de uma rodada para outra.
 *
 * Para guardar o log do relatório:
 *   java -cp target/classes app.Main > log-antes.txt
 */
public class Main {

    private static final int RODADAS = 10;
    private static final int THREADS = 8;
    private static final int REPETICOES_POR_THREAD = 100_000;

    private static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
    private static final BigDecimal VALOR_DEPOSITO = new BigDecimal("10.00");
    private static final BigDecimal VALOR_SAQUE = new BigDecimal("5.00");

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Simulação concorrente (SEM sincronização) ===");
        System.out.printf("%d threads x %d repetições (cada repetição = 1 depósito de R$ %s + 1 saque de R$ %s)%n%n",
                THREADS, REPETICOES_POR_THREAD, VALOR_DEPOSITO, VALOR_SAQUE);

        int corrompidas = 0;
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            if (executarRodada(rodada)) {
                corrompidas++;
            }
        }

        System.out.printf("%nResumo: %d de %d rodadas com saldo CORROMPIDO.%n", corrompidas, RODADAS);
    }

    /** Executa uma rodada. Devolve true se o saldo final ficou errado. */
    private static boolean executarRodada(int numeroRodada) throws InterruptedException {
        Conta conta = new Conta(1L, new Cliente("Ana Souza", "123.456.789-09"), SALDO_INICIAL);

        List<OperacaoBancariaThread> tarefas = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            OperacaoBancariaThread tarefa =
                    new OperacaoBancariaThread(conta, VALOR_DEPOSITO, VALOR_SAQUE, REPETICOES_POR_THREAD);
            tarefas.add(tarefa);
            threads.add(new Thread(tarefa, "cliente-" + i));
        }

        long inicio = System.nanoTime();
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join(); // depois do join, os contadores de cada tarefa são seguros de ler
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        int depositos = tarefas.stream().mapToInt(OperacaoBancariaThread::getDepositosRealizados).sum();
        int saques = tarefas.stream().mapToInt(OperacaoBancariaThread::getSaquesRealizados).sum();

        BigDecimal esperado = SALDO_INICIAL
                .add(VALOR_DEPOSITO.multiply(BigDecimal.valueOf(depositos)))
                .subtract(VALOR_SAQUE.multiply(BigDecimal.valueOf(saques)));
        BigDecimal obtido = conta.getSaldo();
        BigDecimal diferenca = obtido.subtract(esperado);
        boolean corrompido = diferenca.signum() != 0;

        System.out.printf("Rodada %2d | esperado R$ %s | obtido R$ %s | diferença R$ %s | %d ms | %s%n",
                numeroRodada, esperado.toPlainString(), obtido.toPlainString(),
                diferenca.toPlainString(), ms, corrompido ? "CORROMPIDO" : "ok");
        return corrompido;
    }
}
