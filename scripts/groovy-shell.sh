#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

exec ${PLUGIN_TOOLS_GROOVY_DIR}/bin/groovysh
