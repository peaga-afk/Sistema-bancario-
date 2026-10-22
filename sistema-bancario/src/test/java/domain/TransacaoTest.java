package domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoTest {

    private static final BigDecimal CEM = new BigDecimal("100.00");

    @Test
    void guardaTipoValorEHorario() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 15, 10, 30);
        Transacao t = new Transacao(7L, 1L, TipoTransacao.DEPOSITO, CEM, agora, null);

        assertEquals(7L, t.getId());
        assertEquals(1L, t.getNumeroConta());
        assertEquals(TipoTransacao.DEPOSITO, t.getTipo());
        assertEquals(CEM, t.getValor());
        assertEquals(agora, t.getHorario());
        assertNull(t.getContaContraparte());
    }

    @Test
    void novaTransacaoNaoTemIdEPossuiHorario() {
        Transacao t = Transacao.nova(1L, TipoTransacao.SAQUE, CEM, null);
        assertNull(t.getId());
        assertNotNull(t.getHorario());
    }

    @Test
    void rejeitaValorNaoPositivo() {
        assertThrows(ValorInvalidoException.class,
                () -> Transacao.nova(1L, TipoTransacao.DEPOSITO, BigDecimal.ZERO, null));
        assertThrows(ValorInvalidoException.class,
                () -> Transacao.nova(1L, TipoTransacao.DEPOSITO, new BigDecimal("-1.00"), null));
    }

    @Test
    void rejeitaTipoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Transacao.nova(1L, null, CEM, null));
    }

    @Test
    void transferenciaExigeContraparte() {
        assertThrows(IllegalArgumentException.class,
                () -> Transacao.nova(1L, TipoTransacao.TRANSFERENCIA_ENVIADA, CEM, null));
        Transacao t = Transacao.nova(1L, TipoTransacao.TRANSFERENCIA_ENVIADA, CEM, 2L);
        assertEquals(2L, t.getContaContraparte());
    }
}
