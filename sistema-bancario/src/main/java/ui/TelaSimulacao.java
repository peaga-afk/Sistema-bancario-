package ui;

import io.Formatos;
import simulacao.SimulacaoBancaria;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

/**
 * Simulação concorrente na tela: várias threads mexem nas contas reais ao mesmo
 * tempo. No fim, o saldo de cada conta precisa bater com o esperado.
 * A simulação roda em uma thread de fundo (SwingWorker) para a janela não travar.
 */
public class TelaSimulacao extends JPanel {

    private final JanelaPrincipal janela;
    private final CampoApp campoThreads = new CampoApp("Threads");
    private final CampoApp campoOperacoes = new CampoApp("Operações por thread");
    private final BotaoApp iniciar = new BotaoApp("Iniciar simulação", BotaoApp.Estilo.PRIMARIO);
    private final BarraProgresso barra = new BarraProgresso();
    private final JLabel status = new JLabel(" ");
    private final JPanel resultado = new JPanel();
    private Timer relogio;

    public TelaSimulacao(JanelaPrincipal janela) {
        super(new BorderLayout());
        this.janela = janela;
        setBackground(Tema.FUNDO);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setBorder(BorderFactory.createEmptyBorder(22, 14, 6, 22));
        BotaoApp voltar = new BotaoApp("‹  Voltar", BotaoApp.Estilo.FANTASMA);
        voltar.addActionListener(e -> janela.abrirContas());
        JLabel titulo = new JLabel("Simulação");
        titulo.setFont(Tema.fonte(java.awt.Font.BOLD, 20f));
        titulo.setForeground(Tema.BRANCO);
        titulo.setHorizontalAlignment(JLabel.CENTER);
        topo.add(voltar, BorderLayout.WEST);
        topo.add(titulo, BorderLayout.CENTER);
        topo.add(Box.createHorizontalStrut(90), BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setBorder(BorderFactory.createEmptyBorder(6, 22, 16, 22));

        JLabel explicacao = new JLabel(Tema.html("Várias threads fazem depósitos, "
                + "saques e transferências ao mesmo tempo, nas contas reais do banco. No final, o saldo de cada "
                + "conta precisa bater centavo por centavo com o esperado: na memória, no banco de dados e no "
                + "extrato.", 345));
        explicacao.setForeground(Tema.CINZA);
        explicacao.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        explicacao.setBorder(BorderFactory.createEmptyBorder(0, 2, 12, 2));
        corpo.add(Tema.larguraTotal(explicacao));

        campoThreads.setText("4");
        campoOperacoes.setText("100");
        JPanel campos = new JPanel(new GridLayout(2, 2, 10, 4));
        campos.setOpaque(false);
        campos.add(legenda("Threads (1 a 16)"));
        campos.add(legenda("Operações por thread"));
        campos.add(campoThreads);
        campos.add(campoOperacoes);
        corpo.add(Tema.larguraTotal(campos));
        corpo.add(Box.createVerticalStrut(14));

        iniciar.addActionListener(e -> iniciar());
        corpo.add(Tema.larguraTotal(iniciar));
        corpo.add(Box.createVerticalStrut(14));
        barra.setAlignmentX(LEFT_ALIGNMENT);
        corpo.add(barra);
        status.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        status.setForeground(Tema.CINZA);
        status.setBorder(BorderFactory.createEmptyBorder(6, 2, 8, 0));
        status.setAlignmentX(LEFT_ALIGNMENT);
        corpo.add(status);

        resultado.setOpaque(false);
        resultado.setLayout(new BoxLayout(resultado, BoxLayout.Y_AXIS));
        resultado.setAlignmentX(LEFT_ALIGNMENT);
        corpo.add(resultado);

        PainelRolavel envoltorio = new PainelRolavel();
        envoltorio.add(corpo, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(envoltorio);
        Tema.estilizar(rolagem);
        add(rolagem, BorderLayout.CENTER);
    }

    private static JLabel legenda(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fonte(java.awt.Font.PLAIN, 12f));
        l.setForeground(Tema.CINZA);
        return l;
    }

    public void preparar() {
        resultado.removeAll();
        barra.setFracao(0);
        status.setText(janela.banco().listarContas().size() < 2
                ? "Abra pelo menos 2 contas para simular." : "Pronta para começar.");
        iniciar.setEnabled(janela.banco().listarContas().size() >= 2);
        resultado.revalidate();
        repaint();
    }

    private void iniciar() {
        int threads;
        int operacoes;
        try {
            threads = Integer.parseInt(campoThreads.getText().trim());
            operacoes = Integer.parseInt(campoOperacoes.getText().trim());
        } catch (NumberFormatException e) {
            janela.aviso("Digite números inteiros", true);
            return;
        }
        if (threads < 1 || threads > 16 || operacoes < 1 || operacoes > 2000) {
            janela.aviso("Use de 1 a 16 threads e de 1 a 2000 operações", true);
            return;
        }

        SimulacaoBancaria sim = new SimulacaoBancaria(janela.banco(), threads, operacoes);
        iniciar.setEnabled(false);
        resultado.removeAll();
        resultado.revalidate();
        status.setText("Executando " + sim.totalDeOperacoes() + " operações em " + threads + " threads...");

        relogio = new Timer(80, e -> barra.setFracao((double) sim.progresso() / sim.totalDeOperacoes()));
        relogio.start();

        new SwingWorker<SimulacaoBancaria.Resultado, Void>() {
            @Override
            protected SimulacaoBancaria.Resultado doInBackground() throws Exception {
                return sim.executar();
            }

            @Override
            protected void done() {
                relogio.stop();
                iniciar.setEnabled(true);
                try {
                    barra.setFracao(1);
                    mostrar(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    status.setText("Erro: " + ex.getCause().getMessage());
                    janela.aviso("A simulação falhou", true);
                }
            }
        }.execute();
    }

    private void mostrar(SimulacaoBancaria.Resultado r) {
        status.setText(Tema.html(r.operacoesRealizadas() + " operações realizadas · "
                + r.operacoesRecusadas() + " recusadas (saldo insuficiente) · " + r.tempoMs() + " ms", 345));
        resultado.removeAll();

        boolean ok = r.consistente();
        PainelArredondado resumo = new PainelArredondado(ok ? Tema.ROXO : new java.awt.Color(0x8E1B33), 22,
                new BorderLayout());
        resumo.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel a = new JLabel(ok ? "Tudo certo!" : "Inconsistência detectada!");
        a.setFont(Tema.fonte(java.awt.Font.BOLD, 20f));
        a.setForeground(Tema.BRANCO);
        JLabel b = new JLabel(Tema.html(ok
                ? "Nenhum centavo se perdeu, mesmo com " + r.threads() + " threads disputando as mesmas contas."
                : "Algum saldo não bate com o esperado. Revise a sincronização.", 320));
        b.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        b.setForeground(Tema.comAlfa(Tema.BRANCO, 220));
        resumo.add(a, BorderLayout.NORTH);
        resumo.add(b, BorderLayout.CENTER);
        resultado.add(Tema.larguraTotal(resumo));
        resultado.add(Box.createVerticalStrut(12));

        for (SimulacaoBancaria.ResultadoConta c : r.contas()) {
            resultado.add(Tema.larguraTotal(cartaoConta(c)));
            resultado.add(Box.createVerticalStrut(10));
        }
        resultado.revalidate();
        resultado.repaint();
    }

    private PainelArredondado cartaoConta(SimulacaoBancaria.ResultadoConta c) {
        PainelArredondado p = new PainelArredondado(Tema.SUPERFICIE, 20, new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JLabel nome = new JLabel(c.titular() + " · " + c.numero());
        nome.setFont(Tema.fonte(java.awt.Font.BOLD, 15f));
        nome.setForeground(Tema.BRANCO);
        JLabel sel = new JLabel(c.consistente() ? "OK" : "ERRO");
        sel.setFont(Tema.fonte(java.awt.Font.BOLD, 13f));
        sel.setForeground(c.consistente() ? Tema.ROXO_CLARO : Tema.VERMELHO);
        cab.add(nome, BorderLayout.WEST);
        cab.add(sel, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);

        JPanel linhas = new JPanel(new GridLayout(4, 1, 0, 3));
        linhas.setOpaque(false);
        linhas.add(linha("Esperado", c.esperado(), c.esperado()));
        linhas.add(linha("Em memória", c.emMemoria(), c.esperado()));
        linhas.add(linha("No banco", c.noBanco(), c.esperado()));
        linhas.add(linha("Pelo extrato", c.peloExtrato(), c.esperado()));
        p.add(linhas, BorderLayout.CENTER);
        return p;
    }

    private static JPanel linha(String rotulo, BigDecimal valor, BigDecimal esperado) {
        JPanel l = new JPanel(new BorderLayout());
        l.setOpaque(false);
        JLabel a = new JLabel(rotulo);
        a.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        a.setForeground(Tema.CINZA);
        JLabel b = new JLabel(Formatos.moeda(valor));
        b.setFont(Tema.fonte(java.awt.Font.BOLD, 13f));
        b.setForeground(valor.compareTo(esperado) == 0 ? Tema.BRANCO : Tema.VERMELHO);
        l.add(a, BorderLayout.WEST);
        l.add(b, BorderLayout.EAST);
        return l;
    }
}
