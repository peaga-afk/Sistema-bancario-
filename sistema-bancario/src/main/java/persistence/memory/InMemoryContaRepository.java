package persistence.memory;

import domain.Cliente;
import domain.Conta;
import persistence.ContaRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória, sem banco. Serve para testar o BancoService sem
 * depender de JDBC. Guarda CÓPIAS, como um banco de verdade faria.
 */
public class InMemoryContaRepository implements ContaRepository {

    private final Map<Long, Conta> armazenadas = new ConcurrentHashMap<>();

    @Override
    public void salvar(Conta conta) {
        armazenadas.put(conta.getNumero(), copia(conta));
    }

    @Override
    public Optional<Conta> buscarPorNumero(long numero) {
        return Optional.ofNullable(armazenadas.get(numero)).map(InMemoryContaRepository::copia);
    }

    @Override
    public List<Conta> listarTodas() {
        List<Conta> lista = new ArrayList<>();
        armazenadas.values().forEach(c -> lista.add(copia(c)));
        lista.sort(Comparator.comparingLong(Conta::getNumero));
        return lista;
    }

    @Override
    public synchronized long proximoNumero() {
        return armazenadas.keySet().stream().mapToLong(Long::longValue).max().orElse(1000) + 1;
    }

    private static Conta copia(Conta c) {
        Cliente t = c.getTitular();
        return new Conta(c.getNumero(), new Cliente(t.getNome(), t.getCpf()), c.getSaldo());
    }
}
