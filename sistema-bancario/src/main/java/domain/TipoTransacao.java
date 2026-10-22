package domain;

/**
 * Tipos de operação registrados em uma {@link Transacao}.
 */
public enum TipoTransacao {
    DEPOSITO,
    SAQUE,
    TRANSFERENCIA_ENVIADA,
    TRANSFERENCIA_RECEBIDA
}
