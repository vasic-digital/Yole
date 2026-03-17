#!/bin/bash
# Non-interactive SonarQube setup script
# Waits for SonarQube to be ready, changes default password, creates project token

set -e

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
MAX_WAIT=300
WAIT_INTERVAL=10

echo "Waiting for SonarQube to be ready at ${SONAR_URL}..."
elapsed=0
while [ $elapsed -lt $MAX_WAIT ]; do
    status=$(curl -s "${SONAR_URL}/api/system/status" 2>/dev/null | grep -o '"status":"[A-Z]*"' | grep -o '[A-Z]*' || true)
    if [ "$status" = "UP" ]; then
        echo "SonarQube is ready!"
        break
    fi
    echo "  Status: ${status:-not reachable} (${elapsed}s/${MAX_WAIT}s)"
    sleep $WAIT_INTERVAL
    elapsed=$((elapsed + WAIT_INTERVAL))
done

if [ "$status" != "UP" ]; then
    echo "ERROR: SonarQube did not become ready within ${MAX_WAIT}s"
    exit 1
fi

echo "Changing default admin password..."
curl -s -u admin:admin -X POST "${SONAR_URL}/api/users/change_password" \
    -d "login=admin&previousPassword=admin&password=yole-sonar-local" \
    && echo "  Password changed." || echo "  Password already changed or error."

echo "Creating project..."
curl -s -u admin:yole-sonar-local -X POST "${SONAR_URL}/api/projects/create" \
    -d "name=Yole&project=yole" \
    && echo "  Project created." || echo "  Project already exists or error."

echo "Generating analysis token..."
TOKEN=$(curl -s -u admin:yole-sonar-local -X POST "${SONAR_URL}/api/user_tokens/generate" \
    -d "name=yole-local-$(date +%Y%m%d)" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo "  Token generated. Use: -Dsonar.token=$TOKEN"
    echo "$TOKEN" > /tmp/sonar-token.txt
    echo "  Token saved to /tmp/sonar-token.txt"
else
    echo "  Could not generate token (may already exist)."
fi

echo "SonarQube setup complete!"
