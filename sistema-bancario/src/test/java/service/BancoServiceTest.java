package service;

import domain.Cliente;
import domain.Conta;
import domain.ContaNaoEncontradaException;
import domain.SaldoInsuficienteException;
import domain.TipoTransacao;
import domain.Transacao;
import domain.ValorInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenciaException;
import persistence.TransacaoRepository;
import persistence.memory.InMemoryContaRepository;
import persistence.memory.InMemoryTransacaoRepository;
import persistence.memory.UnidadeDeTrabalhoSimples;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** BancoService testado SEM banco real: repositórios em memória. */
class BancoServiceTest {

    private InMemoryContaRepository contas;
    private InMemoryTransacaoRepository transacoes;
    private BancoService banco;
    private Conta ana;
    private Conta bruno;

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        contas = new InMemoryContaRepository();
        transacoes = new InMemoryTransacaoRepository();
        banco = new BancoService(contas, transacoes, new UnidadeDeTrabalhoSimples());
        ana = banco.criarConta(new Cliente("Ana Souza", "123.456.789-09"), v("1000.00"));
        bruno = banco.criarConta(new Cliente("Bruno Lima", "987.654.321-00"), v("500.00"));
    }

    @Test
    void criarContaGeraNumerosSequenciaisERegistraSaldoInicialNoExtrato() {
        assertEquals(1001L, ana.getNumero());
        assertEquals(1002L, bruno.getNumero());
        List<Transacao> extrato = banco.extrato(ana.getNumero());
        assertEquals(1, extrato.size());
        assertEquals(TipoTransacao.DEPOSITO, extrato.get(0).getTipo());
        assertEquals(v("1000.00"), extrato.get(0).getValor());
    }

    @Test
    void criarContaComSaldoZeroNaoGeraTransacao() {
        Conta c = banco.criarConta(new Cliente("Carla", "111.222.333-44"), v("0.00"));
        assertTrue(banco.extrato(c.getNumero()).isEmpty());
    }

    @Test
    void criarContaComSaldoNegativoEhRejeitada() {
        assertThrows(ValorInvalidoException.class,
                () -> banco.criarConta(new Cliente("Carla", "111.222.333-44"), v("-1.00")));
        assertEquals(2, banco.listarContas().size());
    }

    @Test
    void depositarAlteraSaldoERegistraTransacaoEPersisteOSaldo() {
        banco.depositar(ana.getNumero(), v("250.50"));

        assertEquals(v("1250.50"), ana.getSaldo());
        assertEquals(v("1250.50"), banco.saldoNoBanco(ana.getNumero()));
        List<Transacao> extrato = banco.extrato(ana.getNumero());
        assertEquals(2, extrato.size());
        assertEquals(TipoTransacao.DEPOSITO, extrato.get(1).getTipo());
        assertEquals(v("250.50"), extrato.get(1).getValor());
    }

    @Test
    void depositarValorSemCasasDecimaisNormalizaParaDuasCasas() {
        banco.depositar(ana.getNumero(), v("10"));
        assertEquals(v("10.00"), banco.extrato(ana.getNumero()).get(1).getValor());
    }

    @Test
    void sacarReduzSaldoERegistraTransacao() throws Exception {
        banco.sacar(ana.getNumero(), v("300.00"));
        assertEquals(v("700.00"), ana.getSaldo());
        assertEquals(v("700.00"), banco.saldoNoBanco(ana.getNumero()));
        assertEquals(TipoTransacao.SAQUE, banco.extrato(ana.getNumero()).get(1).getTipo());
    }

    @Test
    void saqueSemSaldoNaoAlteraNadaENaoRegistraTransacao() {
        assertThrows(SaldoInsuficienteException.class, () -> banco.sacar(ana.getNumero(), v("1000.01")));
        assertEquals(v("1000.00"), ana.getSaldo());
        assertEquals(v("1000.00"), banco.saldoNoBanco(ana.getNumero()));
        assertEquals(1, banco.extrato(ana.getNumero()).size());
    }

    @Test
    void transferirMoveOValorRegistraDuasTransacoesEPreservaOTotal() throws Exception {
        banco.transferir(ana.getNumero(), bruno.getNumero(), v("400.00"));

        assertEquals(v("600.00"), ana.getSaldo());
        assertEquals(v("900.00"), bruno.getSaldo());
        assertEquals(v("600.00"), banco.saldoNoBanco(ana.getNumero()));
        assertEquals(v("900.00"), banco.saldoNoBanco(bruno.getNumero()));

        Transacao enviada = banco.extrato(ana.getNumero()).get(1);
        Transacao recebida = banco.extrato(bruno.getNumero()).get(1);
        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA, enviada.getTipo());
        assertEquals(bruno.getNumero(), enviada.getContaContraparte());
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA, recebida.getTipo());
        assertEquals(ana.getNumero(), recebida.getContaContraparte());
    }

    @Test
    void transferenciaSemSaldoNaoAlteraContasNemGravaNada() {
        assertThrows(SaldoInsuficienteException.class,
                () -> banco.transferir(ana.getNumero(), bruno.getNumero(), v("5000.00")));
        assertEquals(v("1000.00"), ana.getSaldo());
        assertEquals(v("500.00"), bruno.getSaldo());
        assertEquals(1, banco.extrato(ana.getNumero()).size());
        assertEquals(1, banco.extrato(bruno.getNumero()).size());
    }

    @Test
    void transferirParaAMesmaContaEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> banco.transferir(ana.getNumero(), ana.getNumero(), v("10.00")));
    }

    @Test
    void contaInexistenteLancaExcecao() {
        assertThrows(ContaNaoEncontradaException.class, () -> banco.depositar(9999L, v("1.00")));
        assertThrows(ContaNaoEncontradaException.class, () -> banco.extrato(9999L));
        assertThrows(ContaNaoEncontradaException.class, () -> banco.transferir(ana.getNumero(), 9999L, v("1.00")));
    }

    @Test
    void saldoPeloExtratoSempreFechaComOSaldoDaConta() throws Exception {
        banco.depositar(ana.getNumero(), v("100.00"));
        banco.sacar(ana.getNumero(), v("30.00"));
        banco.transferir(ana.getNumero(), bruno.getNumero(), v("200.00"));
        banco.transferir(bruno.getNumero(), ana.getNumero(), v("50.00"));

        for (Conta c : banco.listarContas()) {
            assertEquals(c.getSaldo(), banco.saldoPeloExtrato(c.getNumero()));
            assertEquals(c.getSaldo(), banco.saldoNoBanco(c.getNumero()));
        }
    }

    @Test
    void novoServicoRecuperaContasEExtratoGravadosAntes() throws Exception {
        banco.depositar(ana.getNumero(), v("100.00"));
        banco.transferir(ana.getNumero(), bruno.getNumero(), v("50.00"));

        // "reinicia a aplicação": novo serviço sobre os MESMOS repositórios
        BancoService reiniciado = new BancoService(contas, transacoes, new UnidadeDeTrabalhoSimples());

        assertEquals(2, reiniciado.listarContas().size());
        assertEquals(v("1050.00"), reiniciado.buscarConta(ana.getNumero()).getSaldo());
        assertEquals(v("550.00"), reiniciado.buscarConta(bruno.getNumero()).getSaldo());
        assertEquals(3, reiniciado.extrato(ana.getNumero()).size());
        assertEquals("Ana Souza", reiniciado.buscarConta(ana.getNumero()).getTitular().getNome());
    }

    @Test
    void seAGravacaoFalharAOperacaoEhDesfeitaEmMemoria() {
        TransacaoRepository quebrado = new TransacaoRepository() {
            @Override
            public Transacao salvar(Transacao t) {
                throw new PersistenciaException("banco fora do ar", null);
            }

            @Override
            public List<Transacao> buscarPorConta(long n) {
                return List.of();
            }
        };
        BancoService semBanco = new BancoService(contas, quebrado, new UnidadeDeTrabalhoSimples());
        Conta a = semBanco.buscarConta(ana.getNumero());
        Conta b = semBanco.buscarConta(bruno.getNumero());

        assertThrows(PersistenciaException.class, () -> semBanco.depositar(a.getNumero(), v("100.00")));
        assertThrows(PersistenciaException.class, () -> semBanco.sacar(a.getNumero(), v("100.00")));
        assertThrows(PersistenciaException.class, () -> semBanco.transferir(a.getNumero(), b.getNumero(), v("100.00")));

        assertEquals(v("1000.00"), a.getSaldo());   // tudo voltou ao que era
        assertEquals(v("500.00"), b.getSaldo());
    }
}
