-- Bloqueio de horário pelo dono: ocupa a agenda como uma reserva, com o motivo em cliente_nome
ALTER TABLE reserva
    DROP CONSTRAINT reserva_status_check,
    ADD CONSTRAINT reserva_status_check
        CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA', 'FALTOU', 'BLOQUEADA'));

CREATE INDEX idx_reserva_quadra_inicio ON reserva (quadra_id, inicio);
