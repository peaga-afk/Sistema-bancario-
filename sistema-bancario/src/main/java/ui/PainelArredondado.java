package ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/** Painel com fundo de cantos arredondados (o "cartão" do app). */
public class PainelArredondado extends JPanel {

    private Color cor;
    private Color borda;
    private final int raio;

    public PainelArredondado(Color cor, int raio) {
        this(cor, raio, null);
    }

    public PainelArredondado(Color cor, int raio, LayoutManager layout) {
        super(layout);
        this.cor = cor;
        this.raio = raio;
        setOpaque(false);
    }

    public void setCor(Color cor) {
        this.cor = cor;
        repaint();
    }

    public void setBorda(Color borda) {
        this.borda = borda;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        g2.setColor(cor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), raio, raio);
        if (borda != null) {
            g2.setColor(borda);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, raio, raio);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
