-- Empresas novas só aparecem ao público depois de aprovadas. As que já existem continuam visíveis.
ALTER TABLE empresa ADD COLUMN aprovada BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE empresa SET aprovada = TRUE;
