package ui;

import domain.Cliente;
import domain.Conta;
import io.Formatos;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.math.BigDecimal;

/** Formulário de abertura de conta. */
public class DialogoNovaConta extends DialogoApp {

    private final JanelaPrincipal janela;
    private final Runnable aoConcluir;
    private final CampoApp nome = new CampoApp("Nome completo");
    private final CampoApp cpf = new CampoApp("000.000.000-00");
    private final CampoApp saldo = new CampoApp("0,00");

    public DialogoNovaConta(JanelaPrincipal janela, Runnable aoConcluir) {
        super(janela, "Nova conta", "Abrir conta");
        this.janela = janela;
        this.aoConcluir = aoConcluir;
    }

    @Override
    protected void montarCorpo(JPanel corpo) {
        corpo.add(legenda("Nome do titular"));
        alinhar(nome);
        corpo.add(nome);
        corpo.add(legenda("CPF"));
        alinhar(cpf);
        corpo.add(cpf);
        corpo.add(legenda("Depósito inicial (R$)"));
        alinhar(saldo);
        corpo.add(saldo);
    }

    @Override
    public void abrir(JanelaPrincipal dono) {
        SwingUtilities.invokeLater(nome::requestFocusInWindow);
        super.abrir(dono);
    }

    @Override
    protected String confirmar() {
        try {
            BigDecimal inicial = saldo.getText().isBlank() ? BigDecimal.ZERO : Formatos.lerValor(saldo.getText());
            Conta c = janela.banco().criarConta(new Cliente(nome.getText(), cpf.getText()), inicial);
            janela.aviso("Conta " + c.getNumero() + " criada para " + c.getTitular().getNome(), false);
            aoConcluir.run();
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
