-- Esportes usados nos testes (ids fixos para facilitar).
INSERT INTO esportes (id, nome, quantidade_jogadores_padrao) VALUES
    (1, 'Futebol', 22),
    (2, 'Futsal', 10),
    (3, 'Vôlei', 12),
    (4, 'Tênis', 4),
    (5, 'Corrida', NULL);
ALTER TABLE esportes ALTER COLUMN id RESTART WITH 100;
