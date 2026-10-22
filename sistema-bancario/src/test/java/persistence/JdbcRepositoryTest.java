package persistence;

import domain.Cliente;
import domain.Conta;
import domain.TipoTransacao;
import domain.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.jdbc.ConnectionFactory;
import persistence.jdbc.JdbcContaRepository;
import persistence.jdbc.JdbcSessao;
import persistence.jdbc.JdbcTransacaoRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testa os repositórios JDBC contra um H2 real (em memória). */
class JdbcRepositoryTest {

    private JdbcSessao sessao;
    private JdbcContaRepository contas;
    private JdbcTransacaoRepository transacoes;

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    private static Conta conta(long n, String saldo) {
        return new Conta(n, new Cliente("Cliente " + n, "12345678909"), v(saldo));
    }

    @BeforeEach
    void setUp() {
        ConnectionFactory fabrica = ConnectionFactory.h2Memoria();
        fabrica.criarTabelas();
        fabrica.criarTabelas(); // repetir é seguro (IF NOT EXISTS)
        sessao = new JdbcSessao(fabrica);
        contas = new JdbcContaRepository(sessao);
        transacoes = new JdbcTransacaoRepository(sessao);
    }

    @Test
    void salvaEBuscaConta() {
        contas.salvar(conta(1001, "250.75"));
        Conta c = contas.buscarPorNumero(1001).orElseThrow();
        assertEquals("Cliente 1001", c.getTitular().getNome());
        assertEquals(v("250.75"), c.getSaldo());
    }

    @Test
    void salvarDeNovoAtualizaOSaldoSemDuplicar() {
        contas.salvar(conta(1001, "100.00"));
        contas.salvar(conta(1001, "175.00"));
        assertEquals(1, contas.listarTodas().size());
        assertEquals(v("175.00"), contas.buscarPorNumero(1001).orElseThrow().getSaldo());
    }

    @Test
    void buscarContaInexistenteDevolveVazio() {
        assertTrue(contas.buscarPorNumero(42).isEmpty());
    }

    @Test
    void listaOrdenadaEProximoNumero() {
        assertEquals(1001L, contas.proximoNumero());
        contas.salvar(conta(1003, "1.00"));
        contas.salvar(conta(1001, "1.00"));
        List<Conta> todas = contas.listarTodas();
        assertEquals(1001L, todas.get(0).getNumero());
        assertEquals(1003L, todas.get(1).getNumero());
        assertEquals(1004L, contas.proximoNumero());
    }

    @Test
    void salvaTransacaoComIdGeradoERecuperaEmOrdem() {
        contas.salvar(conta(1001, "0.00"));
        contas.salvar(conta(1002, "0.00"));
        Transacao a = transacoes.salvar(Transacao.nova(1001, TipoTransacao.DEPOSITO, v("10.00"), null));
        Transacao b = transacoes.salvar(Transacao.nova(1001, TipoTransacao.TRANSFERENCIA_ENVIADA, v("4.00"), 1002L));

        assertNotNull(a.getId());
        assertTrue(b.getId() > a.getId());
        List<Transacao> lista = transacoes.buscarPorConta(1001);
        assertEquals(2, lista.size());
        assertEquals(TipoTransacao.DEPOSITO, lista.get(0).getTipo());
        assertNull(lista.get(0).getContaContraparte());
        assertEquals(1002L, lista.get(1).getContaContraparte());
        assertEquals(v("4.00"), lista.get(1).getValor());
        assertTrue(transacoes.buscarPorConta(1002).isEmpty());
    }

    @Test
    void transacaoDeContaInexistenteViolaChaveEstrangeira() {
        assertThrows(PersistenciaException.class,
                () -> transacoes.salvar(Transacao.nova(777, TipoTransacao.DEPOSITO, v("1.00"), null)));
    }

    @Test
    void bancoRecusaSaldoNegativo() {
        contas.salvar(conta(1001, "10.00"));
        sessao.comConexao(c -> {
            try (var ps = c.prepareStatement("UPDATE contas SET saldo = -1 WHERE numero = 1001")) {
                assertThrows(java.sql.SQLException.class, ps::executeUpdate);
            }
            return null;
        });
    }

    @Test
    void unidadeDeTrabalhoConfirmaTudoQuandoDaCerto() {
        contas.salvar(conta(1001, "0.00"));
        sessao.executar(() -> {
            transacoes.salvar(Transacao.nova(1001, TipoTransacao.DEPOSITO, v("5.00"), null));
            contas.salvar(conta(1001, "5.00"));
        });
        assertEquals(1, transacoes.buscarPorConta(1001).size());
        assertEquals(v("5.00"), contas.buscarPorNumero(1001).orElseThrow().getSaldo());
    }

    @Test
    void unidadeDeTrabalhoDesfazTudoQuandoAlgoFalha() {
        contas.salvar(conta(1001, "0.00"));
        assertThrows(IllegalStateException.class, () -> sessao.executar(() -> {
            transacoes.salvar(Transacao.nova(1001, TipoTransacao.DEPOSITO, v("5.00"), null));
            contas.salvar(conta(1001, "5.00"));
            throw new IllegalStateException("falha no meio");
        }));
        assertTrue(transacoes.buscarPorConta(1001).isEmpty(), "a transação gravada deveria ter sofrido rollback");
        assertEquals(v("0.00"), contas.buscarPorNumero(1001).orElseThrow().getSaldo());
    }
}
