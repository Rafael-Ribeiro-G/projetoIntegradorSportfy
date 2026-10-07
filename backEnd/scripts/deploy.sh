#!/usr/bin/env bash
# =============================================================
# Sobe (ou atualiza) a API do Sportfy no servidor.
# Rodar no servidor, na pasta do projeto:   bash scripts/deploy.sh
#
# 1. confere o ambiente (scripts/verificar-servidor.sh)
# 2. gera a imagem e sobe o container (docker-compose.prod.yml)
# 3. espera o /actuator/health responder UP
# =============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.prod.yml)

bash ./scripts/verificar-servidor.sh || { echo "Deploy cancelado: corrija os erros acima."; exit 1; }

PORTA=$(grep -E '^[[:space:]]*SERVER_PORT=' .env | tail -1 | cut -d= -f2- | tr -d '"'"'"' \r' || true)
PORTA="${PORTA:-8080}"

echo ">> Gerando a imagem e subindo o container (a primeira vez demora alguns minutos)..."
"${COMPOSE[@]}" up -d --build

echo ">> Aguardando a API responder em http://localhost:$PORTA/actuator/health ..."
for _ in $(seq 1 60); do
    if curl -fsS "http://localhost:$PORTA/actuator/health" 2>/dev/null | grep -q '"UP"'; then
        echo
        echo ">> API no ar!"
        IP_TS=$(command -v tailscale >/dev/null 2>&1 && tailscale ip -4 2>/dev/null | head -1 || true)
        HOST="${IP_TS:-localhost}"
        echo "   Swagger: http://$HOST:$PORTA/swagger-ui.html"
        echo "   Logs:    docker compose -f docker-compose.prod.yml logs -f"
        exit 0
    fi
    printf '.'
    sleep 3
done

echo
echo "!! A API não respondeu em 3 minutos. Últimas linhas do log:"
"${COMPOSE[@]}" logs --tail=80
exit 1
