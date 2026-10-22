package persistence.jdbc;

import persistence.PersistenciaException;
import persistence.UnidadeDeTrabalho;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Entrega a conexão certa para os repositórios.
 *
 * - Fora de uma unidade de trabalho: cada operação abre e fecha a sua própria
 *   conexão (nunca se compartilha uma Connection entre threads).
 * - Dentro de executar(...): todas as operações DA MESMA THREAD usam a mesma
 *   conexão, com autocommit desligado, e o resultado é commit ou rollback.
 */
public class JdbcSessao implements UnidadeDeTrabalho {

    @FunctionalInterface
    public interface OperacaoSql<T> {
        T aplicar(Connection conexao) throws SQLException;
    }

    private final ConnectionFactory fabrica;
    private final ThreadLocal<Connection> daThread = new ThreadLocal<>();

    public JdbcSessao(ConnectionFactory fabrica) {
        this.fabrica = fabrica;
    }

    public <T> T comConexao(OperacaoSql<T> operacao) {
        Connection emTransacao = daThread.get();
        try {
            if (emTransacao != null) {
                return operacao.aplicar(emTransacao);
            }
            try (Connection c = fabrica.abrir()) {
                return operacao.aplicar(c);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro de banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    public void executar(Runnable acao) {
        if (daThread.get() != null) {   // já dentro de uma transação: só participa dela
            acao.run();
            return;
        }
        try (Connection c = fabrica.abrir()) {
            c.setAutoCommit(false);
            daThread.set(c);
            try {
                acao.run();
                c.commit();
            } catch (RuntimeException | Error e) {
                c.rollback();           // desfaz TUDO que a ação gravou
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro de banco de dados: " + e.getMessage(), e);
        } finally {
            daThread.remove();
        }
    }
}
