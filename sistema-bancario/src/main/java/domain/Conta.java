package domain;

import java.math.BigDecimal;

/**
 * Conta bancária. É o ÚNICO lugar do sistema onde o saldo pode ser alterado.
 *
 * ============================================================
 * ESTADO ATUAL: SEMANA 2 — SEM SINCRONIZAÇÃO (de propósito!)
 * ============================================================
 * Os métodos abaixo estão corretos para uso com UMA thread, mas NÃO são
 * seguros para várias threads: dois depositar() simultâneos podem ler o mesmo
 * saldo e um deles "se perde" (lost update). Isso é o que a simulação da
 * semana 2 expõe.
 *
 * Na SEMANA 3, os pontos marcados com "TODO SEMANA 3" recebem 'synchronized'.
 * Na SEMANA 4, transferirPara() ganha a ordem consistente de bloqueio.
 *
 * Dinheiro é sempre BigDecimal com 2 casas decimais (nunca double).
 */
public class Conta {

    private final long numero;
    private final Cliente titular;
    private BigDecimal saldo;

    public Conta(long numero, Cliente titular, BigDecimal saldoInicial) {
        if (titular == null) {
            throw new IllegalArgumentException("Titular da conta é obrigatório");
        }
        BigDecimal normalizado = normalizar(saldoInicial);
        if (normalizado.signum() < 0) {
            throw new ValorInvalidoException("Saldo inicial não pode ser negativo");
        }
        this.numero = numero;
        this.titular = titular;
        this.saldo = normalizado;
    }

    public long getNumero() {
        return numero;
    }

    public Cliente getTitular() {
        return titular;
    }

    // TODO SEMANA 3: synchronized (também garante visibilidade do valor entre threads)
    public BigDecimal getSaldo() {
        return saldo;
    }

    // TODO SEMANA 3: synchronized
    public void depositar(BigDecimal valor) {
        BigDecimal v = valorPositivo(valor);
        // Leitura + soma + escrita: três passos que NÃO são atômicos.
        saldo = saldo.add(v);
    }

    // TODO SEMANA 3: synchronized
    public void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        BigDecimal v = valorPositivo(valor);
        // check-then-act: a verificação e a escrita precisam ficar no MESMO
        // bloco sincronizado, senão duas threads passam na verificação juntas.
        if (saldo.compareTo(v) < 0) {
            throw new SaldoInsuficienteException(numero, saldo, v);
        }
        saldo = saldo.subtract(v);
    }

    // TODO SEMANA 4: bloquear as duas contas SEMPRE na mesma ordem (ex.: menor
    // número primeiro) para evitar deadlock em transferências cruzadas.
    public void transferirPara(Conta destino, BigDecimal valor) throws SaldoInsuficienteException {
        if (destino == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória");
        }
        if (destino == this || destino.numero == this.numero) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta");
        }
        BigDecimal v = valorPositivo(valor);
        this.sacar(v);        // se faltar saldo, lança aqui e nada foi alterado
        destino.depositar(v); // v já é válido, então não falha
    }

    /** Valida e normaliza para 2 casas. Rejeita nulo, zero e negativo. */
    private static BigDecimal valorPositivo(BigDecimal valor) {
        BigDecimal v = normalizar(valor);
        if (v.signum() <= 0) {
            throw new ValorInvalidoException("Valor deve ser positivo");
        }
        return v;
    }

    /** Rejeita nulo e valores com mais de 2 casas; devolve com escala 2. */
    private static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            throw new ValorInvalidoException("Valor é obrigatório");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new ValorInvalidoException("Valor deve ter no máximo 2 casas decimais");
        }
        return valor.setScale(2);
    }

    @Override
    public String toString() {
        return "Conta " + numero + " [" + titular.getNome() + "] saldo R$ " + saldo.toPlainString();
    }
}
