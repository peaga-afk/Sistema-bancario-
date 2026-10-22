package ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

/**
 * Janela de diálogo no tema do app. A subclasse monta o corpo e implementa
 * confirmar(): devolve null se deu certo (fecha) ou o texto do erro (fica aberta).
 */
public abstract class DialogoApp extends JDialog {

    private final JLabel rotuloErro = new JLabel(" ");
    private final JPanel centro = new JPanel();
    private int larguraFixa = 380;

    protected DialogoApp(JanelaPrincipal dono, String titulo, String confirmarTexto) {
        super(dono, true);
        setUndecorated(true);
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.SUPERFICIE);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.ROXO, 1),
                BorderFactory.createEmptyBorder(22, 24, 20, 24)));

        JLabel t = new JLabel(titulo);
        t.setFont(Tema.fonte(java.awt.Font.BOLD, 20f));
        t.setForeground(Tema.BRANCO);
        raiz.add(t, BorderLayout.NORTH);

        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        rotuloErro.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        rotuloErro.setForeground(Tema.VERMELHO);
        rotuloErro.setAlignmentX(LEFT_ALIGNMENT);
        raiz.add(centro, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        BotaoApp cancelar = new BotaoApp("Cancelar", BotaoApp.Estilo.FANTASMA);
        BotaoApp ok = new BotaoApp(confirmarTexto, BotaoApp.Estilo.PRIMARIO);
        cancelar.addActionListener(e -> dispose());
        ok.addActionListener(e -> tentarConfirmar());
        botoes.add(cancelar);
        botoes.add(ok);
        raiz.add(botoes, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        larguraFixa = Math.min(380, dono.getWidth() - 30);
    }

    /** Subclasse adiciona os campos aqui. Chamado só em abrir(), quando a subclasse já está pronta. */
    protected abstract void montarCorpo(JPanel corpo);

    /** @return null se confirmou com sucesso, ou a mensagem de erro a mostrar. */
    protected abstract String confirmar();

    private void tentarConfirmar() {
        String erro = confirmar();
        if (erro == null) {
            dispose();
        } else {
            rotuloErro.setText(Tema.html(erro, 320));
            ajustar();
        }
    }

    private void ajustar() {
        setPreferredSize(null);
        pack();
        setSize(new Dimension(larguraFixa, getHeight()));
    }

    public void abrir(JanelaPrincipal dono) {
        montarCorpo(centro);
        centro.add(rotuloErro);
        ajustar();
        setLocationRelativeTo(dono);
        setVisible(true);
    }

    /** Rótulo pequeno acima de um campo. */
    protected static JLabel legenda(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        l.setForeground(Tema.CINZA);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(10, 2, 6, 0));
        return l;
    }

    protected static void alinhar(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
    }
}
