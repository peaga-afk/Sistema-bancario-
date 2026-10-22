package ui;

import domain.Conta;
import domain.Transacao;
import io.Formatos;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.List;

/** Tela principal da conta: saldo, ações rápidas e últimas movimentações. */
public class TelaInicio extends JPanel {

    private final JanelaPrincipal janela;
    private long numero;

    private final JLabel saudacao = rotulo(java.awt.Font.BOLD, 20f);
    private final JLabel contaLabel = rotulo(java.awt.Font.PLAIN, 13f);
    private final JLabel saldoLabel = rotulo(java.awt.Font.BOLD, 36f);
    private final BotaoOlho olho = new BotaoOlho();
    private final JPanel avatarSlot = new JPanel(new BorderLayout());
    private final JPanel movimentos = new JPanel();

    public TelaInicio(JanelaPrincipal janela) {
        super(new BorderLayout());
        this.janela = janela;
        setBackground(Tema.FUNDO);

        add(montarCabecalho(), BorderLayout.NORTH);

        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setBorder(BorderFactory.createEmptyBorder(22, 22, 12, 22));

        JPanel acoes = new JPanel(new GridLayout(1, 4, 4, 0));
        acoes.setOpaque(false);
        acoes.add(acao("Depositar", BotaoAcao.Icone.MAIS, () -> operacao(DialogoOperacao.Tipo.DEPOSITO)));
        acoes.add(acao("Sacar", BotaoAcao.Icone.MENOS, () -> operacao(DialogoOperacao.Tipo.SAQUE)));
        acoes.add(acao("Transferir", BotaoAcao.Icone.TRANSFERIR, () -> operacao(DialogoOperacao.Tipo.TRANSFERENCIA)));
        acoes.add(acao("Extrato", BotaoAcao.Icone.EXTRATO, () -> janela.abrirExtrato(numero)));
        corpo.add(Tema.larguraTotal(acoes));
        corpo.add(Box.createVerticalStrut(22));

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        JLabel t = new JLabel("Últimas movimentações");
        t.setFont(Tema.fonte(java.awt.Font.BOLD, 17f));
        t.setForeground(Tema.BRANCO);
        BotaoApp verTudo = new BotaoApp("Ver tudo", BotaoApp.Estilo.FANTASMA);
        verTudo.setFont(Tema.fonte(java.awt.Font.BOLD, 13f));
        verTudo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        verTudo.addActionListener(e -> janela.abrirExtrato(numero));
        titulo.add(t, BorderLayout.WEST);
        titulo.add(verTudo, BorderLayout.EAST);
        corpo.add(Tema.larguraTotal(titulo));
        corpo.add(Box.createVerticalStrut(10));

        movimentos.setOpaque(false);
        movimentos.setLayout(new BoxLayout(movimentos, BoxLayout.Y_AXIS));
        PainelArredondado cartao = new PainelArredondado(Tema.SUPERFICIE, 22, new BorderLayout());
        cartao.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        cartao.add(movimentos, BorderLayout.CENTER);
        cartao.setAlignmentX(LEFT_ALIGNMENT);
        corpo.add(cartao);

        PainelRolavel envoltorio = new PainelRolavel();
        envoltorio.add(corpo, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(envoltorio);
        Tema.estilizar(rolagem);
        add(rolagem, BorderLayout.CENTER);
    }

    private JPanel montarCabecalho() {
        JPanel cab = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.qualidade(g2);
                g2.setPaint(new GradientPaint(0, 0, new java.awt.Color(0x9A10D4), 0, getHeight(), Tema.ROXO_ESCURO));
                g2.fillRoundRect(0, -40, getWidth(), getHeight() + 40, 44, 44);
                g2.dispose();
            }
        };
        cab.setOpaque(false);
        cab.setBorder(BorderFactory.createEmptyBorder(26, 24, 24, 20));

