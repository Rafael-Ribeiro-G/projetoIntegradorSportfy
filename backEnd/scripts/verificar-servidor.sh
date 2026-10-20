#!/usr/bin/env bash
# =============================================================
# Confere se o servidor está pronto para subir a API do Sportfy.
# Rodar no servidor, na pasta do projeto:   bash scripts/verificar-servidor.sh
#
# Verifica: .env preenchido, conexão com o banco, se as tabelas do Enzo existem,
# se o usuário da API pode ler/gravar nelas, se há esportes, Docker e porta.
# Não altera nada. Sai com código 1 se houver algo a corrigir.
# =============================================================
set -u

ARQUIVO_ENV="${1:-.env}"
ERROS=0
AVISOS=0

ok()    { printf '  \033[32m[OK]\033[0m    %s\n' "$*"; }
aviso() { printf '  \033[33m[AVISO]\033[0m %s\n' "$*"; AVISOS=$((AVISOS + 1)); }
erro()  { printf '  \033[31m[ERRO]\033[0m  %s\n' "$*"; ERROS=$((ERROS + 1)); }
dica()  { printf '          -> %s\n' "$*"; }
titulo(){ printf '\n\033[1m%s\033[0m\n' "$*"; }

# ------------------------------------------------------------- .env
titulo "1. Arquivo $ARQUIVO_ENV"
declare -A ENV=()
if [[ ! -f "$ARQUIVO_ENV" ]]; then
    erro "Arquivo $ARQUIVO_ENV não encontrado."
    dica "cp .env.example .env && nano .env"
    exit 1
