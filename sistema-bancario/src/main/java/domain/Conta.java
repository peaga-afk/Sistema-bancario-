package domain;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Conta bancária. É o ÚNICO lugar do sistema onde o saldo pode ser alterado.
 *
 * ============================================================
 * ESTADO ATUAL: SEMANA 4 — SINCRONIZADA + TRANSFERÊNCIA SEM DEADLOCK
 * ============================================================
 * Todo acesso ao saldo (leitura e escrita) passa por 'synchronized' no monitor
 * (lock) do próprio objeto. Em Java, enquanto uma thread está dentro de um
 * método/bloco synchronized de uma conta, as outras que tentarem entrar em
 * QUALQUER trecho synchronized da MESMA conta esperam. Assim "ler + somar +
 * escrever" é indivisível e nenhuma atualização se perde.
 *
 * TRANSFERÊNCIA: mexe em duas contas, então precisa de dois locks. Se a
 * thread 1 transfere A->B (trava A, depois B) e a thread 2 transfere B->A
 * (trava B, depois A) ao mesmo tempo, cada uma segura um lock e espera o
 * outro para sempre: DEADLOCK. A solução é uma ORDEM GLOBAL de bloqueio:
 * qualquer transferência trava sempre a conta de MENOR número primeiro,
 * não importa o sentido. Duas threads nunca ficam esperando em círculo.
 *
 * Versões anteriores guardadas em docs/ (Semana 2: sem sincronização).
 *
 * Dinheiro é sempre BigDecimal com 2 casas decimais (nunca double).
 */
public class Conta {

    /**
     * Ordem global de bloqueio das contas (menor número primeiro). Todo código
     * que precisar travar DUAS contas deve usar esta mesma ordem.
     */
    public static final Comparator<Conta> ORDEM_DE_BLOQUEIO = Comparator.comparingLong(Conta::getNumero);

    private final long numero;
    private final Cliente titular;
    private BigDecimal saldo; // só é lido/escrito dentro de métodos synchronized

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

    /**
     * Também é synchronized: além de impedir leitura no meio de uma escrita,
     * garante VISIBILIDADE — sem isso, uma thread poderia enxergar um valor
     * antigo do saldo guardado em cache, mesmo depois de outra thread alterá-lo.
     */
    public synchronized BigDecimal getSaldo() {
        return saldo;
    }

    public synchronized void depositar(BigDecimal valor) {
        BigDecimal v = valorPositivo(valor);
        saldo = saldo.add(v); // ler + somar + escrever: agora indivisível
    }

    public synchronized void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        BigDecimal v = valorPositivo(valor);
        // check-then-act: a verificação e a escrita estão no MESMO método
        // synchronized, então nenhuma outra thread pode mexer no saldo entre
        // as duas. Sem isso, duas threads passariam na verificação juntas e o
        // saldo ficaria negativo.
        if (saldo.compareTo(v) < 0) {
            throw new SaldoInsuficienteException(numero, saldo, v);
        }
        saldo = saldo.subtract(v);
    }

    /**
     * Transfere 'valor' desta conta para 'destino' de forma ATÔMICA: enquanto a
     * transferência acontece, ninguém consegue operar nas duas contas, então
     * ninguém enxerga o dinheiro "no ar" (já saiu de uma, ainda não entrou na
     * outra). Os locks são tomados SEMPRE na mesma ordem para evitar deadlock.
     */
    public void transferirPara(Conta destino, BigDecimal valor) throws SaldoInsuficienteException {
        if (destino == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória");
        }
        if (destino == this || destino.numero == this.numero) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta");
        }
        BigDecimal v = valorPositivo(valor);

        Conta primeira = ORDEM_DE_BLOQUEIO.compare(this, destino) < 0 ? this : destino;
        Conta segunda = (primeira == this) ? destino : this;

        synchronized (primeira) {
            synchronized (segunda) {
                this.sacar(v);        // se faltar saldo, lança aqui e nada foi alterado
                destino.depositar(v); // v já é válido, então não falha
            }
        }
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
        return "Conta " + numero + " [" + titular.getNome() + "] saldo R$ " + getSaldo().toPlainString();
    }
}
