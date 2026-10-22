package persistence.jdbc;

import domain.Cliente;
import domain.Conta;
import persistence.ContaRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistência das contas via JDBC. É um dos únicos lugares onde existe SQL. */
public class JdbcContaRepository implements ContaRepository {

    private final JdbcSessao sessao;

    public JdbcContaRepository(JdbcSessao sessao) {
        this.sessao = sessao;
    }

    @Override
    public void salvar(Conta conta) {
        sessao.comConexao(c -> {
            int atualizadas;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE contas SET titular_nome = ?, titular_cpf = ?, saldo = ? WHERE numero = ?")) {
                ps.setString(1, conta.getTitular().getNome());
                ps.setString(2, conta.getTitular().getCpf());
                ps.setBigDecimal(3, conta.getSaldo());
                ps.setLong(4, conta.getNumero());
                atualizadas = ps.executeUpdate();
            }
            if (atualizadas == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO contas (numero, titular_nome, titular_cpf, saldo) VALUES (?, ?, ?, ?)")) {
                    ps.setLong(1, conta.getNumero());
                    ps.setString(2, conta.getTitular().getNome());
                    ps.setString(3, conta.getTitular().getCpf());
                    ps.setBigDecimal(4, conta.getSaldo());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public Optional<Conta> buscarPorNumero(long numero) {
        return sessao.comConexao(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT numero, titular_nome, titular_cpf, saldo FROM contas WHERE numero = ?")) {
                ps.setLong(1, numero);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(montar(rs)) : Optional.<Conta>empty();
                }
            }
        });
    }

    @Override
    public List<Conta> listarTodas() {
        return sessao.comConexao(c -> {
            List<Conta> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT numero, titular_nome, titular_cpf, saldo FROM contas ORDER BY numero");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public long proximoNumero() {
        return sessao.comConexao(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(numero), 1000) + 1 FROM contas");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    private static Conta montar(ResultSet rs) throws java.sql.SQLException {
        return new Conta(rs.getLong("numero"),
                new Cliente(rs.getString("titular_nome"), rs.getString("titular_cpf")),
                rs.getBigDecimal("saldo"));
    }
}
