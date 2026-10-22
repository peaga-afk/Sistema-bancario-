package ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Botão redondo com ícone desenhado e rótulo embaixo (Depositar, Sacar...). */
public class BotaoAcao extends JButton {

    public enum Icone { MAIS, MENOS, TRANSFERIR, EXTRATO }

    private static final int DIAMETRO = 62;

    private final Icone icone;
    private boolean sobre;

    public BotaoAcao(String rotulo, Icone icone) {
        super(rotulo);
        this.icone = icone;
        setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
        setForeground(Tema.BRANCO);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(82, 100));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sobre = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobre = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        int cx = getWidth() / 2;
        int topo = 4;

        g2.setColor(getModel().isPressed() ? Tema.ROXO_ESCURO : sobre ? Tema.ROXO : Tema.SUPERFICIE_ALTA);
        g2.fillOval(cx - DIAMETRO / 2, topo, DIAMETRO, DIAMETRO);
        g2.setColor(Tema.comAlfa(Tema.ROXO, 140));
        g2.drawOval(cx - DIAMETRO / 2, topo, DIAMETRO - 1, DIAMETRO - 1);

        g2.setColor(sobre ? Tema.BRANCO : Tema.ROXO_CLARO);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cy = topo + DIAMETRO / 2;
        switch (icone) {
            case MAIS:
                g2.drawLine(cx - 11, cy, cx + 11, cy);
                g2.drawLine(cx, cy - 11, cx, cy + 11);
                break;
            case MENOS:
                g2.drawLine(cx - 11, cy, cx + 11, cy);
                break;
            case TRANSFERIR:
                g2.drawLine(cx - 12, cy - 6, cx + 12, cy - 6);
                g2.drawLine(cx + 12, cy - 6, cx + 6, cy - 12);
                g2.drawLine(cx + 12, cy - 6, cx + 6, cy);
                g2.drawLine(cx + 12, cy + 7, cx - 12, cy + 7);
                g2.drawLine(cx - 12, cy + 7, cx - 6, cy + 1);
                g2.drawLine(cx - 12, cy + 7, cx - 6, cy + 13);
                break;
            default:
                g2.drawLine(cx - 11, cy - 8, cx + 11, cy - 8);
                g2.drawLine(cx - 11, cy, cx + 11, cy);
                g2.drawLine(cx - 11, cy + 8, cx + 3, cy + 8);
                break;
        }

        g2.setFont(getFont());
        g2.setColor(Tema.BRANCO);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), cx - fm.stringWidth(getText()) / 2, topo + DIAMETRO + 8 + fm.getAscent());
        g2.dispose();
    }
}
