#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# -- Directories --
export PLUGIN_DIR="${PLUGIN_DIR:=$DIR}"
export PLUGIN_LIBS_DIR="${PLUGIN_LIBS_DIR:=$PLUGIN_DIR/libs}"
export PLUGIN_BUILD_LIBS_DIR="${PLUGIN_BUILD_LIBS_DIR:=$PLUGIN_DIR/build/libs}"
export PLUGIN_TOOLS_DIR="${PLUGIN_TOOLS_DIR:=$PLUGIN_DIR/tools}"
export PLUGIN_CY3_DIR="${PLUGIN_CY3_DIR:=$PLUGIN_TOOLS_DIR/cytoscape}"
export PLUGIN_CY3_WORK_DIR="${PLUGIN_CY3_WORK_DIR:=$PLUGIN_CY3_DIR/work}"
export PLUGIN_CY3_BUNDLE_DIR="${PLUGIN_CY3_BUNDLE_DIR:=$PLUGIN_CY3_WORK_DIR/bundles}"
export PLUGIN_CY3_DATA_DIR="${PLUGIN_CY3_DATA_DIR:=$PLUGIN_CY3_WORK_DIR/data}"

# -- Files --
export PLUGIN_CY3_LOG_FILE="${PLUGIN_CY3_LOG_FILE:=$PLUGIN_CY3_WORK_DIR/cytoscape.log}"
