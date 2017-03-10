## build periodically was 55 * * * *

cd restreplay

mvn install

REPORTSDIR="./reports"
echo "REPORTSDIR: $REPORTSDIR"
echo "BUILD_URL: $BUILD_URL "

echo "sourcing java-options in las/restreplay/"
. ./java-options

echo "RESTREPLAY_JAR: $RESTREPLAY_JAR"
echo "JAVA_OPTS: $JAVA_OPTS"


java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -selftest -reports "$REPORTSDIR"

java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env ilp-ppe -master jira/am757/las-master-scoring-am757.xml -reports "$REPORTSDIR"

java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env ilp-stg -master jira/am757/las-master-scoring-am757.xml -reports "$REPORTSDIR"

java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env ilp-stgb -master jira/am757/las-master-scoring-am757.xml -reports "$REPORTSDIR"

java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env ilp-stgc -master jira/am757/las-master-scoring-am757.xml -reports "$REPORTSDIR"

java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env ilp-qa   -master jira/am757/las-master-scoring-am757.xml -reports "$REPORTSDIR"

 
##java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env dev -master las-master.xml -reports "$REPORTSDIR"

##java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env qa -master las-master.xml -reports "$REPORTSDIR"

##java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env stage -master las-master.xml -reports "$REPORTSDIR"

## oct 3, 2016 ## java $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -env prod -master las-master.xml -reports "$REPORTSDIR"



##/var/lib/jenkins/.laramie/upload-RestReplay-report.bash
##this script is relative to the source directory we are in:
bin/upload-RestReplay-report.bash

