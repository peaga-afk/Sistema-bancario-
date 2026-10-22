package service;

import domain.Cliente;
import domain.Conta;
import domain.ContaNaoEncontradaException;
import domain.SaldoInsuficienteException;
import domain.TipoTransacao;
import domain.Transacao;
import io.ExtratoExporter;
import persistence.ContaRepository;
import persistence.TransacaoRepository;
import persistence.UnidadeDeTrabalho;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordena o fluxo de cada operação:
 *   1) a Conta valida e altera o saldo em memória (com sincronização);
 *   2) cria-se a Transacao (registro imutável);
 *   3) a Transacao e o novo saldo são gravados pelos repositórios.
 *
 * "A Conta decide. O Service coordena. O Repository grava. A Thread só pede."
 *
 * CONCORRÊNCIA: o service mantém UMA instância de Conta por número (as threads
 * compartilham o mesmo objeto). A gravação no banco acontece DENTRO do lock da
 * conta (o mesmo monitor que a Conta usa), para que a ordem das gravações seja
 * igual à ordem das operações em memória: sem isso, um saldo "velho" poderia
 * sobrescrever um saldo "novo" no banco. Em transferências, os dois locks são
 * tomados na mesma ordem global da Conta (Conta.ORDEM_DE_BLOQUEIO).
 *
 * CONSISTÊNCIA: se a gravação falhar, a operação é desfeita em memória e a
 * exceção é propagada; no banco, a UnidadeDeTrabalho faz rollback.
 */
