Doco: 
    doc/RestReplay.html

Building:
    The first time: 

        mvn install;

    After that, you can run without maven downloading the Internet each time:

        mvn -o -DskipTests=true install;



Running: 
  for a "qa" environment set up in the master file:

    mvn -o exec:java -DenvID=qa

  or for "dev" environment:

    mvn -o exec:java -DenvID=dev



Running with tests in some other project-based directory: 

    mvn -o exec:java -DenvID=local -DrestReplayBaseDir=/Users/vcrocla/src/RestReplay.pearson.basedir -DenvID=dev

so the restReplayBaseDir is a directory that has your test master control files, 
and where rest-replay-reports directory will appear.




================================================================================
  History
================================================================================    
Run maven locally: 
    http://jojovedder.blogspot.com/2009/04/running-maven-offline-using-local.html
    
I disabled tomcat tagonomy/dynamide authentication locally to test RestReplay.

2014-10-29: 
   I ran on the command line with:
   mvn -DskipTests exec:java  -DrestReplayBaseDir=/Users/vcrocla/src/RestReplay/src/main/resources/restreplay -DrestReplayMaster=dynamide-master.xml

   This is the command line that works in the IntelliJ IDEA project: 
     args:  -restReplayBaseDir /Users/vcrocla/src/RestReplay/src/main/resources/restreplay -restReplayMaster dynamide-master.xml
     workingdir: /Users/vcrocla/src/RestReplay
     mainclass: org.dynamide.restreplay.RestReplay
     use-classpath-of-module: org.dynamide.RestReplay