fi
# lê chave=valor sem executar o arquivo (senhas com @ % $ etc. ficam intactas)
while IFS= read -r linha || [[ -n "$linha" ]]; do
    linha="${linha%$'\r'}"
    [[ -z "${linha// }" || "$linha" =~ ^[[:space:]]*# || "$linha" != *=* ]] && continue
    chave="${linha%%=*}"; chave="${chave//[[:space:]]/}"
    valor="${linha#*=}"
    if [[ "$valor" =~ ^\"(.*)\"$ || "$valor" =~ ^\'(.*)\'$ ]]; then valor="${BASH_REMATCH[1]}"; fi
    ENV["$chave"]="$valor"
done < "$ARQUIVO_ENV"

for obrigatoria in DB_URL DB_USER DB_PASSWORD JWT_SECRET; do
    if [[ -z "${ENV[$obrigatoria]:-}" ]]; then
        erro "$obrigatoria não está preenchida."
    fi
done
JWT="${ENV[JWT_SECRET]:-}"
if [[ -n "$JWT" ]]; then
    if (( ${#JWT} < 32 )); then
        erro "JWT_SECRET tem ${#JWT} caracteres (mínimo 32)."
        dica "gere um com: openssl rand -base64 64"
    elif [[ "$JWT" == *cole-aqui* ]]; then
        erro "JWT_SECRET ainda é o texto de exemplo."
        dica "gere um com: openssl rand -base64 64"
    else
        ok "JWT_SECRET definido (${#JWT} caracteres)."
    fi
fi
if [[ "$(stat -c %a "$ARQUIVO_ENV" 2>/dev/null)" != "600" ]]; then
    aviso "$ARQUIVO_ENV pode ser lido por outros usuários do servidor."
    dica "chmod 600 $ARQUIVO_ENV"
fi

SCHEMA="${ENV[DB_SCHEMA]:-public}"
PORTA_API="${ENV[SERVER_PORT]:-8080}"

# ------------------------------------------------------------- banco
titulo "2. Banco de dados"
DB_URL="${ENV[DB_URL]:-}"
if [[ "$DB_URL" =~ ^jdbc:postgresql://([^/:?]+)(:([0-9]+))?/([^?]+) ]]; then
    DB_HOST="${BASH_REMATCH[1]}"
    DB_PORTA="${BASH_REMATCH[3]:-5432}"
    DB_NOME="${BASH_REMATCH[4]}"
    ok "DB_URL aponta para host=$DB_HOST porta=$DB_PORTA banco=$DB_NOME (schema das tabelas: $SCHEMA)."
else
    erro "DB_URL fora do formato jdbc:postgresql://host:porta/banco"
    DB_HOST=""
fi

if [[ -n "${DB_HOST:-}" && -n "${ENV[DB_USER]:-}" ]]; then
    if ! command -v psql >/dev/null 2>&1; then
        aviso "psql não encontrado; não deu para testar o banco daqui."
        dica "sudo apt install postgresql-client   (e rode este script de novo)"
    else
        export PGPASSWORD="${ENV[DB_PASSWORD]:-}"
        export PGCONNECT_TIMEOUT=5
        CONEXAO=(-h "$DB_HOST" -p "$DB_PORTA" -U "${ENV[DB_USER]}" -X -q -t -A -v ON_ERROR_STOP=1)

        if SAIDA=$(psql "${CONEXAO[@]}" -d "$DB_NOME" -c "select 1" 2>&1); then
            ok "Conectou como ${ENV[DB_USER]} no banco $DB_NOME."

            VERSAO=$(psql "${CONEXAO[@]}" -d "$DB_NOME" -c "select current_setting('server_version')" 2>/dev/null)
            ok "PostgreSQL $VERSAO."

            # tabelas que a API usa (estrutura criada pelo responsável do banco)
            TABELAS="usuarios esportes locais quadras partidas partida_participantes"
            FALTANDO=""; SEM_PERMISSAO=""
            for t in $TABELAS; do
                # esportes: a API só lê. Nas outras ela lê e grava.
                PRIVS="SELECT INSERT UPDATE DELETE"; [[ "$t" == "esportes" ]] && PRIVS="SELECT"
                R=$(psql "${CONEXAO[@]}" -d "$DB_NOME" -F '|' -v schema="$SCHEMA" -v tabela="$t" -v privs="$PRIVS" <<'SQL' 2>/dev/null
select to_regclass(format('%I.%I', :'schema', :'tabela')) is not null,
       coalesce((select bool_and(has_table_privilege(format('%I.%I', :'schema', :'tabela'), p))
                 from unnest(string_to_array(:'privs', ' ')) as p
                 where to_regclass(format('%I.%I', :'schema', :'tabela')) is not null), false);
SQL
)
                IFS='|' read -r EXISTE PODE <<< "$R"
                if [[ "$EXISTE" != "t" ]]; then FALTANDO="$FALTANDO $t";
                elif [[ "$PODE" != "t" ]]; then SEM_PERMISSAO="$SEM_PERMISSAO $t"; fi
            done

            if [[ -n "$FALTANDO" ]]; then
                erro "Tabela(s) não encontrada(s) no schema \"$SCHEMA\":$FALTANDO"
                if [[ "$SCHEMA" != "public" ]]; then
                    dica "as tabelas do Enzo ficam no schema public: no .env use DB_SCHEMA=public (ou apague a linha)."
                else
                    dica "a estrutura do banco é criada pelo responsável do banco (referência: sql/estrutura_banco_enzo.sql)."
                fi
            else
                ok "As 6 tabelas que a API usa existem no schema \"$SCHEMA\"."
            fi
            if [[ -n "$SEM_PERMISSAO" ]]; then
                erro "${ENV[DB_USER]} não pode ler/gravar em:$SEM_PERMISSAO"
                dica "o responsável do banco roda: sudo -u postgres psql -d $DB_NOME -f sql/00_preparar_banco.sql"
            elif [[ -z "$FALTANDO" ]]; then
                ok "${ENV[DB_USER]} pode ler e gravar dados nessas tabelas."
            fi

            if [[ -z "$FALTANDO" && -z "$SEM_PERMISSAO" ]]; then
                QTD_ESPORTES=$(psql "${CONEXAO[@]}" -d "$DB_NOME" -c "select count(*) from \"$SCHEMA\".esportes" 2>/dev/null)
                if [[ "${QTD_ESPORTES:-0}" == "0" ]]; then
                    aviso "A tabela esportes está vazia: não dá para cadastrar quadras nem partidas."
                    dica "o responsável do banco roda: sudo -u postgres psql -d $DB_NOME -f sql/01_esportes_iniciais.sql"
                else
                    ok "$QTD_ESPORTES esporte(s) cadastrado(s)."
                fi
            fi

            ANTIGO=$(psql "${CONEXAO[@]}" -d "$DB_NOME" -c \
                "select count(*) from pg_namespace where nspname = 'sportfy'" 2>/dev/null)
            if [[ "$SCHEMA" != "sportfy" && "${ANTIGO:-0}" != "0" ]]; then
                aviso "Existe o schema \"sportfy\", da versão anterior da API (tabelas próprias). Não é mais usado."
                dica "para apagar (só tem dados de teste): psql ... -c 'DROP SCHEMA sportfy CASCADE;'  (ver README)"
            fi
        else
            erro "Não conectou no banco $DB_NOME."
            printf '          %s\n' "$(echo "$SAIDA" | head -2)"
            if [[ "$SAIDA" == *"does not exist"* && "$SAIDA" == *database* ]] || [[ "$SAIDA" == *"não existe"* ]]; then
                BANCOS=$(psql "${CONEXAO[@]}" -d postgres -c \
                    "select string_agg(datname, ', ' order by datname) from pg_database where not datistemplate and has_database_privilege(datname, 'CONNECT')" 2>/dev/null)
                [[ -n "$BANCOS" ]] && dica "bancos que este usuário consegue acessar: $BANCOS"
                dica "corrija o nome no DB_URL (o banco do projeto se chama sportfy)."
            elif [[ "$SAIDA" == *"password authentication failed"* || "$SAIDA" == *"autenticação"* ]]; then
                dica "usuário ou senha errados no .env (DB_USER / DB_PASSWORD)."
            elif [[ "$SAIDA" == *"Connection refused"* || "$SAIDA" == *"timeout"* || "$SAIDA" == *"could not connect"* ]]; then
                dica "o Postgres não está ouvindo em $DB_HOST:$DB_PORTA. Se a API roda no mesmo servidor, use localhost no DB_URL."
                dica "status do serviço: sudo systemctl status postgresql"
            elif [[ "$SAIDA" == *"pg_hba.conf"* ]]; then
                dica "o pg_hba.conf não libera esse usuário/endereço. Com a API no servidor, use host=localhost."
            fi
        fi
        unset PGPASSWORD
    fi
fi

# ------------------------------------------------------------- docker
titulo "3. Docker"
if command -v docker >/dev/null 2>&1; then
    ok "Docker $(docker --version 2>/dev/null | awk '{print $3}' | tr -d ',') instalado."
    if docker compose version >/dev/null 2>&1; then
        ok "Docker Compose disponível."
    else
        erro "Docker Compose (plugin) não encontrado."
        dica "sudo apt install docker-compose-plugin"
    fi
    if ! docker info >/dev/null 2>&1; then
        erro "Sem permissão para usar o Docker com este usuário (ou o serviço está parado)."
        dica "sudo usermod -aG docker \$USER  (e faça login de novo)  |  sudo systemctl start docker"
    fi
else
    erro "Docker não está instalado."
    dica "curl -fsSL https://get.docker.com | sudo sh"
    dica "alternativa sem Docker: instalar JDK 21 e rodar o .jar (ver README)."
fi

# ------------------------------------------------------------- rede
titulo "4. Rede"
if command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE "[:.]${PORTA_API}\$"; then
    if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -qx sportfy-api; then
        ok "Porta $PORTA_API em uso pela própria API (sportfy-api já está rodando)."
    else
        erro "A porta $PORTA_API já está em uso por outro programa."
        dica "veja quem é: sudo ss -ltnp | grep :$PORTA_API   — ou mude SERVER_PORT no .env"
    fi
else
    ok "Porta $PORTA_API livre."
fi
if command -v tailscale >/dev/null 2>&1; then
    IP_TS=$(tailscale ip -4 2>/dev/null | head -1)
    [[ -n "$IP_TS" ]] && ok "Tailscale ativo: o time vai acessar http://$IP_TS:$PORTA_API/swagger-ui.html"
fi
ORIGENS="${ENV[CORS_ORIGENS]:-}"
[[ -z "$ORIGENS" ]] && aviso "CORS_ORIGENS vazio: o front no navegador será bloqueado." && dica "ex.: CORS_ORIGENS=http://localhost:3000,https://sportfy.vercel.app"

# ------------------------------------------------------------- resumo
titulo "Resultado"
if (( ERROS > 0 )); then
    printf '  \033[31m%d erro(s)\033[0m e %d aviso(s). Corrija os erros e rode de novo.\n\n' "$ERROS" "$AVISOS"
    exit 1
fi
printf '  \033[32mTudo pronto\033[0m (%d aviso(s)). Para subir: bash scripts/deploy.sh\n\n' "$AVISOS"
exit 0
