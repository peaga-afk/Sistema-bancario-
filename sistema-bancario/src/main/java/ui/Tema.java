package ui;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.util.Set;
import javax.swing.JButton;

/** Cores, fontes e utilidades visuais do app: preto + roxo + branco. */
public final class Tema {

    public static final Color FUNDO = new Color(0x0B0B10);
    public static final Color SUPERFICIE = new Color(0x16161F);
    public static final Color SUPERFICIE_ALTA = new Color(0x22222E);
    public static final Color ROXO = new Color(0x8A05BE);
    public static final Color ROXO_ESCURO = new Color(0x4F0A75);
    public static final Color ROXO_CLARO = new Color(0xC58CFF);
    public static final Color BRANCO = Color.WHITE;
    public static final Color CINZA = new Color(0x9C9CB4);
    public static final Color VERMELHO = new Color(0xFF5470);

    private static final String FAMILIA = escolherFamilia();

    private Tema() {
    }

    private static String escolherFamilia() {
        Set<String> disponiveis = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Inter", "Poppins"}) {
            if (disponiveis.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font fonte(int estilo, float tamanho) {
        return new Font(FAMILIA, estilo, 12).deriveFont(estilo, tamanho);
    }

    public static void qualidade(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color comAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    public static String iniciais(String nome) {
        String[] p = nome.trim().split("\\s+");
        String r = p[0].substring(0, 1);
        if (p.length > 1) {
            r += p[p.length - 1].substring(0, 1);
        }
        return r.toUpperCase();
    }

    /**
     * Texto com quebra de linha dentro de um JLabel. O HTML do Swing converte "px" com um fator de
     * ~1,33 (96/72), então a largura pedida em pixels reais é dividida por esse fator.
     */
    public static String html(String texto, int larguraPixels) {
        return "<html><body style='width:" + Math.round(larguraPixels / 1.33) + "px'>" + texto + "</body></html>";
    }

    /** Faz um componente ocupar toda a largura dentro de um BoxLayout vertical. */
    public static <T extends JComponent> T larguraTotal(T c) {
        c.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** Rolagem discreta: trilho invisível e "pílula" roxa. */
    public static void estilizar(JScrollPane scroll) {
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new BarraRoxa());
        scroll.getVerticalScrollBar().setOpaque(false);
    }

    private static class BarraRoxa extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = comAlfa(ROXO, 190);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // trilho transparente
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            qualidade(g2);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 8, 8);
            g2.dispose();
        }
    }
}
