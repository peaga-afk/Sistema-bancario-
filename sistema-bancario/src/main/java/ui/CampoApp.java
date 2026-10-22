package ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Campo de texto escuro, arredondado, com texto de dica (placeholder). */
public class CampoApp extends JTextField {

    private final String dica;

    public CampoApp(String dica) {
        this.dica = dica;
        setOpaque(false);
        setFont(Tema.fonte(java.awt.Font.PLAIN, 16f));
        setForeground(Tema.BRANCO);
        setCaretColor(Tema.ROXO_CLARO);
        setSelectionColor(Tema.ROXO);
        setSelectedTextColor(Tema.BRANCO);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        g2.setColor(Tema.SUPERFICIE_ALTA);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        if (isFocusOwner()) {
            g2.setColor(Tema.ROXO);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        }
        g2.dispose();
        super.paintComponent(g);
        if (getText().isEmpty() && dica != null) {
            Graphics2D g3 = (Graphics2D) g.create();
            Tema.qualidade(g3);
            g3.setFont(getFont());
            g3.setColor(Tema.CINZA);
            g3.drawString(dica, getInsets().left, (getHeight() + g3.getFontMetrics().getAscent()
                    - g3.getFontMetrics().getDescent()) / 2);
            g3.dispose();
        }
    }
}
