package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de regra de negócio, sequenciais (SEM concorrência) — Semana 1. */
class ContaTest {

    private Conta contaAna;
    private Conta contaBruno;

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @BeforeEach
    void setUp() {
        contaAna = new Conta(1L, new Cliente("Ana Souza", "123.456.789-09"), v("100.00"));
        contaBruno = new Conta(2L, new Cliente("Bruno Lima", "987.654.321-00"), v("50.00"));
    }

    @Test
    void contaNasceComSaldoInicial() {
        assertEquals(v("100.00"), contaAna.getSaldo());
    }

    @Test
    void saldoInicialNegativoERejeitado() {
        assertThrows(ValorInvalidoException.class,
                () -> new Conta(3L, new Cliente("Carla", "111.222.333-44"), v("-0.01")));
    }

    @Test
    void depositarAumentaOSaldo() {
        contaAna.depositar(v("25.50"));
        assertEquals(v("125.50"), contaAna.getSaldo());
    }

    @Test
    void sacarReduzOSaldo() throws SaldoInsuficienteException {
        contaAna.sacar(v("40.00"));
        assertEquals(v("60.00"), contaAna.getSaldo());
    }

    @Test
    void sacarTodoOSaldoEPermitido() throws SaldoInsuficienteException {
        contaAna.sacar(v("100.00"));
        assertEquals(v("0.00"), contaAna.getSaldo());
    }

    @Test
    void saqueComSaldoInsuficienteLancaExcecaoENaoAlteraSaldo() {
        SaldoInsuficienteException e = assertThrows(SaldoInsuficienteException.class,
                () -> contaAna.sacar(v("100.01")));
        assertEquals(v("100.00"), contaAna.getSaldo());
        assertEquals(1L, e.getNumeroConta());
        assertEquals(v("100.00"), e.getSaldoAtual());
        assertEquals(v("100.01"), e.getValorSolicitado());
    }

    @Test
    void valorZeroOuNegativoERejeitado() {
        assertThrows(ValorInvalidoException.class, () -> contaAna.depositar(BigDecimal.ZERO));
        assertThrows(ValorInvalidoException.class, () -> contaAna.depositar(v("-5.00")));
        assertThrows(ValorInvalidoException.class, () -> contaAna.sacar(BigDecimal.ZERO));
        assertThrows(ValorInvalidoException.class, () -> contaAna.sacar(v("-5.00")));
        assertEquals(v("100.00"), contaAna.getSaldo());
    }

    @Test
    void valorNuloOuComMaisDeDuasCasasERejeitado() {
        assertThrows(ValorInvalidoException.class, () -> contaAna.depositar(null));
        assertThrows(ValorInvalidoException.class, () -> contaAna.depositar(v("10.001")));
        assertEquals(v("100.00"), contaAna.getSaldo());
    }

    @Test
    void transferenciaMoveOValorEPreservaASomaTotal() throws SaldoInsuficienteException {
        BigDecimal somaAntes = contaAna.getSaldo().add(contaBruno.getSaldo());

        contaAna.transferirPara(contaBruno, v("30.00"));

        assertEquals(v("70.00"), contaAna.getSaldo());
        assertEquals(v("80.00"), contaBruno.getSaldo());
        assertEquals(somaAntes, contaAna.getSaldo().add(contaBruno.getSaldo()));
    }

    @Test
    void transferenciaComSaldoInsuficienteNaoAlteraNenhumaConta() {
        assertThrows(SaldoInsuficienteException.class,
                () -> contaAna.transferirPara(contaBruno, v("100.01")));
        assertEquals(v("100.00"), contaAna.getSaldo());
        assertEquals(v("50.00"), contaBruno.getSaldo());
    }

    @Test
    void transferenciaParaAMesmaContaERejeitada() {
        assertThrows(IllegalArgumentException.class, () -> contaAna.transferirPara(contaAna, v("10.00")));
        assertEquals(v("100.00"), contaAna.getSaldo());
    }

    @Test
    void transferenciaParaContaNulaERejeitada() {
        assertThrows(IllegalArgumentException.class, () -> contaAna.transferirPara(null, v("10.00")));
    }
}
