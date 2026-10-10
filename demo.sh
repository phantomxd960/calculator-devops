#!/usr/bin/env bash
# Live-demo self-check for the Calculator CI/CD stack (run from WSL/Linux after `docker compose up -d`)
set -u
BASE=${BASE:-http://localhost}
IMAGE=${DOCKER_IMAGE:-vijayvignesh960/calculator-api}:${IMAGE_TAG:-latest}
fail=0

step() { printf '\n\033[1;33m[%s]\033[0m\n' "$1"; }

check() {  # name url expected_status
    local body code
    body=$(curl -s -w '\n%{http_code}' "$2")
    code=${body##*$'\n'}; body=${body%$'\n'*}
    if [ "$code" = "$3" ]; then
        printf '\033[32mPASS\033[0m %-28s %s -> %s\n      %s\n' "$1" "$2" "$code" "$body"
    else
        printf '\033[31mFAIL\033[0m %-28s %s -> %s (expected %s)\n      %s\n' "$1" "$2" "$code" "$3" "$body"
        fail=1
    fi
}

step "1. Running containers"
docker compose ps

step "2. Container runs as non-root user"
docker run --rm --entrypoint whoami "$IMAGE"

step "3. Image size (multi-stage build)"
docker images "${IMAGE%:*}"

step "4. API endpoints via Nginx reverse proxy"
check "Health"            "$BASE/actuator/health"                200
check "Add 25+15"         "$BASE/api/v1/add?a=25&b=15"           200
check "Subtract 100-35"   "$BASE/api/v1/subtract?a=100&b=35"     200
check "Multiply 7*8"      "$BASE/api/v1/multiply?a=7&b=8"        200
check "Divide 84/4"       "$BASE/api/v1/divide?a=84&b=4"         200

step "5. Validation and error responses"
check "Divide by zero"    "$BASE/api/v1/divide?a=10&b=0"         400
check "Non-numeric input" "$BASE/api/v1/add?a=hello&b=5"         400
check "Missing parameter" "$BASE/api/v1/add?a=50"                400
check "Unknown endpoint"  "$BASE/api/v1/power"                   404

[ $fail -eq 0 ] && echo -e "\nAll checks passed." || { echo -e "\nSome checks failed."; exit 1; }
