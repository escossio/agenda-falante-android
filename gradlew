#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

DIR="$(cd "$(dirname "$0")" && pwd)"

APP_BASE_NAME=${0##*/}
APP_HOME=$(cd "${DIR}" >/dev/null && pwd -P)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/java}
if [ -z "$JAVA_CMD" ]; then
  JAVA_CMD=java
fi

if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
  echo "ERROR: Java not found. Set JAVA_HOME or install a JDK." >&2
  exit 1
fi

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
