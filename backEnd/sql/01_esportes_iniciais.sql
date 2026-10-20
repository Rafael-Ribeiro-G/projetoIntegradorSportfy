-- =============================================================
-- Cadastra os esportes básicos na tabela public.esportes (só os que ainda não existem).
-- Opcional: rode se a tabela estiver vazia. Quem roda: o responsável pelo banco.
--
--   sudo -u postgres psql -d sportfy < sql/01_esportes_iniciais.sql
-- =============================================================
INSERT INTO public.esportes (nome, quantidade_jogadores_padrao)
SELECT v.nome, v.qtd
FROM (VALUES
    ('Futebol', 22),
    ('Futsal', 10),
    ('Society', 14),
    ('Vôlei', 12),
    ('Vôlei de Praia', 4),
    ('Futevôlei', 4),
    ('Basquete', 10),
    ('Handebol', 14),
    ('Tênis', 4),
    ('Beach Tennis', 4),
    ('Corrida', 20),
    ('Ciclismo', 20)
) AS v(nome, qtd)
WHERE NOT EXISTS (SELECT 1 FROM public.esportes e WHERE lower(e.nome) = lower(v.nome));
