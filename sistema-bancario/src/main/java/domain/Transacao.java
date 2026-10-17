package domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro IMUTÁVEL de uma operação já realizada em uma conta.
 * Todos os campos são final e não há setters, então a instância pode ser
 * compartilhada entre threads sem sincronização.
 *
 * Não sabe se salvar: quem persiste é um Repository (semana 5).
 */
public final class Transacao {

    private final Long id;                 // null até ser persistida
    private final long numeroConta;
    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final LocalDateTime horario;
    private final Long contaContraparte;   // só para transferências

    public Transacao(Long id, long numeroConta, TipoTransacao tipo, BigDecimal valor,
                     LocalDateTime horario, Long contaContraparte) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo da transação é obrigatório");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new ValorInvalidoException("Valor da transação deve ser positivo");
        }
        if (horario == null) {
            throw new IllegalArgumentException("Horário da transação é obrigatório");
        }
        boolean transferencia = tipo == TipoTransacao.TRANSFERENCIA_ENVIADA
                || tipo == TipoTransacao.TRANSFERENCIA_RECEBIDA;
        if (transferencia && contaContraparte == null) {
            throw new IllegalArgumentException("Transferência exige a conta contraparte");
        }
        this.id = id;
        this.numeroConta = numeroConta;
        this.tipo = tipo;
        this.valor = valor;
        this.horario = horario;
        this.contaContraparte = contaContraparte;
    }

    /** Cria uma transação nova, ainda sem id, com o horário de agora. */
    public static Transacao nova(long numeroConta, TipoTransacao tipo, BigDecimal valor, Long contaContraparte) {
        return new Transacao(null, numeroConta, tipo, valor, LocalDateTime.now(), contaContraparte);
    }

    public Long getId() {
        return id;
    }

    public long getNumeroConta() {
        return numeroConta;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public Long getContaContraparte() {
        return contaContraparte;
    }

    @Override
    public String toString() {
        String base = horario + " | conta " + numeroConta + " | " + tipo + " | R$ " + valor.toPlainString();
        return contaContraparte == null ? base : base + " | contraparte " + contaContraparte;
    }
}
