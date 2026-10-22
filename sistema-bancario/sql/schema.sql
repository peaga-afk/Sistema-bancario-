-- ============================================================================
-- Sistema Bancário com Transações Concorrentes — criação das tabelas
-- Banco: H2 (compatível com MySQL/PostgreSQL com pequenos ajustes de tipo).
-- Pode ser executado várias vezes (IF NOT EXISTS).
-- ============================================================================

CREATE TABLE IF NOT EXISTS contas (
    numero        BIGINT        PRIMARY KEY,
    titular_nome  VARCHAR(120)  NOT NULL,
    titular_cpf   VARCHAR(11)   NOT NULL,
    saldo         DECIMAL(15,2) NOT NULL CHECK (saldo >= 0)
);

CREATE TABLE IF NOT EXISTS transacoes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_conta       BIGINT        NOT NULL,
    tipo               VARCHAR(30)   NOT NULL,
    valor              DECIMAL(15,2) NOT NULL CHECK (valor > 0),
    horario            TIMESTAMP     NOT NULL,
    conta_contraparte  BIGINT,
    FOREIGN KEY (numero_conta) REFERENCES contas (numero)
);

CREATE INDEX IF NOT EXISTS idx_transacoes_conta ON transacoes (numero_conta, id);
