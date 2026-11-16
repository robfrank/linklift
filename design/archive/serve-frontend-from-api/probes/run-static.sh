#!/bin/sh
# Shared runner: builds the project's runtime classpath and runs StaticProbe in the given mode.
set -eu
mode="$1"
cp_file="$(pwd)/cp.txt"
"$PA_REPO/mvnw" -q -f "$PA_REPO/pom.xml" dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile="$cp_file" >/dev/null
echo "versions: javalin $(grep -o '<javalin.version>[^<]*' "$PA_REPO/pom.xml" | cut -d'>' -f2); $(java -version 2>&1 | head -1)"
exec java -cp "$(cat "$cp_file")" "$PA_DELTA/probes/StaticProbe.java" "$mode"
