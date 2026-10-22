package ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Mensagem rápida ("toast") que aparece na parte de baixo da janela e some sozinha. */
public class Aviso extends JComponent {

    private String texto;
    private boolean erro;
    private final Timer relogio = new Timer(2800, e -> {
        texto = null;
        repaint();
    });

    public Aviso() {
        relogio.setRepeats(false);
        setOpaque(false);
    }

    public void mostrar(String mensagem, boolean ehErro) {
        this.texto = mensagem;
        this.erro = ehErro;
        repaint();
        relogio.restart();
    }

    /** Não captura cliques: o que está embaixo continua clicável. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (texto == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        g2.setFont(Tema.fonte(java.awt.Font.BOLD, 14f));
        FontMetrics fm = g2.getFontMetrics();
        int w = Math.min(getWidth() - 40, fm.stringWidth(texto) + 44);
        int h = 44;
        int x = (getWidth() - w) / 2;
        int y = getHeight() - h - 28;
        g2.setColor(erro ? new Color(0x8E1B33) : Tema.ROXO);
        g2.fillRoundRect(x, y, w, h, 22, 22);
        g2.setColor(Tema.BRANCO);
        g2.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
