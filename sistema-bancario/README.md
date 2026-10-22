# ThreadBank — Sistema Bancário com Transações Concorrentes

Trabalho de POO (2º bimestre, Opção C). Java 25+, JDBC puro (H2), JUnit 5.

## Estado atual: SEMANAS 1 a 5 + interface gráfica

| Semana | Conteúdo | Onde |
|---|---|---|
| 1 | Domínio sequencial: `Cliente`, `Conta`, `Transacao` | `domain/` |
| 2 | Threads expondo a **race condition** | `simulacao/OperacaoBancariaThread`, `app/Main` |
| 3 | Sincronização (`synchronized`) | `domain/Conta` |
| 4 | Transferência **atômica** e **sem deadlock** (ordem de bloqueio) | `Conta.transferirPara`, `app/DeadlockDemo` |
| 5 | Persistência **JDBC** + `BancoService` + extrato `.txt` | `persistence/`, `service/`, `io/`, `sql/schema.sql` |
| extra | Interface gráfica (Swing) e simulação concorrente na tela | `ui/`, `app/AppBancario` |

## Como rodar

```bash
mvn test                                  # todos os testes (JUnit 5 + H2 em memória)
mvn compile exec:java                     # abre a INTERFACE (app/AppBancario)
mvn compile
java -cp target/classes app.Main          # simulação em console (race condition x sincronizado)
java -cp target/classes app.DeadlockDemo  # deadlock proposital x transferência segura
```
Pela IDE: importe como projeto Maven e rode `app.AppBancario` (interface),
`app.Main` ou `app.DeadlockDemo`.

Na primeira execução da interface são criadas 3 contas de exemplo. Os dados
ficam em `./data/threadbank.mv.db` e **continuam lá** quando você abre de novo
(é a prova da persistência). Para zerar tudo, apague a pasta `data/`.

## Estrutura

```
src/main/java/
  domain/       Cliente, Conta, Transacao, TipoTransacao, exceções   (regra de negócio)
  persistence/  ContaRepository, TransacaoRepository, UnidadeDeTrabalho (interfaces)
    jdbc/       ConnectionFactory, JdbcSessao, JdbcContaRepository, JdbcTransacaoRepository
    memory/     versões em memória (para testar sem banco)
  service/      BancoService  (coordena Conta + repositórios)
  io/           ExtratoExporter (.txt), Formatos (R$, CPF, leitura de valores)
  simulacao/    OperacaoBancariaThread, TransferenciaThread, SimulacaoBancaria
  ui/           telas Swing (tema roxo / preto / branco)
  app/          Main (console), AppBancario (interface), DeadlockDemo
src/main/resources/schema.sql   (cópia em sql/schema.sql)
```
Regra de dependência: `domain` não conhece ninguém. `service` usa `domain` e as
**interfaces** de `persistence`. Só `persistence/jdbc` tem SQL. As threads só
chamam o service/conta.

## Roteiro para explicar (apresentação / relatório)

**Semana 2 → 3 (race condition).** 8 threads × 100 mil operações na mesma conta.
Esperado R$ 4.001.000,00. Sem `synchronized`, o saldo sai errado a cada rodada
(*lost update*: ler, somar e escrever são 3 passos que se misturam). Com
`synchronized`, sai R$ 4.001.000,00 em todas. Logs: `docs/log-antes-exemplo.txt`
e `docs/log-depois-exemplo.txt`.

**Semana 4 (deadlock).** Transferir A→B trava A depois B; B→A trava B depois A.
Se acontecem juntas, cada thread segura um lock e espera o outro **para sempre**.
Solução: ordem global de bloqueio (`Conta.ORDEM_DE_BLOQUEIO`): sempre a conta de
**menor número primeiro**, qualquer que seja o sentido. `app.DeadlockDemo` provoca
o deadlock de propósito, o JVM o detecta (`ThreadMXBean`) e mostra a versão segura
terminando (`docs/log-deadlock-exemplo.txt`). A transferência também é **atômica**:
com os dois locks tomados, ninguém vê o dinheiro "no ar".

**Semana 5 (persistência).** `BancoService` faz: Conta valida e altera o saldo →
cria a `Transacao` → repositórios gravam. Pontos para defender:
- A gravação acontece **dentro do lock da conta**, para a ordem no banco ser a mesma
  da memória (senão um saldo antigo poderia sobrescrever um novo).
- A transferência grava 2 transações + 2 saldos numa **única transação JDBC**
  (`UnidadeDeTrabalho`): ou tudo, ou nada (rollback). Se falhar no meio, a memória
  também é desfeita. Há teste simulando queda no meio da transferência.
- `SimulacaoBancaria` confere, no fim, 4 números por conta: **esperado**, **memória**,
  **banco** e **soma do extrato**. Têm que ser idênticos.

## Testes (115 execuções, incluindo repetições)

- Regras de negócio, `Transacao`, `Cliente` (semana 1).
- Concorrência: depósitos/saques simultâneos; **50 threads disputando 10 saques**
  nunca deixam o saldo negativo (`ContaConcorrenciaTest`).
- Transferências: cruzadas A↔B sem deadlock (`assertTimeoutPreemptively`), malha entre
  5 contas preservando o total, observador que nunca vê soma diferente no meio da
  transferência (`TransferenciaConcorrenciaTest`).
- JDBC contra H2 real, commit/rollback, chave estrangeira (`JdbcRepositoryTest`).
- `BancoService` com repositórios em memória e com H2; falha de gravação no meio da
  transferência; simulação concorrente ponta a ponta.
- Extrato `.txt` e formatação (`ExtratoExporterTest`).

Validação dos testes: trocando a ordem de bloqueio por `synchronized(origem){synchronized(destino)}`,
os testes de transferência **travam/falham** (o teste detecta o deadlock).

## Decisões de projeto

- **Dinheiro é `BigDecimal`** com 2 casas, nunca `double`.
- `Transacao` e `Cliente` imutáveis. `SaldoInsuficienteException` é *checked*
  (situação de negócio esperada); `ValorInvalidoException` é *unchecked* (erro de uso).
- `Conta` é o único lugar que altera saldo; não conhece banco, arquivo nem tela.
- O saldo inicial de uma conta entra no extrato como depósito, para que
  **soma do extrato = saldo** sempre.
- **H2** (embutido, sem instalar) em vez de MySQL. O `schema.sql` é SQL padrão; para
  MySQL/PostgreSQL basta trocar a URL/driver em `ConnectionFactory`.
- A interface substitui o "menu de console": ela usa o **mesmo** `BancoService`.
  Operações simples rodam na tela; a simulação roda em thread de fundo (`SwingWorker`)
  para a janela não travar.

## Limitações conhecidas (bom citar no relatório)

- A garantia de ordem entre memória e banco vale **dentro de uma JVM**. Com vários
  processos acessando o mesmo banco seria preciso controle otimista (coluna `versao`)
  ou `SELECT ... FOR UPDATE`.
- O CPF só valida o formato (11 dígitos), não o dígito verificador.
- `synchronized` é simples e seguro, mas serializa as operações de uma mesma conta;
  o log mostra o custo (~25 ms → ~55 ms por rodada na Semana 3).

## Reproduzir o "antes" da race condition

1. Copie `src/main/java/domain/Conta.java` para um lugar seguro.
2. Substitua o conteúdo pelo de `docs/Conta-semana2-sem-sincronizacao.txt`
   (essa versão é a da Semana 2 e **não tem** `transferirPara` com ordem de bloqueio).
3. Recompile só o necessário e rode `java -cp target/classes app.Main > log-antes.txt`.
4. **Devolva** o `Conta.java` atual.