public class BancoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    private final Map<Long, Conta> contas = new ConcurrentHashMap<>();
    private final Object travaCriacao = new Object();

    public BancoService(ContaRepository contaRepository, TransacaoRepository transacaoRepository,
                        UnidadeDeTrabalho unidadeDeTrabalho) {
        this.contaRepository = Objects.requireNonNull(contaRepository);
        this.transacaoRepository = Objects.requireNonNull(transacaoRepository);
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho);
        for (Conta c : contaRepository.listarTodas()) {   // recupera o que foi gravado antes
            contas.put(c.getNumero(), c);
        }
    }

    // ------------------------------------------------------------ contas

    public Conta criarConta(Cliente titular, BigDecimal saldoInicial) {
        synchronized (travaCriacao) {
            long numero = contaRepository.proximoNumero();
            Conta conta = new Conta(numero, titular, saldoInicial); // valida o saldo inicial
            unidadeDeTrabalho.executar(() -> {
                contaRepository.salvar(conta);
                if (conta.getSaldo().signum() > 0) {
                    // o saldo inicial entra no extrato como um depósito
                    transacaoRepository.salvar(Transacao.nova(numero, TipoTransacao.DEPOSITO, conta.getSaldo(), null));
                }
            });
            contas.put(numero, conta);
            return conta;
        }
    }

    public Conta buscarConta(long numero) {
        Conta conta = contas.computeIfAbsent(numero, n -> contaRepository.buscarPorNumero(n).orElse(null));
        if (conta == null) {
            throw new ContaNaoEncontradaException(numero);
        }
        return conta;
    }

    public List<Conta> listarContas() {
        List<Conta> lista = new ArrayList<>(contas.values());
        lista.sort(Conta.ORDEM_DE_BLOQUEIO);
        return lista;
    }

    // -------------------------------------------------------- operações

    public void depositar(long numero, BigDecimal valor) {
        Conta conta = buscarConta(numero);
        synchronized (conta) {
            conta.depositar(valor);                       // valida e altera a memória
            BigDecimal v = valor.setScale(2);             // já validado: no máximo 2 casas
            try {
                persistir(List.of(conta), List.of(Transacao.nova(numero, TipoTransacao.DEPOSITO, v, null)));
            } catch (RuntimeException e) {
                desfazer(() -> conta.sacar(v));           // volta a memória ao que era
                throw e;
            }
        }
    }

    public void sacar(long numero, BigDecimal valor) throws SaldoInsuficienteException {
        Conta conta = buscarConta(numero);
        synchronized (conta) {
            conta.sacar(valor);                           // lança se faltar saldo (nada muda)
            BigDecimal v = valor.setScale(2);
            try {
                persistir(List.of(conta), List.of(Transacao.nova(numero, TipoTransacao.SAQUE, v, null)));
            } catch (RuntimeException e) {
                conta.depositar(v);
                throw e;
            }
        }
    }

    public void transferir(long numeroOrigem, long numeroDestino, BigDecimal valor)
            throws SaldoInsuficienteException {
        Conta origem = buscarConta(numeroOrigem);
        Conta destino = buscarConta(numeroDestino);
        if (origem == destino) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta");
        }
        // mesma ordem de bloqueio da Conta: menor número primeiro (evita deadlock)
        Conta primeira = Conta.ORDEM_DE_BLOQUEIO.compare(origem, destino) < 0 ? origem : destino;
        Conta segunda = (primeira == origem) ? destino : origem;

        synchronized (primeira) {
            synchronized (segunda) {
                origem.transferirPara(destino, valor);    // atômica; lança se faltar saldo
                BigDecimal v = valor.setScale(2);
                try {
                    persistir(List.of(origem, destino), List.of(
                            Transacao.nova(numeroOrigem, TipoTransacao.TRANSFERENCIA_ENVIADA, v, numeroDestino),
                            Transacao.nova(numeroDestino, TipoTransacao.TRANSFERENCIA_RECEBIDA, v, numeroOrigem)));
                } catch (RuntimeException e) {
                    desfazer(() -> destino.transferirPara(origem, v));
                    throw e;
                }
            }
        }
    }

    // ------------------------------------------------------------ extrato

    public List<Transacao> extrato(long numero) {
        buscarConta(numero);
        return transacaoRepository.buscarPorConta(numero);
    }

    /** Grava o extrato da conta em um arquivo de texto. */
    public Path exportarExtrato(long numero, Path destino) throws IOException {
        Conta conta = buscarConta(numero);
        new ExtratoExporter().exportar(conta, extrato(numero), destino);
        return destino;
    }

    /** Saldo gravado no banco (pode ser diferente do da memória se algo falhou). */
    public BigDecimal saldoNoBanco(long numero) {
        return contaRepository.buscarPorNumero(numero).orElseThrow(() -> new ContaNaoEncontradaException(numero))
                .getSaldo();
    }

    /** Saldo recalculado somando o extrato: entradas menos saídas. */
    public BigDecimal saldoPeloExtrato(long numero) {
        BigDecimal soma = BigDecimal.ZERO.setScale(2);
        for (Transacao t : extrato(numero)) {
            soma = ehEntrada(t.getTipo()) ? soma.add(t.getValor()) : soma.subtract(t.getValor());
        }
        return soma;
    }

    public static boolean ehEntrada(TipoTransacao tipo) {
        return tipo == TipoTransacao.DEPOSITO || tipo == TipoTransacao.TRANSFERENCIA_RECEBIDA;
    }

    // ------------------------------------------------------------ internos

    private void persistir(List<Conta> afetadas, List<Transacao> novas) {
        unidadeDeTrabalho.executar(() -> {
            for (Transacao t : novas) {
                transacaoRepository.salvar(t);
            }
            for (Conta c : afetadas) {
                contaRepository.salvar(c);
            }
        });
    }

    @FunctionalInterface
    private interface Reversao {
        void executar() throws SaldoInsuficienteException;
    }

    /** Reverte a memória. Sob o lock da conta isso nunca falta saldo; se faltar, é bug. */
    private static void desfazer(Reversao reversao) {
        try {
            reversao.executar();
        } catch (SaldoInsuficienteException e) {
            throw new IllegalStateException("Falha ao desfazer a operação em memória", e);
        }
    }
}
