package persistence;

/**
 * Agrupa várias gravações em UMA transação de banco: ou todas são confirmadas
 * (commit) ou nenhuma é (rollback). Usada na transferência, que grava duas
 * transações e atualiza dois saldos: não pode ficar "pela metade".
 */
public interface UnidadeDeTrabalho {

    /** Executa a ação; se ela lançar exceção, tudo o que foi gravado é desfeito. */
    void executar(Runnable acao);
}
