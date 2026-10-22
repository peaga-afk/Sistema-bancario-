package ui;

import domain.Conta;
import domain.SaldoInsuficienteException;
import io.Formatos;
import service.BancoService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Depositar, sacar ou transferir: um diálogo só, com o corpo adaptado ao tipo. */
public class DialogoOperacao extends DialogoApp {

    public enum Tipo {
        DEPOSITO("Depositar", "Depositar"), SAQUE("Sacar", "Sacar"), TRANSFERENCIA("Transferir", "Transferir");

        final String titulo;
        final String botao;

        Tipo(String titulo, String botao) {
            this.titulo = titulo;
            this.botao = botao;
        }
    }

    private final JanelaPrincipal janela;
    private final Tipo tipo;
    private final long numeroConta;
    private final Runnable aoConcluir;
    private final CampoApp campoValor = new CampoApp("0,00");
    private final List<Chip> chips = new ArrayList<>();
    private Long destinoEscolhido;

    public DialogoOperacao(JanelaPrincipal janela, Tipo tipo, long numeroConta, Runnable aoConcluir) {
        super(janela, tipo.titulo, tipo.botao);
        this.janela = janela;
        this.tipo = tipo;
        this.numeroConta = numeroConta;
        this.aoConcluir = aoConcluir;
    }

    @Override
    protected void montarCorpo(JPanel corpo) {
        corpo.add(legenda("Valor (R$)"));
        alinhar(campoValor);
        corpo.add(campoValor);
        if (tipo == Tipo.TRANSFERENCIA) {
            corpo.add(legenda("Para quem?"));
            JPanel lista = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            lista.setOpaque(false);
            lista.setAlignmentX(LEFT_ALIGNMENT);
            for (Conta c : janela.banco().listarContas()) {
                if (c.getNumero() != numeroConta) {
                    Chip chip = new Chip(c);
                    chips.add(chip);
                    lista.add(chip);
                }
            }
            corpo.add(lista);
        }
    }

    @Override
    public void abrir(JanelaPrincipal dono) {
        SwingUtilities.invokeLater(campoValor::requestFocusInWindow);
        super.abrir(dono);
    }

    @Override
    protected String confirmar() {
        try {
            BigDecimal valor = Formatos.lerValor(campoValor.getText());
            BancoService banco = janela.banco();
            switch (tipo) {
                case DEPOSITO:
                    banco.depositar(numeroConta, valor);
                    janela.aviso("Depósito de " + Formatos.moeda(valor.setScale(2)) + " realizado", false);
                    break;
                case SAQUE:
                    banco.sacar(numeroConta, valor);
                    janela.aviso("Saque de " + Formatos.moeda(valor.setScale(2)) + " realizado", false);
                    break;
                default:
                    if (destinoEscolhido == null) {
                        return "Escolha a conta de destino.";
                    }
                    banco.transferir(numeroConta, destinoEscolhido, valor);
                    janela.aviso("Transferência de " + Formatos.moeda(valor.setScale(2)) + " enviada", false);
                    break;
            }
            aoConcluir.run();
            return null;
        } catch (SaldoInsuficienteException e) {
            return "Saldo insuficiente. Seu saldo é " + Formatos.moeda(e.getSaldoAtual()) + ".";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    /** Opção de destino (um "chip" selecionável). */
    private class Chip extends PainelArredondado {
        private final long numero;

        Chip(Conta conta) {
            super(Tema.SUPERFICIE_ALTA, 18, new FlowLayout(FlowLayout.CENTER, 14, 8));
            this.numero = conta.getNumero();
            JLabel l = new JLabel(conta.getTitular().getNome() + " · " + conta.getNumero());
            l.setFont(Tema.fonte(java.awt.Font.PLAIN, 13f));
            l.setForeground(Tema.BRANCO);
            add(l);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    destinoEscolhido = numero;
                    for (Chip c : chips) {
                        c.setCor(c == Chip.this ? Tema.ROXO : Tema.SUPERFICIE_ALTA);
                    }
                }
            });
        }
    }
}
