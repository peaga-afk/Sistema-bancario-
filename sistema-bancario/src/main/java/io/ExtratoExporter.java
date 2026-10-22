package io;

import domain.Conta;
import domain.Transacao;
import service.BancoService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Exporta o extrato de uma conta para um arquivo de texto (UTF-8).
 * Recebe a lista de transações pronta: não acessa banco.
 */
public class ExtratoExporter {

    private static final String DUPLA = "=".repeat(78);
    private static final String SIMPLES = "-".repeat(78);

    public void exportar(Conta conta, List<Transacao> transacoes, Path destino) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            linha(w, DUPLA);
            linha(w, " EXTRATO BANCÁRIO");
            linha(w, DUPLA);
            linha(w, " Titular : " + conta.getTitular().getNome());
            linha(w, " CPF     : " + Formatos.cpf(conta.getTitular().getCpf()));
            linha(w, " Conta   : " + conta.getNumero());
            linha(w, " Emissão : " + Formatos.dataHora(LocalDateTime.now()));
            linha(w, SIMPLES);
            linha(w, String.format(" %-19s  %-24s  %15s  %15s", "DATA/HORA", "OPERAÇÃO", "VALOR", "SALDO"));
            linha(w, SIMPLES);

            BigDecimal saldoCorrente = BigDecimal.ZERO.setScale(2);
            for (Transacao t : transacoes) {
                boolean entrada = BancoService.ehEntrada(t.getTipo());
                saldoCorrente = entrada ? saldoCorrente.add(t.getValor()) : saldoCorrente.subtract(t.getValor());
                String descricao = Formatos.tipo(t.getTipo());
                if (t.getContaContraparte() != null) {
                    descricao += " " + t.getContaContraparte();
                }
                String valor = (entrada ? "+ " : "- ") + Formatos.moeda(t.getValor());
                linha(w, String.format(" %-19s  %-24s  %15s  %15s",
                        Formatos.dataHora(t.getHorario()), descricao, valor, Formatos.moeda(saldoCorrente)));
            }

            linha(w, SIMPLES);
            linha(w, String.format(" Total de movimentações: %d", transacoes.size()));
            linha(w, " Saldo atual: " + Formatos.moeda(conta.getSaldo()));
            if (saldoCorrente.compareTo(conta.getSaldo()) != 0) {
                linha(w, " (!) O histórico acima não fecha com o saldo atual: extrato incompleto.");
            }
            linha(w, DUPLA);
        }
    }

    private static void linha(BufferedWriter w, String texto) throws IOException {
        w.write(texto);
        w.newLine();
    }
}
