package simulacao;

import domain.Cliente;
import domain.Conta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes DETERMINÍSTICOS da tarefa de simulação: chamam run() na própria
 * thread de teste, sem concorrência. O teste com várias threads simultâneas
 * entra na Semana 3, depois da correção (antes dela ele seria instável).
 */
class OperacaoBancariaThreadTest {

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private static Conta novaConta(String saldo) {
        return new Conta(1L, new Cliente("Ana Souza", "123.456.789-09"), v(saldo));
    }

    @Test
    void executaDepositosESaquesNaOrdem() {
        Conta conta = novaConta("1000.00");
        OperacaoBancariaThread tarefa = new OperacaoBancariaThread(conta, v("10.00"), v("5.00"), 100);

        tarefa.run();

        // 1000 + 100 * (10 - 5) = 1500
        assertEquals(v("1500.00"), conta.getSaldo());
        assertEquals(100, tarefa.getDepositosRealizados());
        assertEquals(100, tarefa.getSaquesRealizados());
        assertEquals(0, tarefa.getSaquesRecusados());
    }

    @Test
    void contaSaquesRecusadosPorSaldoInsuficiente() {
        Conta conta = novaConta("0.00");
        // deposita 1 e tenta sacar 100: o saldo máximo é 10, então todo saque é recusado
        OperacaoBancariaThread tarefa = new OperacaoBancariaThread(conta, v("1.00"), v("100.00"), 10);

        tarefa.run();

        assertEquals(v("10.00"), conta.getSaldo());
        assertEquals(10, tarefa.getDepositosRealizados());
        assertEquals(0, tarefa.getSaquesRealizados());
        assertEquals(10, tarefa.getSaquesRecusados());
    }

    @Test
    void rejeitaContaNulaEQuantidadeNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new OperacaoBancariaThread(null, v("1.00"), v("1.00"), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new OperacaoBancariaThread(novaConta("1.00"), v("1.00"), v("1.00"), -1));
    }
}
