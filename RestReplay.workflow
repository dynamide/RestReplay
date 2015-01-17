
=============================================================
Doco:
=============================================================
    doc/RestReplay.html

    cp doco to public site:
	    on git server,
        cd ~/git/RestReplay/doc
        git pull
        cp RestReplay.html ~/sites/dynamide.com/RestReplay/doc/index.html

    the new way:
    cp -r ~/git/RestReplayGithub/doc ~/sites/dynamide.com/RestReplay/doc
=============================================================
Building:
=============================================================
    The first time:

        mvn install

    After that, you can run without maven downloading the Internet each time:

        mvn -o -DskipTests=true install

    Remember that this creates the release jar, such as target/RestReplay-1.0.4-standalone.jar
      which should be copied to any deployed clients.

=============================================================
Push to public server
=============================================================
javadoc:
	mvn javadoc:jar
  scp to ~/sites/dynamide.com/RestReplay/javadoc/
  jar xvf RestReplay-javadoc.jar


=============================================================
Deploy to Revel/LAS:
=============================================================
   cd ~/src/RestReplay
   mvn -o -DskipTests install
   cd ~/ws/las.ws/las/restreplay
   ./c

=============================================================
Running:
=============================================================
For a "qa" environment set up in the master file:

    mvn -o exec:java -Denv=qa

 or for "dev" environment:

    mvn -o exec:java -Denv=dev

In your local, in bash you can define macros:

    alias b='mvn -o -DskipTests=true install'
    alias r='mvn -o exec:java -Denv=local'
    alias c='cp ~/src/RestReplay/target/RestReplay-1.0.4-standalone.jar ~/ws/las.ws/las/restreplay/lib'

Running with tests in some other project-based directory:

    mvn -o exec:java -Denv=local -Dtestdir=/Users/vcrocla/src/RestReplay.pearson.testdir -Denv=dev

so the testdir is a directory that has your test master control files,
and where rest-replay-reports directory will appear.



Another example, pointing your testdir and master to a directory in another location:
   java -jar lib/RestReplay-1.0.4.standalone.jar \
           -testdir /Users/vcrocla/ws/las.ws/las/restreplay/tests/ \
           -master las-master.xml


This is useful for debugging in IntelliJ:
  Program args:
     -testdir /Users/vcrocla/ws/las.ws/las/restreplay/tests/  -master las-master.xml
  Working Directory:
     /Users/vcrocla/src/RestReplay
  Main Class:
     org.dynamide.restreplay.RestReplay



This is how you run just one control file, and how in that control file, you can also run a testGroup,
or a single test within a testGroup:

		java -jar target/RestReplay-1.0.4-standalone.jar \
        -control _self_test/dynamide.xml \
        -testGroup login \
        -test dynamideToken \
        -testdir ./src/main/resources/restreplay \
        -reports ./rest-replay-reports


=============================================================
TODO:
=============================================================




=============================================================
  History
=============================================================
Run maven locally:
    http://jojovedder.blogspot.com/2009/04/running-maven-offline-using-local.html

I disabled tomcat tagonomy/dynamide authentication locally to test RestReplay.

2014-10-29:
   I ran on the command line with:
   mvn -DskipTests exec:java  -Dtestdir=/Users/vcrocla/src/RestReplay/src/main/resources/restreplay -DrestReplayMaster=dynamide-master.xml

   This is the command line that works in the IntelliJ IDEA project:
     args:  -testdir /Users/vcrocla/src/RestReplay/src/main/resources/restreplay -master dynamide-master.xml
     workingdir: /Users/vcrocla/src/RestReplay
     mainclass: org.dynamide.restreplay.RestReplay
     use-classpath-of-module: org.dynamide.RestReplay

2014-11-29:
   I fixed a bug where the protoHostPort was not being passed to mutators.
   Command line to check this was:





