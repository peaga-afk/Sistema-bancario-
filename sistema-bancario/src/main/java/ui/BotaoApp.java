package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Botão retangular arredondado em quatro estilos. */
public class BotaoApp extends JButton {

    public enum Estilo { PRIMARIO, SECUNDARIO, CONTORNO, FANTASMA }

    private final Estilo estilo;
    private boolean sobre;

    public BotaoApp(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setFont(Tema.fonte(java.awt.Font.BOLD, 15f));
        setForeground(estilo == Estilo.FANTASMA ? Tema.ROXO_CLARO : Tema.BRANCO);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        Color base;
        switch (estilo) {
            case PRIMARIO:
                base = Tema.ROXO;
                break;
            case SECUNDARIO:
                base = Tema.SUPERFICIE_ALTA;
                break;
            default:
                base = null;
        }
        if (base != null) {
            if (getModel().isPressed()) {
                base = base.darker();
            } else if (sobre) {
                base = estilo == Estilo.PRIMARIO ? new Color(0xA020D6) : new Color(0x2E2E3E);
            }
            g2.setColor(isEnabled() ? base : Tema.comAlfa(base, 90));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        } else if (estilo == Estilo.CONTORNO) {
            g2.setColor(sobre ? Tema.ROXO_CLARO : Tema.ROXO);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        } else if (sobre) {
            g2.setColor(Tema.comAlfa(Tema.ROXO, 40));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
