package ui;

import domain.Conta;
import domain.Transacao;
import io.Formatos;
import service.BancoService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/** Extrato completo da conta, com resumo e exportação para .txt. */
public class TelaExtrato extends JPanel {

    private final JanelaPrincipal janela;
    private long numero;
    private final JLabel titulo = new JLabel();
    private final JPanel resumo = new JPanel(new GridLayout(1, 3, 10, 0));
    private final JPanel lista = new JPanel();

    public TelaExtrato(JanelaPrincipal janela) {
        super(new BorderLayout());
        this.janela = janela;
        setBackground(Tema.FUNDO);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setBorder(BorderFactory.createEmptyBorder(22, 14, 8, 22));
        BotaoApp voltar = new BotaoApp("‹  Voltar", BotaoApp.Estilo.FANTASMA);
        voltar.addActionListener(e -> janela.abrirInicio(numero));
        titulo.setFont(Tema.fonte(java.awt.Font.BOLD, 20f));
        titulo.setForeground(Tema.BRANCO);
        topo.add(voltar, BorderLayout.WEST);
        topo.add(titulo, BorderLayout.CENTER);
        titulo.setHorizontalAlignment(JLabel.CENTER);
        topo.add(Box.createHorizontalStrut(90), BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        resumo.setOpaque(false);
        resumo.setBorder(BorderFactory.createEmptyBorder(6, 22, 12, 22));
        norte.add(resumo, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        PainelArredondado cartao = new PainelArredondado(Tema.SUPERFICIE, 22, new BorderLayout());
        cartao.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        cartao.add(lista, BorderLayout.CENTER);
        PainelRolavel envoltorio = new PainelRolavel();
        envoltorio.setBorder(BorderFactory.createEmptyBorder(0, 22, 8, 22));
        envoltorio.add(cartao, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(envoltorio);
        Tema.estilizar(rolagem);
        add(rolagem, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(8, 22, 24, 22));
        BotaoApp exportar = new BotaoApp("Exportar extrato (.txt)", BotaoApp.Estilo.PRIMARIO);
        exportar.addActionListener(e -> exportar());
        rodape.add(exportar, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    public void mostrar(long numeroConta) {
        this.numero = numeroConta;
        Conta c = janela.banco().buscarConta(numeroConta);
        titulo.setText("Extrato · " + c.getNumero());

        List<Transacao> todas = janela.banco().extrato(numeroConta);
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        for (Transacao t : todas) {
            if (BancoService.ehEntrada(t.getTipo())) {
                entradas = entradas.add(t.getValor());
            } else {
                saidas = saidas.add(t.getValor());
            }
        }
        resumo.removeAll();
        resumo.add(bloco("Saldo", Formatos.moeda(c.getSaldo()), Tema.BRANCO));
        resumo.add(bloco("Entradas", Formatos.moeda(entradas), Tema.ROXO_CLARO));
        resumo.add(bloco("Saídas", Formatos.moeda(saidas), Tema.VERMELHO));

        lista.removeAll();
        if (todas.isEmpty()) {
            JLabel vazio = new JLabel("Sem movimentações");
            vazio.setForeground(Tema.CINZA);
            vazio.setFont(Tema.fonte(java.awt.Font.PLAIN, 14f));
            vazio.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
            lista.add(vazio);
        }
        for (int i = todas.size() - 1; i >= 0; i--) {
            if (i < todas.size() - 1) {
                JPanel s = new JPanel();
                s.setBackground(Tema.SUPERFICIE_ALTA);
                s.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));
                s.setPreferredSize(new java.awt.Dimension(1, 1));
                s.setAlignmentX(LEFT_ALIGNMENT);
                lista.add(s);
            }
            LinhaTransacao l = new LinhaTransacao(todas.get(i), janela.banco());
            l.setAlignmentX(LEFT_ALIGNMENT);
            lista.add(l);
        }
        resumo.revalidate();
        lista.revalidate();
        repaint();
    }

    private static PainelArredondado bloco(String rotulo, String valor, java.awt.Color cor) {
        PainelArredondado p = new PainelArredondado(Tema.SUPERFICIE, 18);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 10));
        JLabel a = new JLabel(rotulo);
        a.setFont(Tema.fonte(java.awt.Font.PLAIN, 12f));
        a.setForeground(Tema.CINZA);
        JLabel b = new JLabel(valor);
        b.setFont(Tema.fonte(java.awt.Font.BOLD, 13f));
        b.setForeground(cor);
        p.add(a);
        p.add(Box.createVerticalStrut(3));
        p.add(b);
        return p;
    }

    private void exportar() {
        JFileChooser escolha = new JFileChooser();
        escolha.setDialogTitle("Salvar extrato");
        escolha.setFileFilter(new FileNameExtensionFilter("Arquivo de texto (*.txt)", "txt"));
        escolha.setSelectedFile(new File("extrato_conta_" + numero + ".txt"));
        if (escolha.showSaveDialog(janela) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = escolha.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith(".txt")) {
            arquivo = new File(arquivo.getParentFile(), arquivo.getName() + ".txt");
        }
        try {
            janela.banco().exportarExtrato(numero, arquivo.toPath());
            janela.aviso("Extrato salvo em " + arquivo.getName(), false);
        } catch (IOException | RuntimeException e) {
            janela.aviso("Não foi possível salvar: " + e.getMessage(), true);
        }
    }
}
