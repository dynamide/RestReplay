. ./java-options

mvn -q -o -DskipTests=true install

java $JAVA_OPTS -jar $RESTREPLAY_JAR \
   -master _self_test/master-selftest-debug.xml \
   -testdir ./src/main/resources/restreplay \
   -selftest \
   -reports ./reports
