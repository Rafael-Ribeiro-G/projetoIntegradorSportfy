-- =============================================================
-- Prepara o PostgreSQL do servidor para a API do Sportfy.
-- Rodar UMA VEZ, como superusuário, na pasta do projeto:
--
--   sudo -u postgres psql -v db=NOME_DO_BANCO -f sql/00_preparar_banco.sql
--
-- Pode rodar de novo sem problema (não apaga nada).
-- Sem "-v db=...", usa o banco "sportfy". O usuário padrão é back_sportfy
-- (mude com -v usuario=outro).
--
-- O que faz:
--   1. confere se o usuário da aplicação existe (a senha NÃO fica aqui);
--   2. cria o banco se ainda não existir, com esse usuário como dono;
--   3. garante que o usuário pode conectar e criar schemas no banco.
-- As TABELAS não são criadas aqui: o Flyway cria tudo, no schema "sportfy",
-- na primeira vez que a API sobe.
-- =============================================================
\set ON_ERROR_STOP on

\if :{?db}
\else
  \set db sportfy
\endif
\if :{?usuario}
\else
  \set usuario back_sportfy
\endif

\echo '>> Banco:' :db '| Usuário da aplicação:' :usuario

-- 1. usuário precisa existir (quem cria é o administrador do servidor, com a senha dele)
SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'usuario') AS usuario_existe \gset
\if :usuario_existe
  \echo '>> Usuário encontrado.'
\else
  \echo '!! O usuário' :usuario 'não existe. Crie antes com:'
  \echo '   CREATE USER' :usuario 'WITH PASSWORD ''senha-forte'';'
  \quit
\endif

-- 2. cria o banco (se não existir) com o usuário da aplicação como dono
SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db') AS banco_existe \gset
\if :banco_existe
  \echo '>> Banco já existe; mantendo o dono atual.'
\else
  SELECT format('CREATE DATABASE %I OWNER %I ENCODING %L TEMPLATE template0', :'db', :'usuario', 'UTF8') \gexec
  \echo '>> Banco criado.'
\endif

-- 3. permissões: conectar e criar o schema "sportfy" (onde o Flyway coloca as tabelas)
GRANT CONNECT, CREATE, TEMPORARY ON DATABASE :"db" TO :"usuario";

\echo '>> Pronto. Agora preencha o .env e rode: bash scripts/verificar-servidor.sh'
