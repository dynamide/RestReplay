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
- Added runOption emitRestReplayHeaders to turn off RestReplay headers that are on by default: X-RestReplay-fromTestID:, and X-RestReplay-version.

===========Tue Mar 10 15:24:21 PDT 2015======================
- Added NOOP as a valid HTTP method, so that you can run tests that hit no server, but still do side-effects such as validation.
-- to this end, re-ordered reading of expected parts so that they would be available in validators
-- also changed rules about empty parts if doing a NOOP.

===========Wed Mar 11 17:27:59 PDT 2015======================
- autodelete had some bugs where results were not being displayed. This result showed up, but not any errors.  Fixed by including missing fields in errorResult. 
- Also GET requests were autodeleted.  This is fixed by having Transport.extractLocation() only mark a ServiceResult.deleteURL if it is a Transport.POST when handling a Location header.
- Fixed toc-toc anchor values not incrementing.
- Re-styled toc-toc.
- Corrected behavior requested by apache httpclient, to only getResponseBodyAsStream().
- Color changes for method boxes.

===========Wed Mar 25 16:50:41 PDT 2015======================
- Refactored Tools functions into FileTools where they had to do with files and filenames.

===========Wed May 6 16:31:26 PDT 2015======================
- Made RestReplay available through Maven Central, including the standalone jar.
- Finalized portable run scripts, all now use just RESTREPLAY_JAR without adding to the path. RESTREPLAY_JAR now should contain full path or relative to dir run is in.
- Added Totals section to report, to command-line output, and added a RunOption to control the command-line output of a summary list, dumpMasterSummary.
- Added options to RunOptions:
    - reportResponseRaw,
    - dumpRunOptions: cleaned up RunOptions dump,
    - reportPayloadsAsXML: controls output of RESPONSE (as xml) and EXPECTED  (as xml)
- Added response/expected/code handling in addition to expected/code.

===========Wed May 6 18:49:45 PDT 2015======================
- Deployed version 1.0.10 to maven central

===========Thu May 7 20:24:21 PDT 2015======================
- Added restReplayMaster/event node, so that you can add a javascript event handler to perform additional processing during the report summary phase.
- Added three events using this mechanism: onBeginMaster, onSummary, onEndMaster.
- Updated doco with events
- Added these three events to master-self-test.xml
- Deployed version 1.0.11 to maven central

===========Thu May 7 20:38:04 PDT 2015======================
- Deployed version 1.0.12 to maven central, fixed NPE.

===========Tue May 12 22:50:07 PDT 2015======================
- added serviceResultsMap to Jexl context.
- added toString for LoopHelper
- added warning ServiceResult for ZERO loops.
- added error ServiceResult for untrapped error in loop.


===========Tue May 12 22:50:32 PDT 2015======================
- added serviceResultsMap to Jexl context.
- added toString for LoopHelper
- added warning ServiceResult for ZERO loops.
- added error ServiceResult for untrapped error in loop.


===========Wed May 13 13:40:59 PDT 2015======================
- Added handling of errors in LoopHelper which were not bubbling up to ServiceResult.
- loops of zero are now flagged as an error in the report.
- FAILURE: 0 is now printed in black, not red.  FAILURE: 1 is printed in red, etc.
- Set version number to 1.0.14


===========Wed May 27 02:02:09 EDT 2015======================
- Fixed small link in javadoc

===========Tue Jul 21 09:33:13 PDT 2015======================
- Added run_scenarios to test against an anarchia server (should be replaced with a generic self-test service) so that scenarios of running the run elements in master can be tested.  Multiple run elements can be repeated if you select either a key scenario or a loop scenario.  With a key scenario, each run element has an ID, and you import by that runID. With a loop scenario, you run run elements multiple times, interleaving with a run element for your test, importing the latest runs, whilst RestReplay generates a runID for reporting purposes.
- Modified the core to support these tests.  runID and sequence are included and reported now, so that re-using testGroups via multiple run elements in the master does not cause collisions.


===========Tue Jul 21 10:58:10 PDT 2015======================
- Added sr.getSequence() to filenames in json db reports for disambiguation of multiple runs.
- Ensured that Master Report Index is a full path so Cmd-click works in iTerm, etc.
