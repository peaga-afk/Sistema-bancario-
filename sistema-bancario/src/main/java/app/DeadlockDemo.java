package app;

import domain.Cliente;
import domain.Conta;
import domain.SaldoInsuficienteException;
import simulacao.TransferenciaThread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;

/**
 * Demonstração da Semana 4: DEADLOCK (de propósito) x transferência segura.
 *
 * Parte 1: duas threads fazem transferências em sentidos opostos travando as
 *          contas na ordem "origem, depois destino". Cada uma segura uma conta
 *          e espera a outra: deadlock. O JVM consegue detectá-lo (ThreadMXBean).
 * Parte 2: as mesmas transferências com Conta.transferirPara, que trava sempre
 *          a conta de menor número primeiro: termina normalmente.
 */
public class DeadlockDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== PARTE 1: ordem de bloqueio INGÊNUA (origem -> destino) ===");
        parte1();
        System.out.println();
        System.out.println("=== PARTE 2: Conta.transferirPara (menor número primeiro) ===");
        parte2();
    }

    private static Conta nova(long numero) {
        return new Conta(numero, new Cliente("Cliente " + numero, "123.456.789-09"), new BigDecimal("1000000.00"));
    }

    /** Transferência "ingênua": trava a origem, depois o destino. Só para demonstrar o problema. */
    private static void transferirIngenuo(Conta origem, Conta destino, BigDecimal valor) {
        synchronized (origem) {
            pausa(100); // dá tempo de a outra thread travar a conta dela: o deadlock fica certo
            synchronized (destino) {
                try {
                    origem.sacar(valor);
                    destino.depositar(valor);
                } catch (SaldoInsuficienteException e) {
                    // não acontece: saldo alto
                }
            }
        }
    }

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void parte1() throws Exception {
        Conta a = nova(1);
        Conta b = nova(2);
        BigDecimal valor = new BigDecimal("10.00");

        Thread t1 = new Thread(() -> transferirIngenuo(a, b, valor), "transferencia-A-para-B");
        Thread t2 = new Thread(() -> transferirIngenuo(b, a, valor), "transferencia-B-para-A");
        t1.setDaemon(true);   // daemon: se travarem, não impedem o programa de terminar
        t2.setDaemon(true);
        t1.start();
        t2.start();

        t1.join(2000);
        t2.join(2000);

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) {
            System.out.println("Nenhum deadlock detectado (inesperado).");
            return;
        }
        System.out.println("DEADLOCK DETECTADO! Threads presas para sempre:");
        for (ThreadInfo info : mx.getThreadInfo(ids)) {
            System.out.println("  - " + info.getThreadName() + " espera um lock de "
                    + info.getLockOwnerName() + " (" + info.getLockName() + ")");
        }
        System.out.println("Cada thread segura uma conta e espera a outra. Ninguém avança.");
    }

    private static void parte2() throws Exception {
        Conta a = nova(1);
        Conta b = nova(2);
        BigDecimal valor = new BigDecimal("1.00");
        int repeticoes = 100_000;
        TransferenciaThread aParaB = new TransferenciaThread(a, b, valor, repeticoes);
        TransferenciaThread bParaA = new TransferenciaThread(b, a, valor, repeticoes);
        Thread t1 = new Thread(aParaB, "A-para-B");
        Thread t2 = new Thread(bParaA, "B-para-A");

        long inicio = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        System.out.printf("Concluído em %d ms, sem deadlock.%n", ms);
        System.out.printf("Transferências A->B: %d | B->A: %d%n", aParaB.getRealizadas(), bParaA.getRealizadas());
        System.out.println("Saldo A: " + a.getSaldo() + " | Saldo B: " + b.getSaldo()
                + " | Total: " + a.getSaldo().add(b.getSaldo()) + " (esperado: 2000000.00)");
    }
}
