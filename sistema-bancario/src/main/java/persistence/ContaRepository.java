package persistence;

import domain.Conta;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência das contas. Não contém nenhum detalhe de JDBC:
 * quem usa só conhece esta interface (Dependency Inversion).
 *
 * Atenção: buscar devolve uma NOVA instância de Conta montada com os dados
 * gravados. Quem precisa de uma única instância compartilhada entre threads
 * (o BancoService) é quem a guarda.
 */
public interface ContaRepository {

    /** Insere a conta se ela ainda não existe; senão atualiza titular e saldo. */
    void salvar(Conta conta);

    Optional<Conta> buscarPorNumero(long numero);

    /** Todas as contas, ordenadas por número. */
    List<Conta> listarTodas();

    /** Próximo número de conta livre. */
    long proximoNumero();
}
