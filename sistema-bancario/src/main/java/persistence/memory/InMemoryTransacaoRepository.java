package persistence.memory;

import domain.Transacao;
import persistence.TransacaoRepository;

import java.util.ArrayList;
import java.util.List;

/** Implementação em memória do repositório de transações (para testes). */
public class InMemoryTransacaoRepository implements TransacaoRepository {

    private final List<Transacao> todas = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public synchronized Transacao salvar(Transacao t) {
        Transacao salva = new Transacao(proximoId++, t.getNumeroConta(), t.getTipo(), t.getValor(),
                t.getHorario(), t.getContaContraparte());
        todas.add(salva);
        return salva;
    }

    @Override
    public synchronized List<Transacao> buscarPorConta(long numeroConta) {
        List<Transacao> resultado = new ArrayList<>();
        for (Transacao t : todas) {
            if (t.getNumeroConta() == numeroConta) {
                resultado.add(t);
            }
        }
        return resultado;
    }
}
