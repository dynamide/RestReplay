var r = JSON.parse(serviceResult.result);

//var theCount = serviceResultsMap.get("selftestUseTokenPost").get("theCount");  figure this for another test.

var theCount = r.req.theCount;

serviceResult.addExport("newCountExport", theCount);
var res = {"newCount": Number(theCount)};  //note that you need Number(prevCount) and not "prevCount": prevCount, because the latter will cause sun.org.mozilla.javascript.internal.EvaluatorException: Java class "[B" has no public instance field or method named "toJSON".

serviceResult.addAlertError("problem with foobar", "some context");
serviceResult.addAlertWarning("A warning", "some warning context");
serviceResult.addAlertOK("Just some info", "some OK context");

JSON.stringify(res);

