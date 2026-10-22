package ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Ícone de "olho" para mostrar/ocultar o saldo. */
public class BotaoOlho extends JButton {

    private boolean aberto = true;
    private Runnable aoAlternar = () -> { };

    public BotaoOlho() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(40, 36));
        addActionListener(e -> {
            aberto = !aberto;
            repaint();
            aoAlternar.run();   // avisa DEPOIS de trocar (a ordem dos listeners do Swing é invertida)
        });
    }

    /** Ação executada depois que o olho abre/fecha. */
    public void setAoAlternar(Runnable acao) {
        this.aoAlternar = acao;
    }

    public boolean isAberto() {
        return aberto;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        g2.setColor(Tema.BRANCO);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2.drawArc(cx - 12, cy - 9, 24, 18, 0, 180);
        g2.drawArc(cx - 12, cy - 9, 24, 18, 180, 180);
        g2.fillOval(cx - 4, cy - 4, 8, 8);
        if (!aberto) {
            g2.drawLine(cx - 12, cy + 11, cx + 12, cy - 11);
        }
        g2.dispose();
    }
}
