#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)
TEMP_REPO=$(mktemp -d "${TMPDIR:-/tmp}/modelmatrix4j-consumer.XXXXXX")
trap 'rm -rf "$TEMP_REPO"' EXIT

REVISION_ARGS=()
if [[ -n "${MODELMATRIX_REVISION:-}" ]]; then
    REVISION_ARGS+=("-Drevision=$MODELMATRIX_REVISION")
fi

cd "$REPO_ROOT"

PROJECT_VERSION=$(./mvnw -q -DforceStdout -Dstyle.color=never \
    "${REVISION_ARGS[@]}" \
    -Dmaven.repo.local="$TEMP_REPO" \
    help:evaluate -Dexpression=project.version)

echo "Installing ModelMatrix4J $PROJECT_VERSION artifacts into isolated Maven repository: $TEMP_REPO"
./mvnw -B "${REVISION_ARGS[@]}" clean install -DskipTests -Dmaven.repo.local="$TEMP_REPO"

# Surefire auto-selects this provider at test execution time, so go-offline does not discover it.
./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.9.0:get \
    -Dartifact=org.apache.maven.surefire:surefire-junit-platform:3.5.5 \
    -Dmaven.repo.local="$TEMP_REPO"

prepare_consumer() {
    local consumer=$1
    echo "Preparing standalone consumer dependencies: $consumer"
    ./mvnw -B -f "$REPO_ROOT/consumer-tests/$consumer/pom.xml" \
        org.apache.maven.plugins:maven-dependency-plugin:3.9.0:go-offline \
        -Dmaven.repo.local="$TEMP_REPO" \
        -Dmodelmatrix.version="$PROJECT_VERSION"
}

verify_consumer() {
    local consumer=$1
    rm -rf "$REPO_ROOT/consumer-tests/$consumer/target"
    echo "Verifying standalone consumer offline: $consumer"
    ./mvnw -B -o -f "$REPO_ROOT/consumer-tests/$consumer/pom.xml" test \
        -Dmaven.repo.local="$TEMP_REPO" \
        -Dmodelmatrix.version="$PROJECT_VERSION"
}

consumers=(
    junit-consumer
    report-consumer
    spring-ai-consumer
    structured-consumer
    tool-consumer
    rag-consumer
    mcp-consumer
    public-api-surface
)

for consumer in "${consumers[@]}"; do
    prepare_consumer "$consumer"
done

for consumer in "${consumers[@]}"; do
    verify_consumer "$consumer"
done

REPORT_TARGET="$REPO_ROOT/consumer-tests/report-consumer/target"
test -s "$REPORT_TARGET/modelmatrix-report.json"
test -s "$REPORT_TARGET/modelmatrix-report.txt"
echo "External consumer reports: $REPORT_TARGET/modelmatrix-report.{json,txt}"

echo "External consumer verification: PASS"
