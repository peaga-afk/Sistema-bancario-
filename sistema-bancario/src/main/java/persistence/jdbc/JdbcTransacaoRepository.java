package persistence.jdbc;

import domain.TipoTransacao;
import domain.Transacao;
import persistence.TransacaoRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Persistência das transações via JDBC. É um dos únicos lugares onde existe SQL. */
public class JdbcTransacaoRepository implements TransacaoRepository {

    private final JdbcSessao sessao;

    public JdbcTransacaoRepository(JdbcSessao sessao) {
        this.sessao = sessao;
    }

    @Override
    public Transacao salvar(Transacao t) {
        return sessao.comConexao(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO transacoes (numero_conta, tipo, valor, horario, conta_contraparte) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, t.getNumeroConta());
                ps.setString(2, t.getTipo().name());
                ps.setBigDecimal(3, t.getValor());
                ps.setTimestamp(4, Timestamp.valueOf(t.getHorario()));
                if (t.getContaContraparte() == null) {
                    ps.setNull(5, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(5, t.getContaContraparte());
                }
                ps.executeUpdate();
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    chaves.next();
                    return new Transacao(chaves.getLong(1), t.getNumeroConta(), t.getTipo(), t.getValor(),
                            t.getHorario(), t.getContaContraparte());
                }
            }
        });
    }

    @Override
    public List<Transacao> buscarPorConta(long numeroConta) {
        return sessao.comConexao(c -> {
            List<Transacao> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, numero_conta, tipo, valor, horario, conta_contraparte "
                            + "FROM transacoes WHERE numero_conta = ? ORDER BY id")) {
                ps.setLong(1, numeroConta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long contraparte = rs.getLong("conta_contraparte");
                        Long contraparteOuNula = rs.wasNull() ? null : contraparte;
                        lista.add(new Transacao(rs.getLong("id"), rs.getLong("numero_conta"),
                                TipoTransacao.valueOf(rs.getString("tipo")), rs.getBigDecimal("valor"),
                                rs.getTimestamp("horario").toLocalDateTime(), contraparteOuNula));
                    }
                }
            }
            return lista;
        });
    }
}
