package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    void criaClienteValidoNormalizandoCpf() {
        Cliente c = new Cliente("  Ana Souza ", "123.456.789-09");
        assertEquals("Ana Souza", c.getNome());
        assertEquals("12345678909", c.getCpf());
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente("   ", "12345678909"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente(null, "12345678909"));
    }

    @Test
    void rejeitaCpfComTamanhoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana", "123"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana", null));
    }

    @Test
    void clientesComMesmoCpfSaoIguais() {
        assertEquals(new Cliente("Ana", "123.456.789-09"), new Cliente("Ana Souza", "12345678909"));
    }
}
