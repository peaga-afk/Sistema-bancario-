package domain;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import simulacao.TransferenciaThread;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** Testes da Semana 4: transferências simultâneas, atomicidade e AUSÊNCIA de deadlock. */
class TransferenciaConcorrenciaTest {

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    private static Conta conta(long numero, String saldo) {
        return new Conta(numero, new Cliente("Cliente " + numero, "123.456.789-09"), v(saldo));
    }

    @Test
    void transferenciaSequencialMoveOValor() throws Exception {
        TransferenciaThread t = new TransferenciaThread(conta(1, "100.00"), conta(2, "0.00"), v("30.00"), 5);
        t.run();
        // 100 / 30 => 3 transferências cabem (90), 2 são recusadas
        assertEquals(3, t.getRealizadas());
        assertEquals(2, t.getRecusadas());
    }

    @RepeatedTest(10)
    @Timeout(60)
    void transferenciasCruzadasNaoCausamDeadlockNemPerdemDinheiro() throws Exception {
        Conta a = conta(1, "1000000.00");
        Conta b = conta(2, "1000000.00");
        TransferenciaThread aParaB = new TransferenciaThread(a, b, v("1.00"), 50_000);
        TransferenciaThread bParaA = new TransferenciaThread(b, a, v("1.00"), 50_000);

        // se houver deadlock, a execução estoura o tempo e o teste FALHA (não trava)
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(30), () -> {
            Thread t1 = new Thread(aParaB);
            Thread t2 = new Thread(bParaA);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        });

        assertEquals(50_000, aParaB.getRealizadas());
        assertEquals(50_000, bParaA.getRealizadas());
        assertEquals(v("1000000.00"), a.getSaldo());   // idas e voltas se anulam
        assertEquals(v("1000000.00"), b.getSaldo());
    }

    @RepeatedTest(5)
    @Timeout(90)
    void malhaDeTransferenciasEntreVariasContasPreservaOTotal() throws Exception {
        int nContas = 5;
        List<Conta> contas = new ArrayList<>();
        for (int i = 1; i <= nContas; i++) {
            contas.add(conta(i, "10000.00"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            futuros.add(pool.submit(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                try {
                    largada.await();
                    for (int i = 0; i < 20_000; i++) {
                        Conta de = contas.get(r.nextInt(nContas));
                        Conta para = contas.get(r.nextInt(nContas));
                        if (de == para) {
                            continue;
                        }
                        try {
                            de.transferirPara(para, BigDecimal.valueOf(r.nextInt(1, 5000), 2));
                        } catch (SaldoInsuficienteException e) {
                            // faz parte do jogo: saldo insuficiente é recusado
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        BigDecimal total = BigDecimal.ZERO;
        for (Conta c : contas) {
            assertTrue(c.getSaldo().signum() >= 0, "saldo negativo na conta " + c.getNumero());
            total = total.add(c.getSaldo());
        }
        assertEquals(v("50000.00"), total);
    }

    @Test
    @Timeout(60)
    void ninguemEnxergaODinheiroNoArDuranteUmaTransferencia() throws Exception {
        Conta a = conta(1, "1000.00");
        Conta b = conta(2, "1000.00");
        AtomicBoolean fim = new AtomicBoolean(false);
        AtomicBoolean violou = new AtomicBoolean(false);

        Thread transferidor = new Thread(() -> {
            try {
                for (int i = 0; i < 50_000; i++) {
                    a.transferirPara(b, v("1.00"));
                    b.transferirPara(a, v("1.00"));
                }
            } catch (SaldoInsuficienteException e) {
                violou.set(true);
            } finally {
                fim.set(true);
            }
        });
        // O observador trava as duas contas (na mesma ordem) e confere a soma.
        Thread observador = new Thread(() -> {
            while (!fim.get()) {
                synchronized (a) {
                    synchronized (b) {
                        if (a.getSaldo().add(b.getSaldo()).compareTo(v("2000.00")) != 0) {
                            violou.set(true);
                        }
                    }
                }
            }
        });
        transferidor.start();
        observador.start();
        transferidor.join();
        observador.join();

        assertFalse(violou.get(), "alguém viu uma soma diferente de 2000,00 no meio de uma transferência");
        assertEquals(v("2000.00"), a.getSaldo().add(b.getSaldo()));
    }
}
