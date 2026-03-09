#!/bin/sh
# -----------------------------------------------------------------------------
# Maven Wrapper Script
# -----------------------------------------------------------------------------

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

# Use the java command from JAVA_HOME if set
if [ -n "$JAVA_HOME" ] ; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Download maven-wrapper.jar if not present
wrapperJarPath="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapperJarPath" ]; then
    echo "Downloading maven-wrapper.jar..."
    mkdir -p "$APP_HOME/.mvn/wrapper"
    curl -o "$wrapperJarPath" https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
fi

# Download Maven distribution if not present
mavenHome="$APP_HOME/.mvn/wrapper/apache-maven-3.9.6"
if [ ! -d "$mavenHome" ]; then
    echo "Downloading Maven 3.9.6..."
    mkdir -p "$APP_HOME/.mvn/wrapper"
    curl -o "$APP_HOME/.mvn/wrapper/maven.tar.gz" https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz
    tar -xzf "$APP_HOME/.mvn/wrapper/maven.tar.gz" -C "$APP_HOME/.mvn/wrapper"
    mv "$APP_HOME/.mvn/wrapper/apache-maven-3.9.6" "$mavenHome"
    rm "$APP_HOME/.mvn/wrapper/maven.tar.gz"
fi

# Build the command line
exec "$JAVA_CMD" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$wrapperJarPath" \
  "-Dmaven.home=$mavenHome" \
  "-Dmaven.multiModuleProjectDirectory=$APP_HOME" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
