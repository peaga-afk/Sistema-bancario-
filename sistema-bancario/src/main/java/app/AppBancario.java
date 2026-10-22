package app;

import domain.Cliente;
import domain.Conta;
import domain.SaldoInsuficienteException;
import persistence.jdbc.ConnectionFactory;
import persistence.jdbc.JdbcContaRepository;
import persistence.jdbc.JdbcSessao;
import persistence.jdbc.JdbcTransacaoRepository;
import service.BancoService;
import ui.JanelaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.math.BigDecimal;

/**
 * Ponto de entrada do app com interface gráfica (ThreadBank).
 * Os dados ficam no arquivo ./data/threadbank.mv.db: ao abrir de novo, as contas
 * e o extrato continuam lá. Para "zerar" tudo, apague a pasta data/.
 */
public class AppBancario {

    public static void main(String[] args) {
        ConnectionFactory fabrica = ConnectionFactory.h2Arquivo("./data/threadbank");
        fabrica.criarTabelas();
        JdbcSessao sessao = new JdbcSessao(fabrica);
        BancoService banco = new BancoService(new JdbcContaRepository(sessao),
                new JdbcTransacaoRepository(sessao), sessao);

        if (banco.listarContas().isEmpty()) {
            popularExemplo(banco);
        }
        SwingUtilities.invokeLater(() -> criarJanela(banco).setVisible(true));
    }

    public static JanelaPrincipal criarJanela(BancoService banco) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {
            // segue com o visual padrão
        }
        return new JanelaPrincipal(banco);
    }

    /** Contas de exemplo para a primeira execução (só se o banco estiver vazio). */
    public static void popularExemplo(BancoService banco) {
        try {
            Conta ana = banco.criarConta(new Cliente("Ana Souza", "529.982.247-25"), new BigDecimal("5000.00"));
            Conta bruno = banco.criarConta(new Cliente("Bruno Lima", "111.444.777-35"), new BigDecimal("3000.00"));
            banco.criarConta(new Cliente("Carla Mendes", "390.533.447-05"), new BigDecimal("1500.00"));
            banco.depositar(ana.getNumero(), new BigDecimal("350.00"));
            banco.sacar(ana.getNumero(), new BigDecimal("120.00"));
            banco.transferir(ana.getNumero(), bruno.getNumero(), new BigDecimal("200.00"));
            banco.transferir(bruno.getNumero(), ana.getNumero(), new BigDecimal("75.50"));
        } catch (SaldoInsuficienteException e) {
            throw new IllegalStateException("Dados de exemplo inválidos", e);
        }
    }
}
