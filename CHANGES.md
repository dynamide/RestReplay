===========Fri Feb 27 18:32:31 PST 2015======================
- Added fail-safe imports from other control files and testGroups.
  If import is not available at runtime, report shows group was skipped.
- Shortened output of long error json strings to MAX_CHARS_FOR_REPORT_LEVEL_SHORT
- Added GET as default method, rather than error.
- Trimmed excessive error message down when JSON error in file.
- Added the lovely Tools.getStackTraceTop()

2015-02-27   RestReplay now handles imports, so that a testGroup can import ServiceResult objects from another testGroup.  The imported test must have been run, otherwise the importing testGroup will receive an error and be skipped.  Thus, the imports are guaranteed to be available if you import them in your testGroup.  This means you can have one group to create some resources on a remote server via REST, another to test those resources, and a third group to tear down the resources.  The imports can reference any control file, testGroup, and testID.  In the master, ensure that all the testGroups are called in the correct order.


===========Fri Feb 27 21:38:47 PST 2015======================

- Added ./commit script, which uses the ./commit-message and generates the ./CHANGES.md file.
- Added JsonPath to the expressions ServiceResult.got(path) can accept.
--   Syntax here: https://github.com/jayway/JsonPath
- And XPath is still supported.
- Also added ServiceResult.gotJson(JsonPath).
- Also added ServiceResult.gotXPath(xpath).

===========Sat Feb 28 17:36:23 PST 2015======================
- Added gson library for serialization of ServiceResult to JSON.
- Added handling of JsonPath in gotJson() and got(), and added public gotXPath().
- Added experimental handlebars templating for displaying report based on serialized export of ServiceResult as JSON.
- Support files for serialization of ServiceResult to JSON thence to handlebar templates and jQuery .
- Added working template for handlebars.js expansion of ServiceResult JSON files.
- Added RunOptions.outputServiceResultDB so you can produce output JSON or not.

**2015-02-28**  RestReplay now outputs a database of all the ServiceResult objects serialized to JSON, if you set the RunOption outputServiceResultDB to "true".  The database is a directory of directories of flat JSON files, one per test.  The directory structure matches the controlFile/testGroup/test structure of your test directory.  For now, these JSON objects can be archived, or be available to Javascript-based manipulation, analytics, or reporting.  In a future version, Handlebars.js templates will convert the database into jQuery-enabled HTML.   (These currently live in the tests/_self_test/_includes/ folder.)

===========Mon Mar 2 12:32:20 PST 2015======================
- Added sent(jsonpath/xpath) and its helpers sentJson and sentXPath.
- Updated doco about using XPath versus JsonPath.

===========Tue Mar 3 16:17:30 PST 2015======================
- Added /restReplay/testGroup/test/comment tag support: 
    comments in this location will end up in the ServiceResult display in the report.
- Fixed handling of mime-type headers, properly condensing multiple headers such as Accept.
- Added runtime visibility of jar version number from the pom, in the Transport.addRestReplayHeaders()
    where I set the header X-RestReplay-version
- Added ResourceManager.readPropertiesFromClasspath()


===========Wed Mar 4 08:13:27 PST 2015======================
- JSONSuper moved to its own class, 
- changed JSONSuper.toString() to toXMLString().
- made ServiceResult.trappedExports transient - no need to include in serialized report.
- Added Javadoc overview files.
- Made <comment> tags display with "more..." links, collapsible spans, styles. 
- <comment> tags in **testGroup** and **test** both allow HTML markup.


===========Sat Mar 7 08:43:33 PST 2015======================
- Added class javadoc
- Reworked ResourceManager to return Resource objects not Strings
- Added test for missing filenames.
- Added ResourceManager legend on control file report page


===========Sat Mar 7 09:15:34 PST 2015======================

===========Sat Mar 7 12:18:21 PST 2015======================
- Added toc-toc to master report
- Fixed test report error aborting testGroup node, that was hidden until toc-toc showed an unreachable testGroup.

===========Mon Mar 9 15:14:23 PDT 2015======================
- Added fix: some headers allow condensed, some don't.
-  to be safe, we now define a list of condensing headers in runOptions.xml, and default them in RunOptions.
- current list is ACCEPT,CONTENT-TYPE,COOKIE, and you can pass NONE to remove even these defaults.

===========Mon Mar 9 16:03:44 PDT 2015======================
Added runOption emitRestReplayHeaders to turn off RestReplay headers that are on by default: X-RestReplay-fromTestID:, and X-RestReplay-version.

===========Tue Mar 10 15:24:21 PDT 2015======================
Added NOOP as a valid HTTP method, so that you can run tests that hit no server, but still do side-effects such as validation.
- to this end, re-ordered reading of expected parts so that they would be available in validators
- also changed rules about empty parts if doing a NOOP.

===========Wed Mar 11 17:27:59 PDT 2015======================
- autodelete had some bugs where results were not being displayed. This result showed up, but not any errors.  Fixed by including missing fields in errorResult. 
- Also GET requests were autodeleted.  This is fixed by having Transport.extractLocation() only mark a ServiceResult.deleteURL if it is a Transport.POST when handling a Location header.
- Fixed toc-toc anchor values not incrementing.
- Re-styled toc-toc.
- Corrected behavior requested by apache httpclient, to only getResponseBodyAsStream().
- Color changes for method boxes.


