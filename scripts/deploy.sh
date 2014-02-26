#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

# safe guard from empty env
[ -n "$DEV_CY3_BUNDLE_DIR" ] || exit 1

# create bundle dir
if [ ! -d "$DEV_CY3_BUNDLE_DIR" ]; then
    if [ -e "$DEV_CY3_BUNDLE_DIR" ]; then
        rm "$DEV_CY3_BUNDLE_DIR"
    fi
    mkdir -p "$DEV_CY3_BUNDLE_DIR"
fi

# copy plugin dependencies
for lib in `find "$DEV_LIBS_DIR" -name "*.jar" -not -path "*/test/*" -not -path "*/provided/*"`; do
    cp "$lib" "$DEV_CY3_BUNDLE_DIR"
done
# copy plugin
for lib in `find "$DEV_MODULES_DIR" -path "*/build/libs/*.jar"`; do
    cp "$lib" "$DEV_CY3_BUNDLE_DIR"
done
