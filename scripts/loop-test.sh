#!/usr/bin/env bash
shopt -s expand_aliases
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

while inotifywait -qq -r -e modify "$PLUGIN_SOURCE_DIR" -e modify "$PLUGIN_TEST_DIR"; do
    gradle -q test
    if [ "$?" != "0" ]; then 
        notify-send -t 3000 "Failed compilation.\n\n$RESULT"
        echo "$RESULT" 2>&1
    else
        notify-send -t 3000 "Success, tests passed."
    fi
done
