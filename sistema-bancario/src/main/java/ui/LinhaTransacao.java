package ui;

import domain.ContaNaoEncontradaException;
import domain.Transacao;
import io.Formatos;
import service.BancoService;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Uma linha do extrato: ícone, descrição, data e valor colorido. */
public class LinhaTransacao extends JComponent {

    private final String titulo;
    private final String subtitulo;
    private final String valor;
    private final boolean entrada;

    public LinhaTransacao(Transacao t, BancoService banco) {
        this.entrada = BancoService.ehEntrada(t.getTipo());
        this.titulo = titulo(t);
        String data = Formatos.dataHoraCurta(t.getHorario());
        this.subtitulo = t.getContaContraparte() == null ? data : nomeContraparte(t, banco) + " · " + data;
        this.valor = (entrada ? "+ " : "- ") + Formatos.moeda(t.getValor());
        setPreferredSize(new Dimension(100, 64));
        setMinimumSize(new Dimension(100, 64));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
    }

    private static String titulo(Transacao t) {
        return Formatos.tipo(t.getTipo());
    }

    private static String nomeContraparte(Transacao t, BancoService banco) {
        try {
            return banco.buscarConta(t.getContaContraparte()).getTitular().getNome();
        } catch (ContaNaoEncontradaException ignorada) {
            return "conta " + t.getContaContraparte();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.qualidade(g2);
        int h = getHeight();
        Color cor = entrada ? Tema.ROXO_CLARO : Tema.VERMELHO;

        int d = 38;
        int y0 = (h - d) / 2;
        g2.setColor(Tema.comAlfa(cor, 40));
        g2.fillOval(0, y0, d, d);
        g2.setColor(cor);
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = d / 2;
        int cy = h / 2;
        if (entrada) {          // seta para baixo: dinheiro entrando
            g2.drawLine(cx, cy - 7, cx, cy + 7);
            g2.drawLine(cx, cy + 7, cx - 5, cy + 2);
            g2.drawLine(cx, cy + 7, cx + 5, cy + 2);
        } else {                // seta para cima: dinheiro saindo
            g2.drawLine(cx, cy + 7, cx, cy - 7);
            g2.drawLine(cx, cy - 7, cx - 5, cy - 2);
            g2.drawLine(cx, cy - 7, cx + 5, cy - 2);
        }

        g2.setFont(Tema.fonte(java.awt.Font.BOLD, 15f));
        FontMetrics fmValor = g2.getFontMetrics();
        int larguraValor = fmValor.stringWidth(valor);
        g2.setColor(entrada ? Tema.ROXO_CLARO : Tema.VERMELHO);
        g2.drawString(valor, getWidth() - larguraValor, h / 2 + fmValor.getAscent() / 2 - 1);

        int xTexto = d + 14;
        int disponivel = getWidth() - xTexto - larguraValor - 14;
        g2.setFont(Tema.fonte(java.awt.Font.BOLD, 14f));
        g2.setColor(Tema.BRANCO);
        g2.drawString(cortar(g2.getFontMetrics(), titulo, disponivel), xTexto, h / 2 - 3);
        g2.setFont(Tema.fonte(java.awt.Font.PLAIN, 12f));
        g2.setColor(Tema.CINZA);
        g2.drawString(cortar(g2.getFontMetrics(), subtitulo, disponivel), xTexto, h / 2 + 15);
        g2.dispose();
    }

    private static String cortar(FontMetrics fm, String texto, int largura) {
        if (fm.stringWidth(texto) <= largura) {
            return texto;
        }
        String t = texto;
        while (t.length() > 1 && fm.stringWidth(t + "…") > largura) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }
}
