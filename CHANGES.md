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
===========Sat Feb 28 22:24:26 PST 2015======================
Support files for serialization of ServiceResult to JSON thence to handlebar templates and jQuery .

removeme
===========Sat Feb 28 22:24:53 PST 2015======================
Support files for serialization of ServiceResult to JSON thence to handlebar templates and jQuery .

removeme
===========Mon Mar 2 11:22:01 PST 2015======================
Added working template for handlebars.js expansion of ServiceResult JSON files.

removeme
===========Mon Mar 2 11:22:38 PST 2015======================
Added working template for handlebars.js expansion of ServiceResult JSON files.

removeme
===========Mon Mar 2 12:32:20 PST 2015======================
Added RunOptions.outputServiceResultDB so you can produce output JSON or not.
Added sent(jsonpath/xpath) and its helpers sentJson and sentXPath.
Updated doco about using XPath versus JsonPath.
removeme
