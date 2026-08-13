#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080/url-shortener}"

wait_for_server() {
    echo "Aguardando a aplicação iniciar em ${BASE_URL} ..."
    for _ in $(seq 1 60); do
        code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "${BASE_URL}/api/urls" \
            -H 'Content-Type: application/json' \
            -d '{"url":"https://example.com/wait"}' 2>/dev/null || true)
        if [ "$code" != "000" ] && [ "$code" != "404" ] && [ -n "$code" ]; then
            echo "Aplicação pronta (HTTP ${code})."
            return 0
        fi
        sleep 2
    done
    echo "ERRO: aplicação não respondeu após 120s." >&2
    return 1
}

smoke_shorten() {
    body=$(curl -s -X POST "${BASE_URL}/api/urls" \
        -H 'Content-Type: application/json' \
        -d '{"url":"https://example.com/smoke-test"}')

    code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "${BASE_URL}/api/urls" \
        -H 'Content-Type: application/json' \
        -d '{"url":"https://example.com/smoke-test"}')

    if [ "$code" != "201" ]; then
        echo "ERRO: POST /api/urls retornou HTTP ${code}: ${body}" >&2
        return 1
    fi

    short_url=$(echo "$body" | sed -E 's/.*"shortUrl":"([^"]+)".*/\1/')
    if [ -z "$short_url" ]; then
        echo "ERRO: shortUrl ausente na resposta: ${body}" >&2
        return 1
    fi
    echo "Smoke OK: criada ${short_url}"
}

smoke_redirect() {
    code=$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/r/inexistente")
    if [ "$code" != "404" ]; then
        echo "ERRO: GET /r/inexistente deveria retornar 404, veio HTTP ${code}" >&2
        return 1
    fi
    echo "Smoke OK: redirecionamento 404 para código inexistente"
}

wait_for_server
smoke_shorten
smoke_redirect