#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

# copy plugin dependencies
for lib in `find $PLUGIN_LIBS_DIR -name *\.jar -not -path "*/test/*" -not -path "*/provided/*"`; do
    cp "$lib" "$PLUGIN_CY3_BUNDLE_DIR"
done
# copy plugin
for lib in `find $PLUGIN_MODULES_DIR -path "*/build/libs/*.jar"`; do
    cp "$lib" "$PLUGIN_CY3_BUNDLE_DIR"
done
