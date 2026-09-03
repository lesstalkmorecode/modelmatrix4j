#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
GROUP_PATH="io/github/lesstalkmorecode"
SOURCE_MODE="${MODELMATRIX_REPRO_SOURCE:-head}"
EXPECTED_VERSION="${MODELMATRIX_EXPECTED_VERSION:-}"
REVISION_ARGS=()
if [[ -n "${MODELMATRIX_REVISION:-}" ]]; then
    REVISION_ARGS+=("-Drevision=$MODELMATRIX_REVISION")
fi
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/modelmatrix4j-repro.XXXXXX")
trap 'rm -rf "$WORK_DIR"' EXIT

BUILD_A="$WORK_DIR/build-a"
BUILD_B="$WORK_DIR/build-b"
REPO_A="$WORK_DIR/repository-a"
REPO_B="$WORK_DIR/repository-b"
PATHS_A="$WORK_DIR/artifacts-a.paths"
PATHS_B="$WORK_DIR/artifacts-b.paths"
MANIFEST_A="$WORK_DIR/artifacts-a.sha256"
MANIFEST_B="$WORK_DIR/artifacts-b.sha256"

copy_tracked_working_tree() {
    local destination=$1
    local relative source target

    while IFS= read -r -d '' relative; do
        source="$ROOT_DIR/$relative"
        if [[ ! -e "$source" && ! -L "$source" ]]; then
            continue
        fi
        target="$destination/$relative"
        mkdir -p "$(dirname "$target")"
        cp -a -- "$source" "$target"
    done < <(git -C "$ROOT_DIR" ls-files -z)
}

copy_checkout() {
    local destination=$1

    case "$SOURCE_MODE" in
        head)
            git -C "$ROOT_DIR" archive --format=tar HEAD | tar -xf - -C "$destination"
            ;;
        working-tree)
            copy_tracked_working_tree "$destination"
            ;;
        *)
            echo "Unsupported MODELMATRIX_REPRO_SOURCE: $SOURCE_MODE" >&2
            exit 1
            ;;
    esac
}

assert_version() {
    local checkout=$1

    if [[ -z "$EXPECTED_VERSION" ]]; then
        return
    fi

    local actual
    actual=$(cd "$checkout" && ./mvnw -q -DforceStdout -Dstyle.color=never \
        "${REVISION_ARGS[@]}" help:evaluate -Dexpression=project.version)
    if [[ "$actual" != "$EXPECTED_VERSION" ]]; then
        echo "Expected reproducibility source version $EXPECTED_VERSION but found $actual" >&2
        exit 1
    fi
}

build_once() {
    local checkout=$1
    local repository=$2

    assert_version "$checkout"
    (
        cd "$checkout"
        ./mvnw -B "${REVISION_ARGS[@]}" -DskipTests -Dmaven.repo.local="$repository" clean install
    )
}

collect_artifacts() {
    local repository=$1
    local paths=$2
    local manifest=$3
    local group_dir="$repository/$GROUP_PATH"

    if [[ ! -d "$group_dir" ]]; then
        echo "Missing installed ModelMatrix4J group: $group_dir" >&2
        exit 1
    fi

    (
        cd "$group_dir"
        find . -type f \( -name '*.jar' -o -name '*.pom' \) -print \
            | sed 's#^./##' \
            | LC_ALL=C sort > "$paths"
    )

    if [[ ! -s "$paths" ]]; then
        echo "No ModelMatrix4J build artifacts were collected from $group_dir" >&2
        exit 1
    fi

    local jar_count source_count javadoc_count pom_count parent_pom_count
    jar_count=$(grep -Ec '\.jar$' "$paths" || true)
    source_count=$(grep -Ec -- '-sources\.jar$' "$paths" || true)
    javadoc_count=$(grep -Ec -- '-javadoc\.jar$' "$paths" || true)
    pom_count=$(grep -Ec '\.pom$' "$paths" || true)
    parent_pom_count=$(grep -Ec '^modelmatrix4j-parent/[^/]+/modelmatrix4j-parent-[^/]+\.pom$' "$paths" || true)

    if (( jar_count == 0 )); then
        echo "No main/source/Javadoc JAR artifacts were collected" >&2
        exit 1
    fi
    if (( source_count == 0 )); then
        echo "No sources JAR artifacts were collected" >&2
        exit 1
    fi
    if (( javadoc_count == 0 )); then
        echo "No Javadoc JAR artifacts were collected" >&2
        exit 1
    fi
    if (( pom_count == 0 )); then
        echo "No POM artifacts were collected" >&2
        exit 1
    fi
    if (( parent_pom_count != 1 )); then
        echo "Expected exactly one installed modelmatrix4j-parent POM, found $parent_pom_count" >&2
        exit 1
    fi

    : > "$manifest"
    while IFS= read -r relative; do
        sha256sum "$group_dir/$relative" | awk -v name="$relative" '{print $1 "  " name}'
    done < "$paths" > "$manifest"
}

mkdir -p "$BUILD_A" "$BUILD_B"
copy_checkout "$BUILD_A"
copy_checkout "$BUILD_B"

build_once "$BUILD_A" "$REPO_A"
build_once "$BUILD_B" "$REPO_B"

collect_artifacts "$REPO_A" "$PATHS_A" "$MANIFEST_A"
collect_artifacts "$REPO_B" "$PATHS_B" "$MANIFEST_B"

if ! diff -u "$PATHS_A" "$PATHS_B"; then
    echo >&2
    echo "Reproducibility verification FAILED: installed artifact paths differ." >&2
    exit 1
fi

if ! diff -u "$MANIFEST_A" "$MANIFEST_B"; then
    echo >&2
    echo "Reproducibility verification FAILED: artifact hashes differ." >&2
    exit 1
fi

echo "Compared ModelMatrix4J build artifacts from source mode: $SOURCE_MODE"
cat "$MANIFEST_A"
echo
echo "Reproducibility verification: PASS"
