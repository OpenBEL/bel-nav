#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# -- Directories --
export DEV_DIR="${DEV_DIR:=$DIR}"
export DEV_LIBS_DIR="${DEV_LIBS_DIR:=$DEV_DIR/libs}"
export DEV_MODULES_DIR="${DEV_MODULES_DIR:=$DEV_DIR/modules}"
export DEV_SCRIPTS_DIR="${DEV_SCRIPTS_DIR:=$DEV_DIR/scripts}"
export DEV_TOOLS_DIR="${DEV_TOOLS_DIR:=$DEV_DIR/tools}"
export DEV_TOOLS_GROOVY_DIR="${DEV_TOOLS_GROOVY_DIR:=$DEV_TOOLS_DIR/groovy}"
export DEV_CY3_DIR="${DEV_CY3_DIR:=$DEV_TOOLS_DIR/cytoscape}"
export DEV_CY3_LIBS_DIR="${DEV_CY3_LIBS_DIR:=$DEV_CY3_DIR/framework/system}"
export DEV_CY3_WORK_DIR="${DEV_CY3_WORK_DIR:=$DEV_CY3_DIR/work}"
export DEV_CY3_BUNDLE_DIR="${DEV_CY3_BUNDLE_DIR:=$DEV_CY3_WORK_DIR/bundles}"
export DEV_CY3_DATA_DIR="${DEV_CY3_DATA_DIR:=$DEV_CY3_WORK_DIR/data}"

# -- Files --
export DEV_CY3_LOG_FILE="${DEV_CY3_LOG_FILE:=$DEV_CY3_WORK_DIR/cytoscape.log}"

# -- Aliases --
alias gradle="${DEV_SCRIPTS_DIR}/gradlew --daemon"
