-- Localização da empresa para a busca por proximidade. Empresas já existentes ficam sem coordenadas até o dono marcar no mapa.
ALTER TABLE empresa
    ADD COLUMN latitude  NUMERIC(9,6) CHECK (latitude BETWEEN -90 AND 90),
    ADD COLUMN longitude NUMERIC(9,6) CHECK (longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT empresa_coordenadas_completas CHECK ((latitude IS NULL) = (longitude IS NULL));
