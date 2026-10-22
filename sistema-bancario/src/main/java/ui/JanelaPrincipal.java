package ui;

import service.BancoService;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/** Janela do app (formato de celular). Troca entre as telas com um CardLayout. */
public class JanelaPrincipal extends JFrame {

    private static final String CONTAS = "contas";
    private static final String INICIO = "inicio";
    private static final String EXTRATO = "extrato";
    private static final String SIMULACAO = "simulacao";

    private final BancoService banco;
    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = new JPanel(cartas);
    private final Aviso aviso = new Aviso();

    private final TelaContas telaContas;
    private final TelaInicio telaInicio;
    private final TelaExtrato telaExtrato;
    private final TelaSimulacao telaSimulacao;

    public JanelaPrincipal(BancoService banco) {
        super("ThreadBank");
        this.banco = banco;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FUNDO);
        conteudo.setBackground(Tema.FUNDO);

        telaContas = new TelaContas(this);
        telaInicio = new TelaInicio(this);
        telaExtrato = new TelaExtrato(this);
        telaSimulacao = new TelaSimulacao(this);
        conteudo.add(telaContas, CONTAS);
        conteudo.add(telaInicio, INICIO);
        conteudo.add(telaExtrato, EXTRATO);
        conteudo.add(telaSimulacao, SIMULACAO);
        setContentPane(conteudo);
        setGlassPane(aviso);
        aviso.setVisible(true);

        setSize(430, 820);
        setMinimumSize(new Dimension(400, 640));
        setLocationRelativeTo(null);
        abrirContas();
    }

    public BancoService banco() {
        return banco;
    }

    public void abrirContas() {
        telaContas.atualizar();
        cartas.show(conteudo, CONTAS);
    }

    public void abrirInicio(long numeroConta) {
        telaInicio.mostrar(numeroConta);
        cartas.show(conteudo, INICIO);
    }

    public void abrirExtrato(long numeroConta) {
        telaExtrato.mostrar(numeroConta);
        cartas.show(conteudo, EXTRATO);
    }

    public void abrirSimulacao() {
        telaSimulacao.preparar();
        cartas.show(conteudo, SIMULACAO);
    }

    /** Mostra uma mensagem rápida na parte de baixo da janela. */
    public void aviso(String mensagem, boolean erro) {
        aviso.mostrar(mensagem, erro);
    }
}
