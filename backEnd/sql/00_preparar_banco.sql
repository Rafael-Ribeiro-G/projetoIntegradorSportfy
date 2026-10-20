-- =============================================================
-- Dá ao usuário da API (back_sportfy) acesso às tabelas do banco do Sportfy.
-- Quem roda: o responsável pelo banco, como superusuário, UMA VEZ:
--
--   sudo -u postgres psql -d sportfy -f sql/00_preparar_banco.sql
--
-- Pode rodar de novo sem problema (não apaga nada, não muda estrutura).
-- Sem "-v usuario=...", usa back_sportfy. Sem "-v schema=...", usa public.
--
-- O que libera: ler e gravar DADOS (SELECT, INSERT, UPDATE, DELETE) nas tabelas.
-- O que NÃO libera: criar, alterar ou apagar tabelas. A estrutura do banco
-- continua sendo responsabilidade do responsável pelo banco.
-- =============================================================
\set ON_ERROR_STOP on

\if :{?usuario}
\else
  \set usuario back_sportfy
\endif
\if :{?schema}
\else
  \set schema public
\endif

\echo '>> Banco:' :DBNAME '| Schema:' :schema '| Usuário da API:' :usuario

SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'usuario') AS usuario_existe \gset
\if :usuario_existe
  \echo '>> Usuário encontrado.'
\else
  \echo '!! O usuário' :usuario 'não existe. Crie antes com:'
  \echo '   CREATE USER' :usuario 'WITH PASSWORD ''senha-forte'';'
  \quit
\endif

GRANT CONNECT ON DATABASE :"DBNAME" TO :"usuario";
GRANT USAGE ON SCHEMA :"schema" TO :"usuario";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA :"schema" TO :"usuario";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA :"schema" TO :"usuario";

-- tabelas criadas no futuro pelo dono atual do schema também ficam liberadas
SELECT nspowner::regrole::text AS dono_schema FROM pg_namespace WHERE nspname = :'schema' \gset
ALTER DEFAULT PRIVILEGES FOR ROLE :"dono_schema" IN SCHEMA :"schema"
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"usuario";
ALTER DEFAULT PRIVILEGES FOR ROLE :"dono_schema" IN SCHEMA :"schema"
    GRANT USAGE, SELECT ON SEQUENCES TO :"usuario";

\echo '>> Pronto. Confira com: bash scripts/verificar-servidor.sh'
