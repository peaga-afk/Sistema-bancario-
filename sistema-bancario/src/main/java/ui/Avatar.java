package ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Círculo com as iniciais do titular. */
public class Avatar extends JComponent {

    private final String iniciais;
    private final Color fundo;
    private final int tamanho;

    public Avatar(String nome, Color fundo, int tamanho) {
        this.iniciais = Tema.iniciais(nome);
        this.fundo = fundo;
        this.tamanho = tamanho;
        setPreferredSize(new Dimension(tamanho, tamanho));
        setMinimumSize(new Dimension(tamanho, tamanho));
        setMaximumSize(new Dimension(tamanho, tamanho));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        g2.setColor(fundo);
        g2.fillOval(0, 0, tamanho - 1, tamanho - 1);
        g2.setColor(Tema.BRANCO);
        g2.setFont(Tema.fonte(java.awt.Font.BOLD, tamanho * 0.36f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(iniciais, (tamanho - fm.stringWidth(iniciais)) / 2,
                (tamanho + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
