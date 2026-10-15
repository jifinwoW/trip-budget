#!/bin/sh

#
# Gradle start script for TripBudget.
# Uses a local .tools/gradle distribution when present; otherwise the wrapper JAR.
#

APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Prefer a known local JDK if JAVA_HOME is unset
if [ -z "$JAVA_HOME" ]; then
  for candidate in \
    "$HOME/.vscode/extensions"/sonarsource.sonarlint-vscode-*/jre/*/
  do
    if [ -x "${candidate}bin/java" ]; then
      JAVA_HOME=$(cd "$candidate" && pwd)
      break
    fi
  done
fi

if [ -n "$JAVA_HOME" ]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ -x "$APP_HOME/.tools/gradle-8.11.1/bin/gradle" ]; then
  exec "$APP_HOME/.tools/gradle-8.11.1/bin/gradle" "$@"
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar missing. Open the project in Android Studio to generate the wrapper." >&2
  exit 1
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
