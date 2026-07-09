#!/usr/bin/env bash
set -euo pipefail

# Trigger a JitPack build for Libreh/WorldReset at the configured version and
# wait for the worldreset-api artifact to become available. Required because the
# build depends on me.libreh:worldreset-api which is not published elsewhere.
#
# Usage: jitpack-build.sh <version>
# Reads the version from gradle.properties when called without arguments.

if [[ $# -lt 1 ]]; then
    version=$(grep '^worldreset_api_version=' gradle.properties | cut -d= -f2)
else
    version=$1
fi

version_enc=$(printf '%s' "$version" | sed 's/+/%2B/g')
pom_url="https://jitpack.io/com/github/Libreh/WorldReset/worldreset-api/${version_enc}/worldreset-api-${version_enc}.pom"
build_url="https://jitpack.io/com/github/Libreh/WorldReset/${version_enc}/build.log"

echo "Triggering JitPack build for Libreh/WorldReset ${version}"
curl -fsSL --retry 3 --retry-delay 5 "$build_url" -o /dev/null || true

for attempt in {1..30}; do
    if curl -fsSL --retry 3 --retry-delay 2 -o /dev/null "$pom_url"; then
        echo "JitPack artifact available after ${attempt} attempt(s)"
        exit 0
    fi
    echo "JitPack artifact not ready (attempt ${attempt}/30), waiting 20s"
    sleep 20
done

echo "JitPack build for Libreh/WorldReset ${version} did not finish in time" >&2
exit 1
