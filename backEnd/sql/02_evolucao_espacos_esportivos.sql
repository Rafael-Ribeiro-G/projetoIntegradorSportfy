-- =============================================================
-- Sportfy: evolução do banco para atividades além de quadras
-- (corrida no parque, treino na academia, caminhada, ciclismo...)
--
-- Quem roda: o responsável pelo banco (Enzo), conectado no banco "sportfy"
-- como admin_sportfy (recomendado) ou postgres:
--   pelo DBeaver: abrir o arquivo e executar tudo (Alt+X)
--   ou: psql -U admin_sportfy -d sportfy < sql/02_evolucao_espacos_esportivos.sql
--
-- O que faz (só ACRESCENTA; nenhuma tabela, coluna ou linha é apagada):
--   locais    + tipo (quadra, parque, academia, pista, rua, outro) + publico
--   partidas  + local_id, esporte_id, titulo, descricao, imagem_url
--             quadra_id passa a ser OPCIONAL
--             partidas que já existem recebem local e esporte da quadra delas
--   usuarios  + foto_url, bio
--   usuario_esportes (nova): esportes favoritos de cada usuário
--
-- Roda dentro de uma transação: se algo der errado, nada é aplicado.
-- Pode rodar de novo sem problema.
-- =============================================================
BEGIN;

-- ------------------------------------------------------------ locais
ALTER TABLE public.locais
    ADD COLUMN IF NOT EXISTS tipo varchar(30) NOT NULL DEFAULT 'outro',
    ADD COLUMN IF NOT EXISTS publico boolean NOT NULL DEFAULT true;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'locais_tipo_check') THEN
        ALTER TABLE public.locais ADD CONSTRAINT locais_tipo_check
            CHECK (tipo IN ('quadra', 'parque', 'academia', 'pista', 'rua', 'outro'));
    END IF;
END $$;

-- locais que já têm quadras cadastradas passam a ser do tipo "quadra"
UPDATE public.locais l SET tipo = 'quadra'
WHERE l.tipo = 'outro' AND EXISTS (SELECT 1 FROM public.quadras q WHERE q.local_id = l.id);

-- ------------------------------------------------------------ partidas
ALTER TABLE public.partidas
    ADD COLUMN IF NOT EXISTS local_id   uuid,
    ADD COLUMN IF NOT EXISTS esporte_id integer,
    ADD COLUMN IF NOT EXISTS titulo     varchar(100),
    ADD COLUMN IF NOT EXISTS descricao  text,
    ADD COLUMN IF NOT EXISTS imagem_url text;

ALTER TABLE public.partidas ALTER COLUMN quadra_id DROP NOT NULL;

-- partidas que já existem: local e esporte vêm da quadra
UPDATE public.partidas p
SET local_id = q.local_id, esporte_id = q.esporte_id
FROM public.quadras q
WHERE q.id = p.quadra_id AND (p.local_id IS NULL OR p.esporte_id IS NULL);

ALTER TABLE public.partidas
    ALTER COLUMN local_id   SET NOT NULL,
    ALTER COLUMN esporte_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'partidas_local_id_fkey') THEN
        ALTER TABLE public.partidas ADD CONSTRAINT partidas_local_id_fkey
            FOREIGN KEY (local_id) REFERENCES public.locais(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'partidas_esporte_id_fkey') THEN
        ALTER TABLE public.partidas ADD CONSTRAINT partidas_esporte_id_fkey
            FOREIGN KEY (esporte_id) REFERENCES public.esportes(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS partidas_local_id_idx      ON public.partidas (local_id);
CREATE INDEX IF NOT EXISTS partidas_esporte_id_idx    ON public.partidas (esporte_id);
CREATE INDEX IF NOT EXISTS partidas_data_hora_inicio_idx ON public.partidas (data_hora_inicio);

-- ------------------------------------------------------------ usuarios
ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS foto_url text,
    ADD COLUMN IF NOT EXISTS bio      varchar(500);

-- ------------------------------------------------------------ esportes favoritos
CREATE TABLE IF NOT EXISTS public.usuario_esportes (
    usuario_id uuid    NOT NULL REFERENCES public.usuarios(id) ON DELETE CASCADE,
    esporte_id integer NOT NULL REFERENCES public.esportes(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, esporte_id)
);

-- mesmo dono das outras tabelas (caso o script tenha sido rodado como postgres)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'admin_sportfy') THEN
        ALTER TABLE public.usuario_esportes OWNER TO admin_sportfy;
    END IF;
END $$;

-- a API (back_sportfy) lê e grava dados na tabela nova (sem poder alterar estrutura)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'back_sportfy') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.usuario_esportes TO back_sportfy;
    END IF;
END $$;

COMMIT;

-- ------------------------------------------------------------ conferência
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (table_name, column_name) IN (
      ('locais','tipo'), ('locais','publico'),
      ('partidas','local_id'), ('partidas','esporte_id'), ('partidas','quadra_id'),
      ('partidas','titulo'), ('partidas','descricao'), ('partidas','imagem_url'),
      ('usuarios','foto_url'), ('usuarios','bio'),
      ('usuario_esportes','usuario_id'), ('usuario_esportes','esporte_id'))
ORDER BY table_name, column_name;

SELECT count(*) AS partidas_sem_local FROM public.partidas WHERE local_id IS NULL;
