package ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/** Painel para dentro de um JScrollPane que acompanha a LARGURA da janela (só rola na vertical). */
public class PainelRolavel extends JPanel implements Scrollable {

    public PainelRolavel() {
        super(new BorderLayout());
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visivel, int orientacao, int direcao) {
        return 18;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visivel, int orientacao, int direcao) {
        return Math.max(visivel.height - 24, 24);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