        JPanel linha = new JPanel(new BorderLayout());
        linha.setOpaque(false);
        JPanel esquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        esquerda.setOpaque(false);
        avatarSlot.setOpaque(false);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        contaLabel.setForeground(Tema.comAlfa(Tema.BRANCO, 200));
        textos.add(saudacao);
        textos.add(contaLabel);
        esquerda.add(avatarSlot);
        esquerda.add(textos);
        BotaoApp trocar = new BotaoApp("Trocar", BotaoApp.Estilo.FANTASMA);
        trocar.setForeground(Tema.BRANCO);
        trocar.setFont(Tema.fonte(java.awt.Font.BOLD, 14f));
        trocar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        trocar.addActionListener(e -> janela.abrirContas());
        linha.add(esquerda, BorderLayout.WEST);
        linha.add(trocar, BorderLayout.EAST);

        JPanel saldoBox = new JPanel();
        saldoBox.setOpaque(false);
        saldoBox.setLayout(new BoxLayout(saldoBox, BoxLayout.Y_AXIS));
        JLabel disp = rotulo(java.awt.Font.PLAIN, 14f);
        disp.setForeground(Tema.comAlfa(Tema.BRANCO, 210));
        disp.setText("Saldo disponível");
        JPanel linhaSaldo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linhaSaldo.setOpaque(false);
        linhaSaldo.add(saldoLabel);
        linhaSaldo.add(olho);
        olho.setAoAlternar(this::atualizarSaldo);
        disp.setAlignmentX(LEFT_ALIGNMENT);
        linhaSaldo.setAlignmentX(LEFT_ALIGNMENT);
        saldoBox.add(disp);
        saldoBox.add(linhaSaldo);

        cab.add(linha, BorderLayout.NORTH);
        cab.add(saldoBox, BorderLayout.CENTER);
        return cab;
    }

    private static JLabel rotulo(int estilo, float tamanho) {
        JLabel l = new JLabel();
        l.setFont(Tema.fonte(estilo, tamanho));
        l.setForeground(Tema.BRANCO);
        return l;
    }

    private BotaoAcao acao(String texto, BotaoAcao.Icone icone, Runnable r) {
        BotaoAcao b = new BotaoAcao(texto, icone);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void operacao(DialogoOperacao.Tipo tipo) {
        new DialogoOperacao(janela, tipo, numero, this::atualizar).abrir(janela);
    }

    public void mostrar(long numeroConta) {
        this.numero = numeroConta;
        Conta c = janela.banco().buscarConta(numeroConta);
        avatarSlot.removeAll();
        avatarSlot.add(new Avatar(c.getTitular().getNome(), Tema.comAlfa(Tema.BRANCO, 60), 46));
        saudacao.setText("Olá, " + c.getTitular().getNome().split("\\s+")[0]);
        contaLabel.setText("Conta " + c.getNumero() + " · CPF " + Formatos.cpf(c.getTitular().getCpf()));
        atualizar();
    }

    public void atualizar() {
        atualizarSaldo();
        movimentos.removeAll();
        List<Transacao> todas = janela.banco().extrato(numero);
        if (todas.isEmpty()) {
            JLabel vazio = new JLabel("Sem movimentações ainda");
            vazio.setForeground(Tema.CINZA);
            vazio.setFont(Tema.fonte(java.awt.Font.PLAIN, 14f));
            vazio.setBorder(BorderFactory.createEmptyBorder(22, 0, 22, 0));
            movimentos.add(vazio);
        } else {
            int mostradas = 0;
            for (int i = todas.size() - 1; i >= 0 && mostradas < 5; i--, mostradas++) {
                if (mostradas > 0) {
                    movimentos.add(separador());
                }
                LinhaTransacao linha = new LinhaTransacao(todas.get(i), janela.banco());
                linha.setAlignmentX(LEFT_ALIGNMENT);
                movimentos.add(linha);
            }
        }
        movimentos.revalidate();
        movimentos.repaint();
    }

    private void atualizarSaldo() {
        Conta c = janela.banco().buscarConta(numero);
        saldoLabel.setText(olho.isAberto() ? Formatos.moeda(c.getSaldo()) : "R$ ••••••");
    }

    private static JPanel separador() {
        JPanel s = new JPanel();
        s.setBackground(Tema.SUPERFICIE_ALTA);
        s.setPreferredSize(new java.awt.Dimension(1, 1));
        s.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(LEFT_ALIGNMENT);
        return s;
    }
}
