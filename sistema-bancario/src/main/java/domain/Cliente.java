package domain;

import java.util.Objects;

/**
 * Titular de uma ou mais contas. Imutável.
 * Não conhece banco, arquivos nem threads.
 */
public final class Cliente {

    private final String nome;
    private final String cpf; // somente dígitos

    public Cliente(String nome, String cpf) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório");
        }
        if (cpf == null) {
            throw new IllegalArgumentException("CPF do cliente é obrigatório");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException("CPF deve ter 11 dígitos");
        }
        this.nome = nome.trim();
        this.cpf = digitos;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente outro)) return false;
        return cpf.equals(outro.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }

    @Override
    public String toString() {
        return nome + " (CPF " + cpf + ")";
    }
}
