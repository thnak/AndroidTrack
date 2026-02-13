#!/bin/bash
mkdir -p gradle/wrapper
cd gradle/wrapper
wget https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
wget https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties
cd ../..
cat > gradlew << 'GRADLEW'
#!/bin/sh
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
GRADLEW
chmod +x gradlew
