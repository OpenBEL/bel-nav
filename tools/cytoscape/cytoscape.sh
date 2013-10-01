#!/bin/bash
#
# Run cytoscape from a jar file
# This script is a UNIX-only (i.e. Linux, Mac OS, etc.) version
#-------------------------------------------------------------------------------

DEBUG_PORT=12345

script_path="$(dirname -- $0)"
if [ -h $0 ]; then
	link="$(readlink $0)"
	script_path="$(dirname -- $link)"
fi

export JAVA_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${DEBUG_PORT}"
if [ `uname` = "Darwin" ]; then
	CYTOSCAPE_MAC_OPTS="-Xdock:icon=cytoscape_logo_512.png"
fi

#vm_options_path=$HOME/.cytoscape
vm_options_path=$script_path

# Attempt to generate Cytoscape.vmoptions if it doesn't exist!
if [ ! -e "$vm_options_path/Cytoscape.vmoptions"  -a  -x "$script_path/gen_vmoptions.sh" ]; then
    "$script_path/gen_vmoptions.sh"
fi

if [ -z "$DEV_DIR" ]; then
    . ../env.sh
fi
export JAVA_OPTS="-Xmx1550M -XX:MaxPermSize=512M"
export JAVA_OPTS="$JAVA_OPTS -Dplugin.cy3.work.dir=$DEV_CY3_WORK_DIR"
export JAVA_OPTS="$JAVA_OPTS -Dplugin.cy3.bundle.dir=$DEV_CY3_BUNDLE_DIR"
export JAVA_OPTS="$JAVA_OPTS -Dplugin.cy3.log.file=$DEV_CY3_LOG_FILE"
if [ -r $vm_options_path/Cytoscape.vmoptions ]; then
    JAVA_OPTS="$JAVA_OPTS `cat $vm_options_path/Cytoscape.vmoptions`"
fi

# The Cytoscape home directory contains the "framework" directory
# and this script.
CYTOSCAPE_HOME_REL=$script_path
CYTOSCAPE_HOME_ABS=`cd "$CYTOSCAPE_HOME_REL"; pwd`

PWD=$(pwd) 
# The user working directory needs to be explecitly set in -Duser.dir to current
# working directory since KARAF changes it to the framework directory. There
# might unforeseeable problems with this since the reason for KARAF setting the 
# working directory to framework is not known.
export KARAF_OPTS=-Xms128M\ -Dcom.sun.management.jmxremote\ -Duser.dir="$PWD"\ -Dcytoscape.home="$CYTOSCAPE_HOME_ABS"\ -splash:CytoscapeSplashScreen.png\ "$CYTOSCAPE_MAC_OPTS"

export KARAF_DATA="${DEV_CY3_DATA_DIR}"
mkdir -p "${KARAF_DATA}/tmp"

$script_path/framework/bin/karaf "$@"
