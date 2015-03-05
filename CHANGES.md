===========Fri Feb 27 18:32:31 PST 2015======================
- Added fail-safe imports from other control files and testGroups.
  If import is not available at runtime, report shows group was skipped.
- Shortened output of long error json strings to MAX_CHARS_FOR_REPORT_LEVEL_SHORT
- Added GET as default method, rather than error.
- Trimmed excessive error message down when JSON error in file.
- Added the lovely Tools.getStackTraceTop()

2015-02-27   RestReplay now handles imports, so that a testGroup can import ServiceResult objects from another testGroup.  The imported test must have been run, otherwise the importing testGroup will receive an error and be skipped.  Thus, the imports are guaranteed to be available if you import them in your testGroup.  This means you can have one group to create some resources on a remote server via REST, another to test those resources, and a third group to tear down the resources.  The imports can reference any control file, testGroup, and testID.  In the master, ensure that all the testGroups are called in the correct order.


===========Fri Feb 27 20:15:41 PST 2015======================
- Added ./commit script, which uses the ./commit-message and generates the ./CHANGES.md file.

===========Fri Feb 27 21:38:47 PST 2015======================

- Added JsonPath to the expressions ServiceResult.got(path) can accept.
--   Syntax here: https://github.com/jayway/JsonPath
- And XPath is still supported.
- Also added ServiceResult.gotJson(JsonPath).
- Also added ServiceResult.gotXPath(xpath).

===========Sat Feb 28 17:36:23 PST 2015======================
- Added gson library for serialization of ServiceResult to JSON.
- Added handling of JsonPath in gotJson() and got(), and added public gotXPath().
- Added experimental handlebars templating for displaying report based on serialized export of ServiceResult as JSON.

2015-02-28  RestReplay now outputs a database of all the ServiceResult objects, if you set the RunOption outputServiceResultDB to "true".  The database is a directory of directories of flat JSON files, one per test.  The directory structure matches the controlFile/testGroup/test structure of your test directory.  There are templates to work with these JSON files to jQuery them into HTML reports, but this is experimental.  For now, these JSON objects can be used to archive the results and do any Javascript-based manipulation, analytics, or reporting against these raw and calculated data.

===========Sat Feb 28 22:24:26 PST 2015======================
- Support files for serialization of ServiceResult to JSON thence to handlebar templates and jQuery .


===========Sat Feb 28 22:24:53 PST 2015======================
- Support files for serialization of ServiceResult to JSON thence to handlebar templates and jQuery .


===========Mon Mar 2 11:22:01 PST 2015======================
- Added working template for handlebars.js expansion of ServiceResult JSON files.


===========Mon Mar 2 11:22:38 PST 2015======================
- Added working template for handlebars.js expansion of ServiceResult JSON files.


===========Mon Mar 2 12:32:20 PST 2015======================
- Added RunOptions.outputServiceResultDB so you can produce output JSON or not.
- Added sent(jsonpath/xpath) and its helpers sentJson and sentXPath.
- Updated doco about using XPath versus JsonPath.

===========Tue Mar 3 16:17:30 PST 2015======================
- Added /restReplay/testGroup/test/comment tag support: 
    comments in this location will end up in the ServiceResult display in the report.
- Fixed handling of mime-type headers, properly condensing multiple headers such as Accept.
- Added runtime visibility of jar version number from the pom, in the Transport.addRestReplayHeaders()
    where I set the header X-RestReplay-version
- Added ResourceManager.readPropertiesFromClasspath()


===========Tue Mar 3 16:21:58 PST 2015======================
- Added /restReplay/testGroup/test/comment tag support: 
    comments in this location will end up in the ServiceResult display in the report.
- Fixed handling of mime-type headers, properly condensing multiple headers such as Accept.
- Added runtime visibility of jar version number from the pom, in the Transport.addRestReplayHeaders()
    where I set the header X-RestReplay-version
- Added ResourceManager.readPropertiesFromClasspath()


===========Tue Mar 3 20:27:24 PST 2015======================
- Cleaning up header handling.


===========Wed Mar 4 08:13:27 PST 2015======================
- General code cleanup:
-- JSONSuper moved to its own class, 
-- changed JSONSuper.toString() to toXMLString().
-- made ServiceResult.trappedExports transient - no need to include in serialized report.


===========Wed Mar 4 17:53:23 PST 2015======================
- Added Javadoc overview files.
- Made <comment> tags display with more... links, collapsible spans, and correct fonts.
- General code cleanup, JSONSuper moved to its own class, 
- changed JSONSuper.toString() to toXMLString().
- made ServiceResult.trappedExports transient - no need to include in serialized report.


===========Wed Mar 4 18:34:40 PST 2015======================
- Made <comment> blocks in testGroup and test both allow HTML markup.
- Also made test/comment show in green even if not so long it causes the more... link.
- typo in TreeWalkResults.


