package domain;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de CONCORRÊNCIA — Semana 3.
 *
 * Todas as threads esperam num CountDownLatch e são liberadas juntas, para
 * maximizar a disputa pela conta. Os testes são repetidos várias vezes porque
 * uma race condition pode passar "por sorte" numa execução e falhar em outra.
 *
 * Com a Conta SEM synchronized (Semana 2), estes testes falham.
 */
class ContaConcorrenciaTest {

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private static Conta novaConta(long numero, String saldo) {
        return new Conta(numero, new Cliente("Ana Souza", "123.456.789-09"), v(saldo));
    }

    /** Executa as tarefas todas ao mesmo tempo e espera terminarem. */
    private static void executarEmParalelo(List<Runnable> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        for (Runnable t : tarefas) {
            futuros.add(pool.submit(() -> {
                try {
                    largada.await();      // todas esperam aqui...
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                t.run();                  // ...e disparam juntas
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get(30, TimeUnit.SECONDS);  // propaga exceções das tarefas
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }

    @RepeatedTest(10)
    @Timeout(60)
    void depositosConcorrentesNaoPerdemAtualizacoes() throws Exception {
        Conta conta = novaConta(1L, "0.00");
        int threads = 8;
        int depositosPorThread = 50_000;

        List<Runnable> tarefas = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tarefas.add(() -> {
                for (int j = 0; j < depositosPorThread; j++) {
                    conta.depositar(v("1.00"));
                }
            });
        }
        executarEmParalelo(tarefas);

        // 8 * 50.000 * R$ 1,00 = R$ 400.000,00
        assertEquals(v("400000.00"), conta.getSaldo());
    }

    @RepeatedTest(10)
    @Timeout(60)
    void depositosESaquesMisturadosFecham() throws Exception {
        Conta conta = novaConta(1L, "1000.00");
        int threads = 8;
        int repeticoes = 30_000;

        List<Runnable> tarefas = new ArrayList<>();
        AtomicInteger recusados = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            tarefas.add(() -> {
                for (int j = 0; j < repeticoes; j++) {
                    conta.depositar(v("10.00"));
                    try {
                        conta.sacar(v("5.00"));
                    } catch (SaldoInsuficienteException e) {
                        recusados.incrementAndGet();
                    }
                }
            });
        }
        executarEmParalelo(tarefas);

        // Cada repetição lucra R$ 5,00; nenhum saque deve ter sido recusado.
        assertEquals(0, recusados.get());
        assertEquals(v("1201000.00"), conta.getSaldo()); // 1.000 + 8 * 30.000 * 5
    }

    @RepeatedTest(20)
    @Timeout(60)
    void saquesSimultaneosNuncaDeixamSaldoNegativo() throws Exception {
        // Saldo para exatamente 10 saques de R$ 10,00; 50 threads tentam sacar.
        // Se a verificação "tem saldo?" e o desconto não forem atômicos, mais
        // de 10 saques passam e o saldo fica negativo.
        Conta conta = novaConta(1L, "100.00");
        AtomicInteger aprovados = new AtomicInteger();
        AtomicInteger recusados = new AtomicInteger();

        List<Runnable> tarefas = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tarefas.add(() -> {
                try {
                    conta.sacar(v("10.00"));
                    aprovados.incrementAndGet();
                } catch (SaldoInsuficienteException e) {
                    recusados.incrementAndGet();
                }
            });
        }
        executarEmParalelo(tarefas);

        assertEquals(10, aprovados.get());
        assertEquals(40, recusados.get());
        assertEquals(v("0.00"), conta.getSaldo());
    }

    @Test
    @Timeout(60)
    void saquesEDepositosSimultaneosPreservamOTotal() throws Exception {
        // Várias contas diferentes, cada uma com sua própria disputa.
        List<Conta> contas = new ArrayList<>();
        List<Runnable> tarefas = new ArrayList<>();
        for (long n = 1; n <= 4; n++) {
            Conta c = novaConta(n, "500.00");
            contas.add(c);
            for (int t = 0; t < 4; t++) {
                tarefas.add(() -> {
                    for (int j = 0; j < 20_000; j++) {
                        c.depositar(v("2.00"));
                    }
                });
            }
        }
        executarEmParalelo(tarefas);

        for (Conta c : contas) {
            assertEquals(v("160500.00"), c.getSaldo()); // 500 + 4 * 20.000 * 2
        }
    }
}
