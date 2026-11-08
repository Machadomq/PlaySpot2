-- Modelo inicial do PlaySpot. Ver docs/02-modelo-de-dados.md

-- Necessária para a regra de "sem reservas sobrepostas" (combina = com && no mesmo índice)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        TEXT        NOT NULL,
    email       TEXT        NOT NULL UNIQUE,
    senha_hash  TEXT        NOT NULL,
    telefone    TEXT,
    papel       TEXT        NOT NULL CHECK (papel IN ('JOGADOR', 'DONO', 'ADMIN')),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE empresa (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dono_id    BIGINT  NOT NULL REFERENCES usuario (id),
    nome       TEXT    NOT NULL,
    documento  TEXT,
    telefone   TEXT,
    endereco   TEXT,
    cidade     TEXT,
    uf         CHAR(2),
    ativa      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE esporte (
    id    SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome  TEXT NOT NULL UNIQUE
);

CREATE TABLE quadra (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id        BIGINT  NOT NULL REFERENCES empresa (id),
    nome              TEXT    NOT NULL,
    descricao         TEXT,
    duracao_slot_min  INTEGER NOT NULL DEFAULT 60 CHECK (duracao_slot_min > 0),
    ativa             BOOLEAN NOT NULL DEFAULT TRUE
);

-- N:N entre quadra e esporte
CREATE TABLE quadra_esporte (
    quadra_id   BIGINT   NOT NULL REFERENCES quadra (id),
    esporte_id  SMALLINT NOT NULL REFERENCES esporte (id),
    PRIMARY KEY (quadra_id, esporte_id)
);

-- Faixa de horário com preço, por quadra e dia da semana
CREATE TABLE funcionamento (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quadra_id    BIGINT        NOT NULL REFERENCES quadra (id),
    dia_semana   SMALLINT      NOT NULL CHECK (dia_semana BETWEEN 0 AND 6), -- 0 = domingo
    hora_inicio  TIME          NOT NULL,
    hora_fim     TIME          NOT NULL,
    preco_hora   NUMERIC(10,2) NOT NULL CHECK (preco_hora >= 0),
    CHECK (hora_fim > hora_inicio)
);

CREATE TABLE reserva (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   BIGINT        NOT NULL REFERENCES usuario (id),
    quadra_id    BIGINT        NOT NULL REFERENCES quadra (id),
    inicio       TIMESTAMPTZ   NOT NULL,
    fim          TIMESTAMPTZ   NOT NULL,
    preco_total  NUMERIC(10,2) NOT NULL CHECK (preco_total >= 0),
    status       TEXT          NOT NULL CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA')),
    criada_em    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (fim > inicio),
    -- Regra central: mesma quadra não pode ter duas reservas ativas com horários sobrepostos
    CONSTRAINT reserva_sem_sobreposicao
        EXCLUDE USING gist (quadra_id WITH =, tstzrange(inicio, fim) WITH &&)
        WHERE (status <> 'CANCELADA')
);

CREATE TABLE pagamento (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id  BIGINT        NOT NULL REFERENCES reserva (id),
    valor       NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    metodo      TEXT          NOT NULL CHECK (metodo IN ('PIX', 'CARTAO', 'DINHEIRO')),
    status      TEXT          NOT NULL CHECK (status IN ('PENDENTE', 'PAGO', 'FALHOU', 'ESTORNADO')),
    id_externo  TEXT,
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Índices nas chaves estrangeiras mais consultadas
CREATE INDEX idx_empresa_dono         ON empresa (dono_id);
CREATE INDEX idx_quadra_empresa       ON quadra (empresa_id);
CREATE INDEX idx_funcionamento_quadra ON funcionamento (quadra_id);
CREATE INDEX idx_reserva_usuario      ON reserva (usuario_id);
CREATE INDEX idx_pagamento_reserva    ON pagamento (reserva_id);
