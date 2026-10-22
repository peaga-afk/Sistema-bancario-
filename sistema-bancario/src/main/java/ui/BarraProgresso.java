package ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Barra de progresso roxa arredondada, com o percentual escrito. */
public class BarraProgresso extends JComponent {

    private double fracao;

    public BarraProgresso() {
        setPreferredSize(new Dimension(100, 26));
        setMinimumSize(new Dimension(100, 26));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
    }

    public void setFracao(double f) {
        this.fracao = Math.max(0, Math.min(1, f));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Tema.SUPERFICIE_ALTA);
        g2.fillRoundRect(0, 0, w, h, h, h);
        int largura = (int) Math.round(w * fracao);
        if (largura > 0) {
            g2.setPaint(new GradientPaint(0, 0, Tema.ROXO, w, 0, Tema.ROXO_CLARO));
            g2.fillRoundRect(0, 0, Math.max(largura, h), h, h, h);
        }
        String texto = Math.round(fracao * 100) + "%";
        g2.setFont(Tema.fonte(java.awt.Font.BOLD, 12f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Tema.BRANCO);
        g2.drawString(texto, (w - fm.stringWidth(texto)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
