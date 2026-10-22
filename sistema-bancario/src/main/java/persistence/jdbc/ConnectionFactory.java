package persistence.jdbc;

import persistence.PersistenciaException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Cria conexões JDBC e prepara o banco. Não tem nenhuma regra bancária.
 * Usa o H2 (banco 100% Java, sem instalação). Para usar MySQL/PostgreSQL basta
 * trocar a URL e o driver; o script schema.sql é SQL padrão.
 */
public class ConnectionFactory {

    private final String url;
    private final String usuario;
    private final String senha;

    public ConnectionFactory(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    /** Banco em arquivo: os dados continuam existindo entre execuções. */
    public static ConnectionFactory h2Arquivo(String caminho) {
        return new ConnectionFactory("jdbc:h2:" + caminho, "sa", "");
    }

    /** Banco só em memória (some ao fechar a aplicação). Útil para testes. */
    public static ConnectionFactory h2Memoria() {
        return new ConnectionFactory("jdbc:h2:mem:banco_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Executa o script schema.sql (do classpath). Seguro de repetir: usa IF NOT EXISTS. */
    public void criarTabelas() {
        try (InputStream in = ConnectionFactory.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new PersistenciaException("schema.sql não encontrado no classpath", null);
            }
            StringBuilder sql = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = r.readLine()) != null) {
                    if (!linha.trim().startsWith("--")) {
                        sql.append(linha).append('\n');
                    }
                }
            }
            try (Connection c = abrir(); Statement st = c.createStatement()) {
                for (String comando : sql.toString().split(";")) {
                    if (!comando.isBlank()) {
                        st.execute(comando);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new PersistenciaException("Falha ao criar as tabelas", e);
        }
    }
}
