package simulacao;

import domain.Conta;
import domain.SaldoInsuficienteException;
import service.BancoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulação concorrente "de verdade": várias threads fazem depósitos, saques e
 * transferências aleatórios, ao mesmo tempo, nas contas do banco (com
 * persistência). No fim, confere se o saldo de cada conta bate EXATAMENTE com o
 * esperado: saldo inicial + tudo o que as threads conseguiram fazer.
 *
 * Cada thread guarda só as próprias somas (sem compartilhar nada mutável); o
 * resultado esperado é montado depois que todas terminam (join).
 */
public class SimulacaoBancaria {

    public record ResultadoConta(long numero, String titular, BigDecimal inicial, BigDecimal esperado,
                                 BigDecimal emMemoria, BigDecimal noBanco, BigDecimal peloExtrato) {
        public boolean consistente() {
            return esperado.compareTo(emMemoria) == 0
                    && esperado.compareTo(noBanco) == 0
                    && esperado.compareTo(peloExtrato) == 0;
        }
    }

    public record Resultado(List<ResultadoConta> contas, int operacoesRealizadas, int operacoesRecusadas,
                            long tempoMs, int threads) {
        public boolean consistente() {
            return contas.stream().allMatch(ResultadoConta::consistente);
        }
    }

    private final BancoService banco;
    private final int numeroDeThreads;
    private final int operacoesPorThread;
    private final AtomicInteger concluidas = new AtomicInteger();

    public SimulacaoBancaria(BancoService banco, int numeroDeThreads, int operacoesPorThread) {
        if (numeroDeThreads < 1 || operacoesPorThread < 1) {
            throw new IllegalArgumentException("Threads e operações devem ser pelo menos 1");
        }
        this.banco = banco;
        this.numeroDeThreads = numeroDeThreads;
        this.operacoesPorThread = operacoesPorThread;
    }

    public int totalDeOperacoes() {
        return numeroDeThreads * operacoesPorThread;
    }

    /** Quantas operações já terminaram (pode ser lido de qualquer thread). */
    public int progresso() {
        return concluidas.get();
    }

    public Resultado executar() throws InterruptedException {
        List<Conta> contas = banco.listarContas();
        if (contas.size() < 2) {
            throw new IllegalStateException("A simulação precisa de pelo menos 2 contas");
        }
        long[] numeros = contas.stream().mapToLong(Conta::getNumero).toArray();
        Map<Long, BigDecimal> inicial = new HashMap<>();
        for (Conta c : contas) {
            inicial.put(c.getNumero(), c.getSaldo());
        }

        concluidas.set(0);
        List<ClienteSimulado> clientes = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numeroDeThreads; i++) {
            ClienteSimulado cliente = new ClienteSimulado(numeros);
            clientes.add(cliente);
            threads.add(new Thread(cliente, "cliente-" + (i + 1)));
        }

        long inicio = System.nanoTime();
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join();   // depois do join, os dados de cada Cliente podem ser lidos com segurança
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        Map<Long, BigDecimal> variacao = new HashMap<>();
        int ok = 0;
        int recusadas = 0;
        for (ClienteSimulado c : clientes) {
            ok += c.realizadas;
            recusadas += c.recusadas;
            c.variacao.forEach((n, v) -> variacao.merge(n, v, BigDecimal::add));
        }

        List<ResultadoConta> resultados = new ArrayList<>();
        for (Conta c : contas) {
            BigDecimal esperado = inicial.get(c.getNumero()).add(variacao.getOrDefault(c.getNumero(), BigDecimal.ZERO));
            resultados.add(new ResultadoConta(c.getNumero(), c.getTitular().getNome(), inicial.get(c.getNumero()),
                    esperado, c.getSaldo(), banco.saldoNoBanco(c.getNumero()), banco.saldoPeloExtrato(c.getNumero())));
        }
        return new Resultado(resultados, ok, recusadas, ms, numeroDeThreads);
    }

    /** Um cliente simulado: sorteia operações e anota só o que de fato deu certo. */
    private class ClienteSimulado implements Runnable {
        private final long[] numeros;
        private final Map<Long, BigDecimal> variacao = new HashMap<>();
        private int realizadas;
        private int recusadas;

        ClienteSimulado(long[] numeros) {
            this.numeros = numeros;
        }

        @Override
        public void run() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < operacoesPorThread; i++) {
                BigDecimal valor = BigDecimal.valueOf(r.nextInt(500, 20_001), 2); // R$ 5,00 a R$ 200,00
                long a = numeros[r.nextInt(numeros.length)];
                int sorteio = r.nextInt(10);
                try {
                    if (sorteio < 4) {                      // 40% depósito
                        banco.depositar(a, valor);
                        somar(a, valor);
                    } else if (sorteio < 7) {               // 30% saque
                        banco.sacar(a, valor);
                        somar(a, valor.negate());
                    } else {                                // 30% transferência
                        long b = numeros[r.nextInt(numeros.length)];
                        if (b == a) {
                            b = numeros[(indice(a) + 1) % numeros.length];
                        }
                        banco.transferir(a, b, valor);
                        somar(a, valor.negate());
                        somar(b, valor);
                    }
                    realizadas++;
                } catch (SaldoInsuficienteException e) {
                    recusadas++;
                }
                concluidas.incrementAndGet();
            }
        }

        private int indice(long numero) {
            for (int i = 0; i < numeros.length; i++) {
                if (numeros[i] == numero) {
                    return i;
                }
            }
            return 0;
        }

        private void somar(long numero, BigDecimal delta) {
            variacao.merge(numero, delta, BigDecimal::add);
        }
    }
}
