#!/bin/bash

#
# Wrapper script needs improvement!
#
# @author Andre van Hoorn

BINDIR=$(dirname $0)

JAVAARGS=-Dlog4j.configuration=${BINDIR}/log4j.properties
MAINCLASSNAME=kieker.tools.logReplayer.FilesystemLogReplayerStarter
CLASSPATH=$(ls lib/*.jar | tr "\n" ":")$(ls dist/*.jar | tr "\n" ":")

java ${JAVAARGS} -cp "${CLASSPATH}" ${MAINCLASSNAME} $*