package io;

import domain.Cliente;
import domain.Conta;
import domain.ValorInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.memory.InMemoryContaRepository;
import persistence.memory.InMemoryTransacaoRepository;
import persistence.memory.UnidadeDeTrabalhoSimples;
import service.BancoService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtratoExporterTest {

    @TempDir
    Path pasta;

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    @Test
    void exportaCabecalhoMovimentacoesESaldoAtual() throws Exception {
        BancoService banco = new BancoService(new InMemoryContaRepository(), new InMemoryTransacaoRepository(),
                new UnidadeDeTrabalhoSimples());
        Conta ana = banco.criarConta(new Cliente("Ana Souza", "12345678909"), v("1000.00"));
        Conta bruno = banco.criarConta(new Cliente("Bruno Lima", "98765432100"), v("0.00"));
        banco.sacar(ana.getNumero(), v("200.00"));
        banco.transferir(ana.getNumero(), bruno.getNumero(), v("300.50"));

        Path arquivo = banco.exportarExtrato(ana.getNumero(), pasta.resolve("extrato.txt"));

        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        String texto = String.join("\n", linhas);
        assertTrue(texto.contains("EXTRATO BANCÁRIO"));
        assertTrue(texto.contains("Ana Souza"));
        assertTrue(texto.contains("123.456.789-09"));
        assertTrue(texto.contains("Conta   : 1001"));
        assertTrue(texto.contains("+ R$ 1.000,00"));
        assertTrue(texto.contains("- R$ 200,00"));
        assertTrue(texto.contains("Transferência enviada 1002"));
        assertTrue(texto.contains("- R$ 300,50"));
        assertTrue(texto.contains("Saldo atual: R$ 499,50"));
        assertTrue(texto.contains("Total de movimentações: 3"));
        assertFalse(texto.contains("incompleto"));
    }

    @Test
    void avisaQuandoOHistoricoNaoFechaComOSaldo() throws Exception {
        Conta conta = new Conta(1, new Cliente("Ana", "12345678909"), v("100.00"));
        new ExtratoExporter().exportar(conta, List.of(), pasta.resolve("e.txt"));
        assertTrue(Files.readString(pasta.resolve("e.txt")).contains("incompleto"));
    }

    @Test
    void formataMoedaNoPadraoBrasileiro() {
        assertEquals("R$ 1.234.567,89", Formatos.moeda(v("1234567.89")));
        assertEquals("R$ 0,05", Formatos.moeda(v("0.05")));
        assertEquals("-R$ 10,00", Formatos.moeda(v("-10")));
    }

    @Test
    void formataCpf() {
        assertEquals("123.456.789-09", Formatos.cpf("12345678909"));
    }

    @Test
    void leValoresDigitadosDeVariasFormas() {
        assertEquals(v("1234.56"), Formatos.lerValor("1.234,56"));
        assertEquals(v("1234.56"), Formatos.lerValor("R$ 1234,56"));
        assertEquals(v("10.5"), Formatos.lerValor("10.5"));
        assertEquals(v("10"), Formatos.lerValor(" 10 "));
    }

    @Test
    void valorIlegivelOuVazioLancaExcecao() {
        assertThrows(ValorInvalidoException.class, () -> Formatos.lerValor("abc"));
        assertThrows(ValorInvalidoException.class, () -> Formatos.lerValor("  "));
        assertThrows(ValorInvalidoException.class, () -> Formatos.lerValor(null));
    }
}
