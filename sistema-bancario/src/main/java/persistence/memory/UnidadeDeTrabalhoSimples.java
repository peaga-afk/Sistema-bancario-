package persistence.memory;

import persistence.UnidadeDeTrabalho;

/** Sem banco não há transação a confirmar: apenas executa a ação. */
public class UnidadeDeTrabalhoSimples implements UnidadeDeTrabalho {

    @Override
    public void executar(Runnable acao) {
        acao.run();
    }
}
