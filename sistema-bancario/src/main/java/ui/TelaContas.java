package ui;

import domain.Conta;
import io.Formatos;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Primeira tela: escolher com qual conta entrar (ou abrir uma nova). */
public class TelaContas extends JPanel {

    private final JanelaPrincipal janela;
    private final JPanel lista = new JPanel();

    public TelaContas(JanelaPrincipal janela) {
        super(new BorderLayout());
        this.janela = janela;
        setBackground(Tema.FUNDO);

        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setBorder(BorderFactory.createEmptyBorder(34, 26, 12, 26));
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        marca.setOpaque(false);
        JLabel a = new JLabel("Thread");
        a.setFont(Tema.fonte(java.awt.Font.BOLD, 32f));
        a.setForeground(Tema.BRANCO);
        JLabel b = new JLabel("Bank");
        b.setFont(Tema.fonte(java.awt.Font.BOLD, 32f));
        b.setForeground(Tema.ROXO_CLARO);
        marca.add(a);
        marca.add(b);
        JLabel sub = new JLabel("Escolha uma conta para entrar");
        sub.setFont(Tema.fonte(java.awt.Font.PLAIN, 15f));
        sub.setForeground(Tema.CINZA);
        sub.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        marca.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        topo.add(marca);
        topo.add(sub);
        add(topo, BorderLayout.NORTH);

        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        PainelRolavel envoltorio = new PainelRolavel();
        envoltorio.add(lista, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(envoltorio);
        Tema.estilizar(rolagem);
        add(rolagem, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new GridLayout(2, 1, 0, 10));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(10, 22, 24, 22));
        BotaoApp nova = new BotaoApp("Abrir nova conta", BotaoApp.Estilo.PRIMARIO);
        nova.addActionListener(e -> new DialogoNovaConta(janela, this::atualizar).abrir(janela));
        BotaoApp sim = new BotaoApp("Simulação concorrente", BotaoApp.Estilo.CONTORNO);
        sim.addActionListener(e -> janela.abrirSimulacao());
        rodape.add(nova);
        rodape.add(sim);
        add(rodape, BorderLayout.SOUTH);
    }

    public void atualizar() {
        lista.removeAll();
        for (Conta c : janela.banco().listarContas()) {
            lista.add(cartao(c));
            lista.add(Box.createVerticalStrut(12));
        }
        if (lista.getComponentCount() == 0) {
            JLabel vazio = new JLabel("Nenhuma conta ainda. Abra a primeira!");
            vazio.setForeground(Tema.CINZA);
            vazio.setFont(Tema.fonte(java.awt.Font.PLAIN, 15f));
            lista.add(vazio);
        }
        lista.revalidate();
        lista.repaint();
    }

    private PainelArredondado cartao(Conta c) {
        PainelArredondado p = new PainelArredondado(Tema.SUPERFICIE, 22, new BorderLayout(14, 0));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 18));
        p.add(new Avatar(c.getTitular().getNome(), Tema.ROXO, 48), BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(c.getTitular().getNome());
        nome.setFont(Tema.fonte(java.awt.Font.BOLD, 16f));
        nome.setForeground(Tema.BRANCO);
        JLabel numero = new JLabel("Conta " + c.getNumero());
        numero.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        numero.setForeground(Tema.CINZA);
        textos.add(Box.createVerticalGlue());
        textos.add(nome);
        textos.add(numero);
        textos.add(Box.createVerticalGlue());
        p.add(textos, BorderLayout.CENTER);

        JLabel saldo = new JLabel(Formatos.moeda(c.getSaldo()));
        saldo.setFont(Tema.fonte(java.awt.Font.BOLD, 15f));
        saldo.setForeground(Tema.ROXO_CLARO);
        p.add(saldo, BorderLayout.EAST);

        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                janela.abrirInicio(c.getNumero());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                p.setCor(Tema.SUPERFICIE_ALTA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                p.setCor(Tema.SUPERFICIE);
            }
        });
        return Tema.larguraTotal(p);
    }
}
