package io;

import domain.TipoTransacao;
import domain.ValorInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação e leitura de valores no padrão brasileiro. */
public final class Formatos {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATA_HORA_CURTA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private Formatos() {
    }

    /** 1234.5 -> "R$ 1.234,50"; negativos -> "-R$ 1.234,50". */
    public static String moeda(BigDecimal valor) {
        BigDecimal v = valor.setScale(2, java.math.RoundingMode.HALF_EVEN);
        String sinal = v.signum() < 0 ? "-" : "";
        return sinal + "R$ " + String.format(PT_BR, "%,.2f", v.abs());
    }

    public static String dataHora(LocalDateTime t) {
        return t.format(DATA_HORA);
    }

    public static String dataHoraCurta(LocalDateTime t) {
        return t.format(DATA_HORA_CURTA);
    }

    /** "12345678909" -> "123.456.789-09". */
    public static String cpf(String onzeDigitos) {
        if (onzeDigitos == null || onzeDigitos.length() != 11) {
            return onzeDigitos;
        }
        return onzeDigitos.substring(0, 3) + "." + onzeDigitos.substring(3, 6) + "."
                + onzeDigitos.substring(6, 9) + "-" + onzeDigitos.substring(9);
    }

    public static String tipo(TipoTransacao tipo) {
        switch (tipo) {
            case DEPOSITO:
                return "Depósito";
            case SAQUE:
                return "Saque";
            case TRANSFERENCIA_ENVIADA:
                return "Transferência enviada";
            default:
                return "Transferência recebida";
        }
    }

    /**
     * Lê um valor digitado pelo usuário: aceita "1234,56", "1.234,56", "R$ 10", "10.5".
     * Se houver só um ponto e nenhuma vírgula, o ponto é tratado como separador decimal.
     */
    public static BigDecimal lerValor(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValorInvalidoException("Informe um valor");
        }
        String t = texto.replace("R$", "").replace(" ", "").trim();
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new ValorInvalidoException("Valor inválido: " + texto.trim());
        }
    }
}
