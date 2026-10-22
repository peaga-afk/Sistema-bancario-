package service;

import domain.Cliente;
import domain.Conta;
import domain.SaldoInsuficienteException;
import domain.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import persistence.PersistenciaException;
import persistence.TransacaoRepository;
import persistence.jdbc.ConnectionFactory;
import persistence.jdbc.JdbcContaRepository;
import persistence.jdbc.JdbcSessao;
import persistence.jdbc.JdbcTransacaoRepository;
import simulacao.SimulacaoBancaria;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** BancoService + banco H2 de verdade: atomicidade das gravações e concorrência ponta a ponta. */
class BancoServiceJdbcTest {

    private JdbcSessao sessao;
    private JdbcContaRepository contas;
    private JdbcTransacaoRepository transacoes;

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        ConnectionFactory fabrica = ConnectionFactory.h2Memoria();
        fabrica.criarTabelas();
        sessao = new JdbcSessao(fabrica);
        contas = new JdbcContaRepository(sessao);
        transacoes = new JdbcTransacaoRepository(sessao);
    }

    private BancoService novoBanco(TransacaoRepository repoTransacoes) {
        return new BancoService(contas, repoTransacoes, sessao);
    }

    @Test
    void dadosSobrevivemAUmNovoServico() throws Exception {
        BancoService banco = novoBanco(transacoes);
        Conta a = banco.criarConta(new Cliente("Ana", "12345678909"), v("100.00"));
        Conta b = banco.criarConta(new Cliente("Bruno", "98765432100"), v("50.00"));
        banco.transferir(a.getNumero(), b.getNumero(), v("30.00"));

        BancoService reiniciado = novoBanco(transacoes);
        assertEquals(v("70.00"), reiniciado.buscarConta(a.getNumero()).getSaldo());
        assertEquals(v("80.00"), reiniciado.buscarConta(b.getNumero()).getSaldo());
        assertEquals(2, reiniciado.extrato(a.getNumero()).size());
    }

    @Test
    void transferenciaQueFalhaNaSegundaGravacaoNaoDeixaNadaPelaMetade() throws Exception {
        BancoService normal = novoBanco(transacoes);
        Conta a = normal.criarConta(new Cliente("Ana", "12345678909"), v("100.00"));
        Conta b = normal.criarConta(new Cliente("Bruno", "98765432100"), v("50.00"));

        // repositório que funciona na 1ª gravação e quebra na 2ª (meio da transferência)
        AtomicInteger chamadas = new AtomicInteger();
        TransacaoRepository defeituoso = new TransacaoRepository() {
            @Override
            public Transacao salvar(Transacao t) {
                if (chamadas.incrementAndGet() == 2) {
                    throw new PersistenciaException("queda de energia simulada", null);
                }
                return transacoes.salvar(t);
            }

            @Override
            public List<Transacao> buscarPorConta(long n) {
                return transacoes.buscarPorConta(n);
            }
        };
        BancoService banco = novoBanco(defeituoso);
        Conta ca = banco.buscarConta(a.getNumero());
        Conta cb = banco.buscarConta(b.getNumero());

        assertThrows(PersistenciaException.class, () -> banco.transferir(ca.getNumero(), cb.getNumero(), v("40.00")));

        // memória voltou ao normal...
        assertEquals(v("100.00"), ca.getSaldo());
        assertEquals(v("50.00"), cb.getSaldo());
        // ...e o banco também: rollback desfez a 1ª transação gravada
        assertEquals(v("100.00"), banco.saldoNoBanco(ca.getNumero()));
        assertEquals(v("50.00"), banco.saldoNoBanco(cb.getNumero()));
        assertEquals(1, banco.extrato(ca.getNumero()).size());   // só o saldo inicial
        assertEquals(1, banco.extrato(cb.getNumero()).size());
    }

    @Test
    @Timeout(120)
    void simulacaoConcorrenteComBancoRealFechaTodosOsSaldos() throws Exception {
        BancoService banco = novoBanco(transacoes);
        banco.criarConta(new Cliente("Ana", "12345678909"), v("5000.00"));
        banco.criarConta(new Cliente("Bruno", "98765432100"), v("3000.00"));
        banco.criarConta(new Cliente("Carla", "11122233344"), v("1500.00"));

        SimulacaoBancaria sim = new SimulacaoBancaria(banco, 8, 150);
        SimulacaoBancaria.Resultado r = sim.executar();

        assertEquals(1200, r.operacoesRealizadas() + r.operacoesRecusadas());
        assertEquals(1200, sim.progresso());
        for (SimulacaoBancaria.ResultadoConta c : r.contas()) {
            assertTrue(c.consistente(), "conta " + c.numero() + " inconsistente: " + c);
            assertTrue(c.emMemoria().signum() >= 0);
        }
        assertTrue(r.consistente());
        BigDecimal total = banco.listarContas().stream().map(Conta::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        // transferências só movem dinheiro; depósitos e saques mudam o total, mas o banco confere com o extrato
        assertEquals(total, r.contas().stream().map(SimulacaoBancaria.ResultadoConta::noBanco)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void saqueSemSaldoNoBancoRealNaoGravaNada() throws Exception {
        BancoService banco = novoBanco(transacoes);
        Conta a = banco.criarConta(new Cliente("Ana", "12345678909"), v("10.00"));
        assertThrows(SaldoInsuficienteException.class, () -> banco.sacar(a.getNumero(), v("10.01")));
        assertEquals(v("10.00"), banco.saldoNoBanco(a.getNumero()));
        assertEquals(1, banco.extrato(a.getNumero()).size());
    }
}
