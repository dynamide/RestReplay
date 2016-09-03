
=============================================================
Doco:
=============================================================
    vi doc/RestReplay.html

    pushd ~/src/RestReplay
    mvn package
    bin/push-doco

=============================================================
Building:
=============================================================
    The first time:

        mvn install

    After that, you can run without maven downloading the Internet each time:

        mvn -o -DskipTests=true install

    Remember that this creates the release jar, such as target/RestReplay-1.0.4-standalone.jar
      which should be copied to any deployed clients.

    DEPLOY on Retina LAPPY WITH:
         restreplay -u
     (this is a script installed in ~/bin/restreplay )

=============================================================
Push to public server
=============================================================
javadoc:
docs:
artifacts:
    run the maven "package" phase:
        pushd ~/src/RestReplay
        mvn package
        bin/push-doco

deploy new jar version to maven central (this now also does an automatic release):
    vi ~/src/RestReplay/pom.xml     ##update the version number
    mvn deploy                      ## see wallet-card for gpg passphrase.

Inspect a repository.  
   (Note that mvn deploy is set to close the staging repository automatically, so you don't have to do this step to push to central.  )  
    log in to https://oss.sonatype.org/#welcome
    click Staging Repositories under Build Promotion on the left.
    look for orgdynamide-1xxx where 1xxx will be like 1009, or 1010 etc.
    When satisfied, click "release" button.
    Alternatively, use
      mvn nexus-staging:release
    Sonatype says their job to push to Central runs every 10 minutes, and updates to search.maven.org take 2 hours.
    
    

=============================================================
Set up OSSRH / sonatype central maven server for deployments.
=============================================================
Followed the jira ticket thing: 
https://issues.sonatype.org/browse/OSSRH-15028
and got an account on: 
    https://oss.sonatype.org/
Credentials are the same for oss.sonatype.orgt as for the ticket. 

First, your pom.xml must point to maven central.
The file in src/RestReplay/pom.xml does this already:

 ...
      </dependency>
    </dependencies>
    
    <distributionManagement>
      <snapshotRepository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
      </snapshotRepository>
      <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
      </repository>
    </distributionManagement>

    <build>
     ...
     
With that file in place, now log in to 
    https://oss.sonatype.org/
and click on user name, then "Profile" then drop down for "Summary" should be changed to "User Token"
then copy the userToken to this block  (these have been faked, below--replace with the block you see on the web page), and put that block in your ~/.m2/settings.xml on your local machine, 
add your userToken block for the user, for server id "ossrh"


        ...
        <server>
          <id>ossrh</id>
          <username>P39RBcb6</username>
          <password>7u+rSLm7SQPir2wSvq5YmrxvUvGYCfZRxSmrmOS73QKP</password>
        </server>
    </servers>  
    
The server id is "ossrh" which is referenced from the pom.xml of the project.  
Here we authorized that user on this local build machine.



=============================================================
Deploy to Revel/LAS:
=============================================================
   cd ~/src/RestReplay
   mvn -o -DskipTests install
   cd ~/ws/las/las/restreplay
   copy the jar, e.g.
       cp /Users/vcrocla/.m2/repository/org/dynamide/RestReplay/1.0.15/RestReplay-1.0.15-standalone.jar lib/
   
   When you change a RestReplay version, you must change these files (in addition to ~/src/RestReplay/pom.xml ):
       ~/ws/las/restreplay/pom.xml
       ~/ws/las/restreplay/java-options

Alternately, if you want to test between maven central releases, in a dependent project that lives in ~/ws/las/restreplay, do: 

   cd ~/src/RestReplay
   mvn -o -DskipTests install
   cd ~/ws/las/restreplay
   
   cp /Users/vcrocla/.m2/repository/org/dynamide/RestReplay/1.0.10/RestReplay-1.0.10-standalone.jar lib/
   use latest version, e.g.
   cp /Users/vcrocla/.m2/repository/org/dynamide/RestReplay/1.0.16/RestReplay-1.0.16-standalone.jar lib/
   
   java  $JAVA_OPTS -jar $RESTREPLAY_JAR -testdir ./tests -master las-master.xml  -env dev

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
and where the reports directory will appear, unless you specify with -reports /my/full/path/to/reports .



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
        -reports ./reports


=============================================================
  Running the self test server
=============================================================
You can run within RestReplay with -selftest

But you can also run the server yourself, just run main() in org.dynamide.restreplay.server.EmbeddedServer.


=============================================================
  Setting up org.dynamide in central maven repo
=============================================================

http://central.sonatype.org/pages/ossrh-guide.html
http://central.sonatype.org/pages/requirements.html

gpg: checking the trustdb
gpg: 3 marginal(s) needed, 1 complete(s) needed, PGP trust model
gpg: depth: 0  valid:   2  signed:   0  trust: 0-, 0q, 0n, 0m, 0f, 2u
gpg: next trustdb check due at 2018-08-19
pub   2048R/76AF320F 2015-04-16
      Key fingerprint = 8B78 9494 6487 09A8 83A4  B14F 6E3E BC34 76AF 320F
uid       [ultimate] Laramie Crocker (contact info at: http://dynamide.org) <order3@dynamide.org>
sub   2048R/0E44E730 2015-04-16

https://issues.sonatype.org/browse/OSSRH-15028

history: 
    [vcrocla@SFCAML-G2XFD56] ~/src/RestReplay (master)
    $ h gpg
      475  gpg  --list-keys
      476  gpg2 --list-secret-keys
      477  gpg --edit-key 76AF320F
      481  gpg -ab FooRestReplay.jar
      482  gpg2 --verify FooRestReplay.jar.asc
      486  gpg2 --keyserver hkp://pool.sks-keyservers.net --recv-keys 76AF320F
      487  gpg --gen-key
      490  gpg --delete-secret-keys 68C43CEB
      491  gpg --delete-key 68C43CEB
      492  gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys 76AF320F
      494  gpg --version
      495  gpg2 --version
      496  gpg --help
      501  gpg --export -a "Laramie Crocker" > public.key
      504  gpg --edit-key E53EB251
      515  gpg2 -ab -u 'Laramie Crocker' RestReplay-1.0.6-sources.jar
      516  gpg2 -ab -u 'Laramie Crocker' RestReplay-1.0.6-javadoc.jar
      517  gpg2 -ab -u 'Laramie Crocker' RestReplay-1.0.6.jar
      526  gpg2 -ab -u 'Laramie Crocker' pom.xml

=============================================================
  Using plugings to automate signing, deploying, and releasing to maven central
=============================================================
documentation:

    http://central.sonatype.org/pages/ossrh-guide.html
    http://central.sonatype.org/pages/releasing-the-deployment.html

    http://maven.apache.org/plugins/maven-gpg-plugin/
    http://maven.apache.org/plugins/maven-gpg-plugin/usage.html
    
    http://central.sonatype.org/pages/apache-maven.html
    
set up:
    I installed gpg, then added maven-gpg-plugin to the pom, which you execute manually with 
        mvn verify
    or: 
        mvn verify -Dgpg.passphrase=thephrase
        
   I installed nexus-staging plugin, it does everything, including staging it, closing it:
        mvn deploy
   When you are done, go here to release it.  
       Also can be done from command line:
           mvn nexus-staging:release
       Or, from the web interface:    
           https://oss.sonatype.org/#stagingRepositories
           staging repositories
           select the latest one, then click Promote on the top of the rows.
           
           https://oss.sonatype.org/content/repositories/releases/org/dynamide/RestReplay/1.0.7/RestReplay-1.0.7.jar
        
=============================================================
TODO:
=============================================================
Switch to apache http client version 4.
   http://debuguide.blogspot.in/2013/01/quick-guide-for-migration-of-commons.html

   Postman Interceptor
   https://www.getpostman.com/docs/capture
   import/export:


Consider architecting generators for requests.
        <request>
            <generator lang="javascript" filename="revelassignments/s/put-assignments.generator.js"/>
        </request>


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





