# Sistema Bancário com Transações Concorrentes

Projeto de Programação Orientada a Objetos (Java) — 2º bimestre, Opção C.

## Estado atual: SEMANAS 1 e 2

| Semana | O que tem | Status |
|---|---|---|
| 1 | Domínio: `Cliente`, `Conta`, `Transacao`, exceções; depositar/sacar/transferir **sequenciais**; testes | feito |
| 2 | `OperacaoBancariaThread` + `Main` que **expõe a race condition** (sem sincronização) | feito |
| 3 | `synchronized` em `Conta` | próxima |

> A `Conta` está **sem sincronização de propósito**. Os pontos `TODO SEMANA 3`
> e `TODO SEMANA 4` no código marcam onde as próximas correções entram.

## Estrutura

```
src/main/java/
  domain/      Cliente, Conta, Transacao, TipoTransacao,
               SaldoInsuficienteException, ValorInvalidoException
  simulacao/   OperacaoBancariaThread (Runnable)
  app/         Main (dispara as threads e compara esperado x obtido)
src/test/java/
  domain/      ClienteTest, TransacaoTest, ContaTest
  simulacao/   OperacaoBancariaThreadTest
docs/
  log-antes-exemplo.txt   exemplo de saída da simulação sem sincronização
```

## Como rodar

### Com Maven
```bash
mvn test                              # roda os 24 testes
mvn compile
java -cp target/classes app.Main      # simulação (a race condition aparece aqui)
```

### Pela IDE (IntelliJ / Eclipse / VS Code)
Importe como projeto Maven (ou aponte `src/main/java` e `src/test/java` como
pastas de código) e rode `app.Main`. Requer **Java 17+**.

## Documentando o "antes" para o relatório

Rode a simulação **na sua máquina** e guarde a saída (o resultado varia de
máquina para máquina, e o relatório deve mostrar a **sua** execução):

```bash
java -cp target/classes app.Main > log-antes.txt
```

Cole o conteúdo (ou um print) no relatório. Na Semana 3, rode o mesmo `Main`
depois de sincronizar a `Conta` e guarde como `log-depois.txt`.

## Decisões de projeto

- **Dinheiro é `BigDecimal`** com 2 casas decimais, nunca `double`.
- `Transacao` e `Cliente` são **imutáveis** (campos `final`, sem setters).
- `SaldoInsuficienteException` é **verificada** (situação de negócio esperada);
  `ValorInvalidoException` é **não verificada** (erro de uso).
- `Conta` é o único lugar que altera saldo; não conhece banco, arquivo nem rede.
- `OperacaoBancariaThread` não tem regra de negócio: só chama a `Conta`.
- Quem vai criar e persistir as `Transacao` é o `BancoService` (semana 5).
