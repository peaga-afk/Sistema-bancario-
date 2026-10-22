package persistence;

import domain.Transacao;

import java.util.List;

/** Contrato de persistência das transações (o extrato das contas). */
public interface TransacaoRepository {

    /** Grava a transação e devolve uma cópia já com o id gerado. */
    Transacao salvar(Transacao transacao);

    /** Transações da conta, da mais antiga para a mais recente. */
    List<Transacao> buscarPorConta(long numeroConta);
}
