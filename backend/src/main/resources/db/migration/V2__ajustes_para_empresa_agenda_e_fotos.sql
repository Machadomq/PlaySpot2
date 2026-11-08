-- Ajustes descritos em docs/03-plano-do-app.md

-- Empresa: uma por dono, com regra de cancelamento e fuso próprios
ALTER TABLE empresa
    ADD COLUMN prazo_cancelamento_horas INTEGER NOT NULL DEFAULT 24 CHECK (prazo_cancelamento_horas >= 0),
    ADD COLUMN fuso TEXT NOT NULL DEFAULT 'America/Sao_Paulo',
    ALTER COLUMN uf TYPE TEXT,
    ADD CONSTRAINT empresa_uf_tamanho CHECK (char_length(uf) = 2),
    ADD CONSTRAINT empresa_dono_unico UNIQUE (dono_id);

DROP INDEX idx_empresa_dono;

-- Reserva: o dono pode lançar cliente sem conta no app, e faltas passam a ser registradas
ALTER TABLE reserva
    ALTER COLUMN usuario_id DROP NOT NULL,
    ADD COLUMN cliente_nome TEXT,
    ADD CONSTRAINT reserva_tem_cliente CHECK (usuario_id IS NOT NULL OR cliente_nome IS NOT NULL),
    DROP CONSTRAINT reserva_status_check,
    ADD CONSTRAINT reserva_status_check
        CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA', 'FALTOU'));

-- Funcionamento: hora_fim = 00:00 significa "até a meia-noite"; faixas da mesma quadra e dia não se sobrepõem
ALTER TABLE funcionamento
    DROP CONSTRAINT funcionamento_check,
    ADD CONSTRAINT funcionamento_horas_validas CHECK (hora_fim > hora_inicio OR hora_fim = TIME '00:00'),
    ADD CONSTRAINT funcionamento_sem_sobreposicao
        EXCLUDE USING gist (
            quadra_id WITH =,
            dia_semana WITH =,
            numrange(
                EXTRACT(EPOCH FROM hora_inicio),
                CASE WHEN hora_fim = TIME '00:00' THEN 86400 ELSE EXTRACT(EPOCH FROM hora_fim) END
            ) WITH &&
        );

CREATE TABLE quadra_foto (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quadra_id      BIGINT      NOT NULL REFERENCES quadra (id),
    chave          TEXT        NOT NULL UNIQUE,
    tipo_conteudo  TEXT        NOT NULL,
    criada_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_quadra_foto_quadra ON quadra_foto (quadra_id);

INSERT INTO esporte (nome) VALUES
    ('Basquete'),
    ('Beach tennis'),
    ('Futebol society'),
    ('Futevôlei'),
    ('Futsal'),
    ('Padel'),
    ('Tênis'),
    ('Vôlei'),
    ('Vôlei de praia');
